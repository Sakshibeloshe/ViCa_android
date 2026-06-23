package com.vica.app.proximity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sqrt

class ViCaBLETapDetector(
    private val context: Context,
    private val onTapConfirmed: (TapEvent) -> Unit,
    private val onTapRejected: (String) -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ViCaBLETapDetector"
        private val VICA_SERVICE_UUID: UUID = ViCaSessionManager.GATT_SERVICE_UUID
        private val VICA_PARCEL_UUID = ParcelUuid(VICA_SERVICE_UUID)
    }

    var rssiThreshold: Int = ViCaSessionManager.RSSI_THRESHOLD_DBM
    var accelThresholdMs2: Double = ViCaSessionManager.ACCEL_THRESHOLD_G * 9.81
    var confirmationWindowMs: Long = ViCaSessionManager.CONFIRMATION_WINDOW_MS
    var bumpDebounceMs: Long = ViCaSessionManager.BUMP_DEBOUNCE_MS

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null

    private val peers = mutableMapOf<String, PeerState>()
    private var localBumpTimestamp: Long? = null
    private var isInProximityZone = false
    private val sessionId: Int = (Math.random() * Int.MAX_VALUE).toInt()

    fun start() {
        Log.i(TAG, "Starting ViCaBLETapDetector")
        startAccelerometer()
        startBLEAdvertising()
        startBLEScanning()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        try {
            if (hasBlePermission()) {
                leAdvertiser?.stopAdvertising(advertiseCallback)
                leScanner?.stopScan(scanCallback)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE stop SecurityException", e)
        }
        peers.clear()
        localBumpTimestamp = null
        isInProximityZone = false
    }

    private fun startAccelerometer() {
        if (linearAccelSensor != null) {
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            val rawAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (rawAccel != null) {
                sensorManager.registerListener(this, rawAccel, SensorManager.SENSOR_DELAY_FASTEST)
            } else {
                onTapRejected("No accelerometer sensor available on this device")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val (x, y, z) = when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION ->
                Triple(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
            Sensor.TYPE_ACCELEROMETER ->
                Triple(event.values[0].toDouble(), event.values[1].toDouble(), (event.values[2] - 9.81).toDouble())
            else -> return
        }
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude > accelThresholdMs2 && isInProximityZone) {
            onBumpDetected(magnitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onBumpDetected(magnitude: Double) {
        val now = System.currentTimeMillis()
        val lastBump = localBumpTimestamp
        if (lastBump != null && now - lastBump < bumpDebounceMs) return
        localBumpTimestamp = now
        val accelByte = (magnitude * 10.0).coerceIn(0.0, 255.0).toInt().toByte()
        advertisePacket(ViCaSessionManager.PACKET_BUMP, accelByte)
        checkForMutualConfirmation()
    }

    private fun checkForMutualConfirmation() {
        val localTime = localBumpTimestamp ?: return
        for ((identifier, peer) in peers) {
            val peerBumpTime = peer.lastBumpTimeMs ?: continue
            if (!peer.isInRange) continue
            val delta = Math.abs(localTime - peerBumpTime)
            if (delta < confirmationWindowMs) {
                advertisePacket(ViCaSessionManager.PACKET_CONFIRM)
                onTapConfirmed(TapEvent(peerIdentifier = identifier, deltaMs = delta.toDouble()))
                return
            }
        }
    }

    private fun updatePeerRSSI(address: String, rssi: Int) {
        val state = peers.getOrPut(address) { PeerState() }
        state.lastRssi = rssi
        state.isInRange = rssi >= rssiThreshold
        isInProximityZone = peers.values.any { it.isInRange }
    }

    private fun handlePeerPacket(address: String, data: ByteArray) {
        if (data.size < 6) return
        val packetType = data[0]
        val timestampMs = ((data[1].toLong() and 0xFF) shl 24) or
                          ((data[2].toLong() and 0xFF) shl 16) or
                          ((data[3].toLong() and 0xFF) shl 8)  or
                           (data[4].toLong() and 0xFF)
        val peerSessionId = ByteBuffer.wrap(data, 6, 4).order(ByteOrder.BIG_ENDIAN).int
        if (peerSessionId == sessionId) return
        when (packetType) {
            ViCaSessionManager.PACKET_BUMP -> {
                peers.getOrPut(address) { PeerState() }.lastBumpTimeMs = timestampMs
                if (localBumpTimestamp != null) checkForMutualConfirmation()
            }
            else -> {}
        }
    }

    private fun hasBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBLEAdvertising() {
        if (!hasBlePermission()) {
            Log.w(TAG, "BLE permissions not granted — advertising skipped (NFC-only mode)")
            return
        }
        try {
            leAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            if (leAdvertiser == null) { Log.w(TAG, "BLE advertising not supported"); return }
            advertisePacket(ViCaSessionManager.PACKET_SCAN)
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE advertise SecurityException — permission not granted at runtime", e)
        }
    }

    private fun advertisePacket(type: Byte, accelByte: Byte = 0x00) {
        val advertiser = leAdvertiser ?: return
        if (!hasBlePermission()) return
        val timestamp = (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()
        val serviceData = ByteArray(19).apply {
            this[0] = type
            this[1] = ((timestamp shr 24) and 0xFF).toByte()
            this[2] = ((timestamp shr 16) and 0xFF).toByte()
            this[3] = ((timestamp shr 8)  and 0xFF).toByte()
            this[4] = (timestamp and 0xFF).toByte()
            this[5] = accelByte
            this[6] = ((sessionId shr 24) and 0xFF).toByte()
            this[7] = ((sessionId shr 16) and 0xFF).toByte()
            this[8] = ((sessionId shr 8)  and 0xFF).toByte()
            this[9] = (sessionId and 0xFF).toByte()
        }
        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false).build()
            val data = AdvertiseData.Builder()
                .addServiceUuid(VICA_PARCEL_UUID)
                .addServiceData(VICA_PARCEL_UUID, serviceData)
                .setIncludeDeviceName(false).build()
            advertiser.stopAdvertising(advertiseCallback)
            advertiser.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE advertise packet SecurityException", e)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) onTapRejected("BLE advertising not supported")
        }
    }

    private fun startBLEScanning() {
        if (!hasBlePermission()) {
            Log.w(TAG, "BLE permissions not granted — scanning skipped (NFC-only mode)")
            return
        }
        try {
            leScanner = bluetoothAdapter?.bluetoothLeScanner
            if (leScanner == null) { Log.w(TAG, "BLE scanning not available"); return }
            val filters = listOf(ScanFilter.Builder().setServiceUuid(VICA_PARCEL_UUID).build())
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0L).build()
            leScanner?.startScan(filters, settings, scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE scan SecurityException — permission not granted at runtime", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            updatePeerRSSI(result.device.address, result.rssi)
            val record: ScanRecord = result.scanRecord ?: return
            val serviceData: ByteArray? = record.getServiceData(VICA_PARCEL_UUID)
            if (serviceData != null) handlePeerPacket(result.device.address, serviceData)
        }
        override fun onScanFailed(errorCode: Int) { onTapRejected("BLE scan failed (code $errorCode)") }
    }

    data class PeerState(
        var lastRssi: Int = -100,
        var isInRange: Boolean = false,
        var lastBumpTimeMs: Long? = null
    )
}
