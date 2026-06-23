package com.vica.app.ui.screens.mycards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vica.app.data.local.CardRepository
import com.vica.app.data.model.CardModel
import com.vica.app.proximity.CardSerializer
import com.vica.app.proximity.ProximityManager
import com.vica.app.proximity.ViCaHCEService
import com.vica.app.proximity.ViCaNFCPayload
import com.vica.app.proximity.ViCaSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * NfcTransferViewModel — bridges NFC/BLE proximity events with the UI.
 *
 * Sender side:
 *   1. [prepareSend] is called when the user taps a card → opens Share screen.
 *   2. It encodes the card into bytes, arms [ViCaHCEService.pendingCardBytes],
 *      generates a fresh session, and starts the [ProximityManager].
 *
 * Receiver side:
 *   1. [onNfcPayloadReceived] is called by [ViCaNFCReader] after a successful tap.
 *   2. The inline card bytes are decoded and saved to Room via [CardRepository].
 *   3. [transferState] transitions to [TransferState.Received] so the UI shows
 *      the incoming card overlay.
 */
class NfcTransferViewModel(
    private val repository: CardRepository,
    private val proximityManager: ProximityManager
) : ViewModel() {

    sealed class TransferState {
        object Idle     : TransferState()
        /** Sender: HCE armed, listening for a tap. */
        object Waiting  : TransferState()
        /** Receiver: card decoded and ready to show. */
        data class Received(val card: CardModel) : TransferState()
        data class Error(val message: String)    : TransferState()
    }

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    // ─── Sender ───────────────────────────────────────────────────────────────

    /**
     * Called when the user selects a card to share.
     * Arms the HCE service with an encoded card payload and starts proximity detection.
     */
    fun prepareSend(card: CardModel) {
        ViCaSessionManager.newSession()
        ViCaHCEService.pendingCardBytes = CardSerializer.encode(card)
        proximityManager.startListening()
        _transferState.value = TransferState.Waiting
    }

    /**
     * Called when the Share screen is dismissed. Cleans up HCE state.
     */
    fun cancelSend() {
        ViCaHCEService.pendingCardBytes = null
        proximityManager.stopListening()
        _transferState.value = TransferState.Idle
    }

    // ─── Receiver ─────────────────────────────────────────────────────────────

    /**
     * Called by [ViCaNFCReader] when an NFC tap has completed.
     *
     * @param payload   Handshake payload parsed from the SELECT AID response.
     * @param cardBytes Compact card JSON bytes from GET DATA P1=01 (null if
     *                  the sender didn't arm HCE with card data yet).
     */
    fun onNfcPayloadReceived(payload: ViCaNFCPayload, cardBytes: ByteArray?) {
        proximityManager.onNfcHandshakeComplete(payload)

        if (cardBytes != null) {
            val card = CardSerializer.decode(cardBytes)
            if (card != null) {
                persistAndShow(card)
                return
            }
        }
        // Card bytes absent or corrupt — BLE follow-up would handle the full data
        _transferState.value = TransferState.Error(
            "Tap detected — waiting for card data via BLE"
        )
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    private fun persistAndShow(card: CardModel) {
        viewModelScope.launch {
            repository.insertReceivedCard(card)
            _transferState.value = TransferState.Received(card)
        }
    }

    /** Called when the user dismisses the received-card overlay. */
    fun dismissReceived() {
        _transferState.value = TransferState.Idle
    }

    fun reset() {
        cancelSend()
        _transferState.value = TransferState.Idle
    }
}
