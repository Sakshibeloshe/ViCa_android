package com.vica.app.proximity

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// ProximityManager  —  Orchestrator for Phase 2 + Phase 3
// ---------------------------------------------------------------------------
// Coordinates NFC HCE (passive — always on via AndroidManifest) and the
// BLE+Accelerometer tap detector (active — started when share sheet opens).
//
// Use as a ViewModel-scoped singleton (or inject via Hilt).
//
// State machine:
//   IDLE  →  LISTENING  →  CONFIRMED  (or FAILED)
//                       ↗ (NFC path — passive, no UI trigger needed)
// ---------------------------------------------------------------------------
class ProximityManager(private val context: Context) {

    companion object {
        private const val TAG = "ProximityManager"
    }

    // -----------------------------------------------------------------------
    // Public state observable by Compose UI
    // -----------------------------------------------------------------------

    enum class ProximityState {
        IDLE,        // Not listening
        LISTENING,   // BLE+Accel active, NFC HCE always running
        CONFIRMED,   // Tap confirmed — ready to exchange card
        FAILED       // Error occurred
    }

    private val _state = MutableStateFlow(ProximityState.IDLE)
    val state: StateFlow<ProximityState> = _state.asStateFlow()

    private val _lastEvent = MutableStateFlow<ProximityResult?>(null)
    val lastEvent: StateFlow<ProximityResult?> = _lastEvent.asStateFlow()

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tapDetector: ViCaBLETapDetector? = null

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Start actively listening for a tap.
     * NFC HCE is always-on via the service registered in AndroidManifest —
     * this method activates the BLE+Accelerometer path in addition.
     */
    fun startListening() {
        if (_state.value == ProximityState.LISTENING) return
        Log.i(TAG, "ProximityManager: startListening()")

        ViCaSessionManager.newSession()

        tapDetector = ViCaBLETapDetector(
            context = context,
            onTapConfirmed = { event -> handleTapConfirmed(event) },
            onTapRejected  = { reason -> handleError(reason) }
        )
        tapDetector?.start()
        _state.value = ProximityState.LISTENING
    }

    /** Stop all active listeners. NFC HCE continues passively. */
    fun stopListening() {
        tapDetector?.stop()
        tapDetector = null
        ViCaSessionManager.clearSession()
        _state.value = ProximityState.IDLE
        Log.i(TAG, "ProximityManager: stopped")
    }

    /** Reset to idle after processing a confirmed tap. */
    fun reset() {
        stopListening()
        _lastEvent.value = null
    }

    fun destroy() {
        stopListening()
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    private fun handleTapConfirmed(event: TapEvent) {
        scope.launch {
            Log.i(TAG, "Tap confirmed: peer=${event.peerIdentifier} delta=${event.deltaMs}ms")
            tapDetector?.stop()
            tapDetector = null
            _lastEvent.value = ProximityResult.BumpTapConfirmed(event)
            _state.value = ProximityState.CONFIRMED
        }
    }

    /**
     * Called when iOS reads our NFC HCE tag.
     * Since HCE is a passive service, the system notifies it automatically.
     * To surface this to the UI, call this from ViCaHCEService via a
     * LocalBroadcastManager or EventBus if you want real-time UI updates.
     *
     * For Phase 2 the HCE service is self-contained; no UI update is needed
     * (iOS drives the UX). This method is a hook for future integration.
     */
    fun onNfcHandshakeComplete(payload: ViCaNFCPayload) {
        scope.launch {
            Log.i(TAG, "NFC handshake complete: token=${payload.sessionToken.toHex()}")
            _lastEvent.value = ProximityResult.NfcHandshakeComplete(payload)
            _state.value = ProximityState.CONFIRMED
        }
    }

    private fun handleError(reason: String) {
        scope.launch {
            Log.e(TAG, "Proximity error: $reason")
            _lastEvent.value = ProximityResult.Error(reason)
            _state.value = ProximityState.FAILED
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
}
