package com.vica.app.proximity

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

// ---------------------------------------------------------------------------
// ViCaHCEService  —  Phase 2: NFC Host Card Emulation
// ---------------------------------------------------------------------------
// Purpose:
//   Emulates an ISO 7816-4 smart card so that an iOS device running
//   ViCaNFCReader can discover this Android device via NFC without pairing.
//
// Protocol flow (Android → iOS NFC read):
//   1. iOS calls NFCTagReaderSession with pollingOption .iso7816
//   2. iOS sends SELECT AID (CLA=00 INS=A4 P1=04 P2=00 AID=F056494341010101)
//   3. This service matches the AID and responds with a 38-byte payload:
//        [0..15]  sessionToken  — 16-byte ephemeral token
//        [16..21] bleMacAddress — 6 bytes identifying our BLE advertiser
//        [22..37] gattUUID      — 16-byte GATT service UUID (big-endian)
//   4. iOS parses the payload → connects to our GATT server → card exchange
//
// Registration:
//   See AndroidManifest.xml (service tag) and res/xml/apduservice.xml (AID).
// ---------------------------------------------------------------------------
class ViCaHCEService : HostApduService() {

    companion object {
        private const val TAG = "ViCaHCEService"
    }

    // -----------------------------------------------------------------------
    // ISO 7816-4 APDU processing
    // -----------------------------------------------------------------------

    /**
     * Called by the NFC subsystem with each incoming APDU command.
     * Must be fast — runs on the NFC reader thread.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            Log.w(TAG, "Received null APDU — returning NOT FOUND")
            return ViCaSessionManager.SW_NOT_FOUND
        }

        Log.d(TAG, "APDU received (${commandApdu.size} bytes): ${commandApdu.toHex()}")

        // ---------------------------------------------------------------
        // Check for ISO 7816-4 SELECT command
        //   CLA = 0x00
        //   INS = 0xA4  (SELECT)
        //   P1  = 0x04  (Select by AID, first or only occurrence)
        //   P2  = 0x00
        // ---------------------------------------------------------------
        if (commandApdu.size >= 5 &&
            commandApdu[0] == 0x00.toByte() &&   // CLA
            commandApdu[1] == 0xA4.toByte() &&   // INS
            commandApdu[2] == 0x04.toByte() &&   // P1
            commandApdu[3] == 0x00.toByte()      // P2
        ) {
            val aidLength = commandApdu[4].toInt() and 0xFF
            if (commandApdu.size >= 5 + aidLength) {
                val incomingAid = commandApdu.copyOfRange(5, 5 + aidLength)
                if (incomingAid.contentEquals(ViCaSessionManager.VICA_AID)) {
                    Log.i(TAG, "ViCa AID matched — responding with 38-byte payload")
                    return buildResponse()
                } else {
                    Log.w(TAG, "AID mismatch: ${incomingAid.toHex()}")
                }
            }
        }

        // ---------------------------------------------------------------
        // GET DATA command (fallback — iOS may follow up with this)
        //   CLA = 0x00, INS = 0xCA, P1 = 0x00, P2 = 0x00
        // ---------------------------------------------------------------
        if (commandApdu.size >= 4 &&
            commandApdu[0] == 0x00.toByte() &&
            commandApdu[1] == 0xCA.toByte() &&
            commandApdu[2] == 0x00.toByte() &&
            commandApdu[3] == 0x00.toByte()
        ) {
            Log.i(TAG, "GET DATA — responding with payload")
            return buildResponse()
        }

        Log.w(TAG, "Unknown APDU — returning NOT FOUND")
        return ViCaSessionManager.SW_NOT_FOUND
    }

    /**
     * Called when the NFC link drops (reader moved away or reader deactivated).
     * Reason codes: DEACTIVATION_LINK_LOSS = 0, DEACTIVATION_DESELECTED = 1
     */
    override fun onDeactivated(reason: Int) {
        val reasonStr = if (reason == DEACTIVATION_LINK_LOSS) "LINK_LOSS" else "DESELECTED"
        Log.d(TAG, "NFC HCE deactivated: $reasonStr")
        // Optionally rotate token after each session
        // ViCaSessionManager.newSession()
    }

    // -----------------------------------------------------------------------
    // Payload builder
    // -----------------------------------------------------------------------

    /**
     * Assembles the 38-byte ViCa payload + 2-byte SW 9000 success trailer.
     * Total = 40 bytes returned to the iOS reader.
     */
    private fun buildResponse(): ByteArray {
        val payload = ViCaSessionManager.assemblePayload()      // 38 bytes
        val response = ByteArray(payload.size + 2)
        System.arraycopy(payload, 0, response, 0, payload.size)
        System.arraycopy(ViCaSessionManager.SW_SUCCESS, 0, response, payload.size, 2)
        Log.d(TAG, "HCE response (${response.size} bytes): ${response.toHex()}")
        return response
    }

    // -----------------------------------------------------------------------
    // Extension helper
    // -----------------------------------------------------------------------

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02X".format(it) }
}
