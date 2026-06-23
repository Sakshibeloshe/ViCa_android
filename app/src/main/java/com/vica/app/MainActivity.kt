package com.vica.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.vica.app.navigation.RootNavigation
import com.vica.app.proximity.ViCaNFCPayload
import com.vica.app.proximity.ViCaNFCReader
import com.vica.app.ui.theme.ViCaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * SharedFlow that emits whenever an NFC tap is completed on this device
     * (i.e., this device is the receiver). [RootNavigation] collects this and
     * hands it to [NfcTransferViewModel.onNfcPayloadReceived].
     *
     * Using a SharedFlow (not StateFlow) so replayed events don't re-trigger
     * the card overlay after it's been dismissed.
     */
    private val _nfcEvents = MutableSharedFlow<Pair<ViCaNFCPayload, ByteArray?>>(
        extraBufferCapacity = 1
    )
    val nfcEvents: SharedFlow<Pair<ViCaNFCPayload, ByteArray?>> = _nfcEvents.asSharedFlow()

    private lateinit var nfcReader: ViCaNFCReader

    // ── Runtime BLE permission launcher ───────────────────────────────────────
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        android.util.Log.d("MainActivity", "BLE permissions granted: $granted")
        // Whether or not permissions are granted, the app continues.
        // ViCaBLETapDetector guards each call with hasBlePermission(),
        // so a denial just means NFC-only mode (no crash).
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request BLE runtime permissions (Android 12+ requires them at runtime)
        requestBlePermissions()

        // Build the NFC reader once — it references this Activity for enableReaderMode
        nfcReader = ViCaNFCReader(
            activity           = this,
            onPayloadReceived  = { payload, cardBytes ->
                _nfcEvents.tryEmit(Pair(payload, cardBytes))
            },
            onError = { reason ->
                // Log only — UI handles the ProximityManager FAILED state
                android.util.Log.w("MainActivity", "NFC reader error: $reason")
            }
        )

        setContent {
            ViCaTheme {
                RootNavigation(nfcEvents = nfcEvents)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable reader mode when the app is in the foreground.
        // NOTE: On the sender device, opening ShareCardScreen does NOT call
        // nfcReader.enable() because ShareCardScreen tells the ViewModel to arm
        // HCE via ProximityManager.startListening() instead. Reader mode and HCE
        // are mutually exclusive on most hardware, so we always keep reader active
        // and only suppress it if this device is explicitly acting as a sender.
        nfcReader.enable()
    }

    override fun onPause() {
        super.onPause()
        nfcReader.disable()
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    private fun requestBlePermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        blePermissionLauncher.launch(perms)
    }
}