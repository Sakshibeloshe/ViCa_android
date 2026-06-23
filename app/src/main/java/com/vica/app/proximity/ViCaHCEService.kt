package com.vica.app.proximity

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

/**
 * ViCaHCEService — Host Card Emulation (Sender Side)
 *
 * Makes this Android device appear as an NFC card to any reader (another Android
 * running ViCaNFCReader, or an iPhone with CoreNFC).
 *
 * APDU exchange sequence:
 *   Reader → SELECT AID (F056494341010101)
 *   HCE   ← 38-byte handshake payload + SW 9000
 *
 *   Reader → GET DATA (INS=CA P1=01)   ← optional, card bytes
 *   HCE   ← compact card JSON bytes + SW 9000
 *
 * The 38-byte handshake payload (see [ViCaSessionManager.assemblePayload]):
 *   [0..15]  Session token   — 16 bytes (random, BLE encryption seed)
 *   [16..21] BLE MAC address — 6 bytes
 *   [22..37] GATT Service UUID — 16 bytes
 *
 * [pendingCardBytes] must be set by [NfcTransferViewModel.prepareSend] before
 * the NFC tap happens so the receiver gets card data inline.
 */
class ViCaHCEService : HostApduService() {

    companion object {
        private const val TAG = "ViCaHCEService"

        /**
         * Compact card JSON bytes set by [NfcTransferViewModel] when a card is
         * selected for sharing. Served to the reader via GET DATA (INS=CA, P1=01).
         * Cleared to null after the share sheet is dismissed.
         */
        @Volatile
        var pendingCardBytes: ByteArray? = null
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            Log.w(TAG, "Received null APDU — returning NOT FOUND")
            return ViCaSessionManager.SW_NOT_FOUND
        }

        Log.d(TAG, "APDU received (${commandApdu.size} bytes): ${commandApdu.toHex()}")

        // ── SELECT AID (CLA=00 INS=A4 P1=04 P2=00) ────────────────────────
        if (commandApdu.size >= 5 &&
            commandApdu[0] == 0x00.toByte() &&
            commandApdu[1] == 0xA4.toByte() &&
            commandApdu[2] == 0x04.toByte() &&
            commandApdu[3] == 0x00.toByte()
        ) {
            val aidLength = commandApdu[4].toInt() and 0xFF
            if (commandApdu.size >= 5 + aidLength) {
                val incomingAid = commandApdu.copyOfRange(5, 5 + aidLength)
                if (incomingAid.contentEquals(ViCaSessionManager.VICA_AID)) {
                    Log.i(TAG, "ViCa AID matched — responding with 38-byte handshake payload")
                    return buildHandshakeResponse()
                }
            }
        }

        // ── GET DATA P1=00 — generic fallback (some readers send this) ─────
        if (commandApdu.size >= 4 &&
            commandApdu[0] == 0x00.toByte() &&
            commandApdu[1] == 0xCA.toByte() &&
            commandApdu[2] == 0x00.toByte() &&
            commandApdu[3] == 0x00.toByte()
        ) {
            return buildHandshakeResponse()
        }

        // ── GET DATA P1=01 — compact card bytes ───────────────────────────
        if (commandApdu.size >= 4 &&
            commandApdu[0] == 0x00.toByte() &&
            commandApdu[1] == 0xCA.toByte() &&
            commandApdu[2] == 0x01.toByte() &&
            commandApdu[3] == 0x00.toByte()
        ) {
            val cardBytes = pendingCardBytes
            return if (cardBytes != null) {
                Log.i(TAG, "Serving card bytes (${cardBytes.size} bytes)")
                cardBytes + ViCaSessionManager.SW_SUCCESS
            } else {
                Log.w(TAG, "GET DATA P1=01 requested but no card bytes available")
                ViCaSessionManager.SW_NOT_FOUND
            }
        }

        return ViCaSessionManager.SW_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS  -> "link lost (phones moved apart)"
            DEACTIVATION_DESELECTED -> "deselected by reader"
            else                    -> "unknown ($reason)"
        }
        Log.d(TAG, "NFC HCE deactivated: $reasonStr")
    }

    private fun buildHandshakeResponse(): ByteArray {
        val payload = ViCaSessionManager.assemblePayload()
        return payload + ViCaSessionManager.SW_SUCCESS
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
}
