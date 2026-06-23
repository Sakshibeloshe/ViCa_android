package com.vica.app.proximity

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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.ParcelUuid
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// ViCaBLETapDetector  —  Phase 3: BLE + Accelerometer Tap Detection
// ---------------------------------------------------------------------------
// Purpose:
//   Replicates iOS ViCaBLETapDetector.swift behaviour for Android.
//   Used when NFC is not available (e.g., Android → Android, iOS → Android BLE).
//
// Two-gate confirmation logic (matches iOS exactly):
//   Gate 1 — Proximity: peer's RSSI must be above RSSI_THRESHOLD_DBM (~30 cm)
//   Gate 2 — Motion:    local acceleration spike > 0.8g (gravity-removed)
//
// Confirmation window:
//   Both devices must bump within CONFIRMATION_WINDOW_MS (300 ms) of each other.
//   Each device broadcasts its bump timestamp as a BLE advertisement packet.
//
// BLE advertisement packet format (19-byte service data field):
//   [0]     packetType:  0x01=SCAN  0x02=BUMP  0x03=CONFIRM
//   [1..4]  timestamp:   ms since epoch, big-endian UInt32
//   [5]     accelByte:   0x00=idle, magnitude*10 clamped to 0xFF when bumped
//   [6..9]  sessionId:   4-byte random ID for this session
//   [10..18] reserved
//
// Usage:
//   val detector = ViCaBLETapDetector(context,
//       onTapConfirmed = { event -> /* exchange card */ },
//       onTapRejected  = { reason -> /* show error */ }
//   )
//   detector.start()
//   // ... when done:
//   detector.stop()
// ---------------------------------------------------------------------------
class ViCaBLETapDetector(
    private val context: Context,
    private val onTapConfirmed: (TapEvent) -> Unit,
    private val onTapRejected: (String) -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ViCaBLETapDetector"

        // BLE service UUID (matches ViCaSessionManager.GATT_SERVICE_UUID)
        private val VICA_SERVICE_UUID: UUID = ViCaSessionManager.GATT_SERVICE_UUID
        private val VICA_PARCEL_UUID = ParcelUuid(VICA_SERVICE_UUID)
    }

    // -----------------------------------------------------------------------
    // Configuration (mirrors iOS Config struct)
    // -----------------------------------------------------------------------
    var rssiThreshold: Int    = ViCaSessionManager.RSSI_THRESHOLD_DBM
    var accelThresholdMs2: Double = ViCaSessionManager.ACCEL_THRESHOLD_G * 9.81   // convert g → m/s²
    var confirmationWindowMs: Long = ViCaSessionManager.CONFIRMATION_WINDOW_MS
    var bumpDebounceMs: Long  = ViCaSessionManager.BUMP_DEBOUNCE_MS

    // -----------------------------------------------------------------------
    // Android subsystem handles
    // -----------------------------------------------------------------------
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null

    // -----------------------------------------------------------------------
    // Mutable state (accessed only on single background thread via callbacks)
    // -----------------------------------------------------------------------
    private val peers = mutableMapOf<String, PeerState>()  // keyed by BLE device address
    private var localBumpTimestamp: Long? = null            // epoch ms
    private var isInProximityZone = false

    // Random 4-byte session ID for this session (used to ignore self-echoes)
    private val sessionId: Int = (Math.random() * Int.MAX_VALUE).toInt()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Start BLE advertising + scanning + accelerometer.
     * Call this when the share/receive sheet opens.
     */
    fun start() {
        Log.i(TAG, "Starting ViCaBLETapDetector (sessionId=${sessionId.toHex()})")
        startAccelerometer()
        startBLEAdvertising()
        startBLEScanning()
    }

    /**
     * Stop all sensors and BLE activity.
     * Call this when the share sheet closes or after a successful tap.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
        leAdvertiser?.stopAdvertising(advertiseCallback)
        leScanner?.stopScan(scanCallback)
        peers.clear()
        localBumpTimestamp = null
        isInProximityZone = false
        Log.i(TAG, "ViCaBLETapDetector stopped")
    }

    // -----------------------------------------------------------------------
    // Accelerometer (SensorEventListener)
    // -----------------------------------------------------------------------

    private fun startAccelerometer() {
        if (linearAccelSensor != null) {
            sensorManager.registerListener(
                this,
                linearAccelSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
            Log.d(TAG, "Linear accelerometer registered")
        } else {
            Log.w(TAG, "TYPE_LINEAR_ACCELERATION not available — using TYPE_ACCELEROMETER fallback")
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
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Gravity already subtracted — pure motion
                Triple(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Rough gravity subtraction (z axis ≈ 1g at rest)
                Triple(event.values[0].toDouble(), event.values[1].toDouble(), (event.values[2] - 9.81).toDouble())
            }
            else -> return
        }

        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude > accelThresholdMs2 && isInProximityZone) {
            onBumpDetected(magnitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    // -----------------------------------------------------------------------
    // Bump logic (mirrors iOS onBumpDetected / checkForMutualConfirmation)
    // -----------------------------------------------------------------------

    private fun onBumpDetected(magnitude: Double) {
        val now = System.currentTimeMillis()
        val lastBump = localBumpTimestamp
        if (lastBump != null && now - lastBump < bumpDebounceMs) return  // debounce

        Log.i(TAG, "Bump detected! magnitude=${String.format("%.2f", magnitude / 9.81)}g")
        localBumpTimestamp = now

        val accelByte = (magnitude * 10.0).coerceIn(0.0, 255.0).toInt().toByte()
        advertisePacket(ViCaSessionManager.PACKET_BUMP, accelByte)

        // Check if a peer already sent their bump within the window
        checkForMutualConfirmation()
    }

    private fun checkForMutualConfirmation() {
        val localTime = localBumpTimestamp ?: return

        for ((identifier, peer) in peers) {
            val peerBumpTime = peer.lastBumpTimeMs ?: continue
            if (!peer.isInRange) continue

            val delta = Math.abs(localTime - peerBumpTime)
            if (delta < confirmationWindowMs) {
                Log.i(TAG, "TAP CONFIRMED with $identifier (delta=${delta}ms)")
                advertisePacket(ViCaSessionManager.PACKET_CONFIRM)
                val event = TapEvent(
                    peerIdentifier = identifier,
                    deltaMs = delta.toDouble()
                )
                onTapConfirmed(event)
                return
            }
        }
        // No match yet — wait for peer's BUMP advertisement
    }

    // -----------------------------------------------------------------------
    // Peer state tracking
    // -----------------------------------------------------------------------

    private fun updatePeerRSSI(address: String, rssi: Int) {
        val state = peers.getOrPut(address) { PeerState() }
        state.lastRssi = rssi
        state.isInRange = rssi >= rssiThreshold
        isInProximityZone = peers.values.any { it.isInRange }
    }

    private fun handlePeerPacket(address: String, data: ByteArray) {
        if (data.size < 6) return

        val packetType = data[0]
        // Read big-endian UInt32 timestamp from [1..4]
        val timestampMs = ((data[1].toLong() and 0xFF) shl 24) or
                          ((data[2].toLong() and 0xFF) shl 16) or
                          ((data[3].toLong() and 0xFF) shl 8)  or
                           (data[4].toLong() and 0xFF)

        // Ignore our own echoed packets (same session ID)
        val peerSessionId = ByteBuffer.wrap(data, 6, 4)
            .order(ByteOrder.BIG_ENDIAN).int
        if (peerSessionId == sessionId) return

        when (packetType) {
            ViCaSessionManager.PACKET_BUMP -> {
                Log.d(TAG, "BUMP packet from $address at ${timestampMs}ms")
                peers.getOrPut(address) { PeerState() }.lastBumpTimeMs = timestampMs
                if (localBumpTimestamp != null) {
                    checkForMutualConfirmation()
                }
            }
            ViCaSessionManager.PACKET_CONFIRM -> {
                // Peer already matched — no further action needed
                Log.d(TAG, "CONFIRM packet from $address")
            }
            else -> { /* SCAN or unknown — ignore */ }
        }
    }

    // -----------------------------------------------------------------------
    // BLE Advertising
    // -----------------------------------------------------------------------

    private fun startBLEAdvertising() {
        leAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (leAdvertiser == null) {
            Log.w(TAG, "BLE advertising not supported on this device")
            return
        }
        advertisePacket(ViCaSessionManager.PACKET_SCAN)
    }

    private fun advertisePacket(type: Byte, accelByte: Byte = 0x00) {
        val advertiser = leAdvertiser ?: return

        val timestamp = (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()
        val serviceData = ByteArray(19).apply {
            this[0] = type
            this[1] = ((timestamp shr 24) and 0xFF).toByte()
            this[2] = ((timestamp shr 16) and 0xFF).toByte()
            this[3] = ((timestamp shr 8)  and 0xFF).toByte()
            this[4] = (timestamp and 0xFF).toByte()
            this[5] = accelByte
            // Session ID [6..9] big-endian
            this[6] = ((sessionId shr 24) and 0xFF).toByte()
            this[7] = ((sessionId shr 16) and 0xFF).toByte()
            this[8] = ((sessionId shr 8)  and 0xFF).toByte()
            this[9] = (sessionId and 0xFF).toByte()
            // [10..18] reserved — zeroed by ByteArray default
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(VICA_PARCEL_UUID)
            .addServiceData(VICA_PARCEL_UUID, serviceData)
            .setIncludeDeviceName(false)
            .build()

        advertiser.stopAdvertising(advertiseCallback)
        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: errorCode=$errorCode")
            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> Log.d(TAG, "Already advertising")
                ADVERTISE_FAILED_DATA_TOO_LARGE  -> Log.e(TAG, "Advertise data too large")
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> onTapRejected("BLE advertising not supported")
                else -> onTapRejected("BLE advertising error: $errorCode")
            }
        }
    }

    // -----------------------------------------------------------------------
    // BLE Scanning
    // -----------------------------------------------------------------------

    private fun startBLEScanning() {
        leScanner = bluetoothAdapter?.bluetoothLeScanner
        if (leScanner == null) {
            Log.w(TAG, "BLE scanning not available")
            return
        }

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(VICA_PARCEL_UUID)
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)  // immediate callbacks
            .build()

        leScanner?.startScan(filters, settings, scanCallback)
        Log.d(TAG, "BLE scan started for service ${VICA_SERVICE_UUID}")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address
            val rssi = result.rssi

            updatePeerRSSI(address, rssi)

            // Extract ViCa service data
            val record: ScanRecord = result.scanRecord ?: return
            val serviceData: ByteArray? = record.getServiceData(VICA_PARCEL_UUID)
            if (serviceData != null) {
                handlePeerPacket(address, serviceData)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed: errorCode=$errorCode")
            onTapRejected("BLE scan failed (code $errorCode)")
        }
    }

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    /** Per-peer tracked state. */
    data class PeerState(
        var lastRssi: Int = -100,
        var isInRange: Boolean = false,
        var lastBumpTimeMs: Long? = null
    )

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun Int.toHex(): String = "%08X".format(this)
}
