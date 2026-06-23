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

class ProximityManager(private val context: Context) {

    companion object {
        private const val TAG = "ProximityManager"
    }

    enum class ProximityState { IDLE, LISTENING, CONFIRMED, FAILED }

    private val _state = MutableStateFlow(ProximityState.IDLE)
    val state: StateFlow<ProximityState> = _state.asStateFlow()

    private val _lastEvent = MutableStateFlow<ProximityResult?>(null)
    val lastEvent: StateFlow<ProximityResult?> = _lastEvent.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tapDetector: ViCaBLETapDetector? = null

    fun startListening() {
        if (_state.value == ProximityState.LISTENING) return
        ViCaSessionManager.newSession()
        tapDetector = ViCaBLETapDetector(
            context = context,
            onTapConfirmed = { event -> handleTapConfirmed(event) },
            onTapRejected  = { reason -> handleError(reason) }
        )
        tapDetector?.start()
        _state.value = ProximityState.LISTENING
    }

    fun stopListening() {
        tapDetector?.stop()
        tapDetector = null
        ViCaSessionManager.clearSession()
        _state.value = ProximityState.IDLE
    }

    fun reset() {
        stopListening()
        _lastEvent.value = null
    }

    fun destroy() {
        stopListening()
        scope.cancel()
    }

    private fun handleTapConfirmed(event: TapEvent) {
        scope.launch {
            tapDetector?.stop()
            tapDetector = null
            _lastEvent.value = ProximityResult.BumpTapConfirmed(event)
            _state.value = ProximityState.CONFIRMED
        }
    }

    fun onNfcHandshakeComplete(payload: ViCaNFCPayload) {
        scope.launch {
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
}
