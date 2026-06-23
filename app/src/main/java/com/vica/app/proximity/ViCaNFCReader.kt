package com.vica.app.proximity

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.content.Context
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViCaNFCReader — NFC Reader Mode (Receiver Side)
 *
 * Puts this Android device into ISO-DEP reader mode so it can read another
 * Android phone running ViCaHCEService. When the two phones touch:
 *
 *   1. This device sends SELECT AID (ViCa AID = F056494341010101)
 *   2. The sender's HCE service responds with a 38-byte handshake payload
 *      (session token + BLE MAC + GATT UUID)
 *   3. We then send GET DATA (INS=CA, P1=01) to retrieve the full card bytes
 *   4. Both payloads are parsed and handed to [onPayloadReceived]
 *
 * Usage — in your Activity:
 *
 *   override fun onResume() { super.onResume(); nfcReader.enable() }
 *   override fun onPause()  { super.onPause();  nfcReader.disable() }
 */
class ViCaNFCReader(
    private val activity: Activity,
    private val onPayloadReceived: (ViCaNFCPayload, ByteArray?) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "ViCaNFCReader"
    }

    private val nfcAdapter: NfcAdapter? by lazy {
        try {
            val manager = activity.getSystemService(Context.NFC_SERVICE) as? NfcManager
            manager?.defaultAdapter
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get NFC adapter", e)
            null
        }
    }

    /** True if NFC hardware is present. */
    val isAvailable: Boolean get() = nfcAdapter != null

    /** True if NFC is enabled in system settings. */
    val isEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    /**
     * Enable reader mode — call in onResume().
     *
     * NFC_A + NFC_B cover the two physical modulation schemes used by phones.
     * FLAG_READER_SKIP_NDEF_CHECK lets us handle raw APDUs ourselves instead of
     * the OS intercepting for NDEF.
     *
     * NOTE: Reader mode and HCE are mutually exclusive on most Android hardware.
     * Only enable reader mode on the receiver device; the sender relies on HCE.
     */
    fun enable() {
        if (!isAvailable || !isEnabled) {
            Log.w(TAG, "NFC not available or disabled — skipping reader mode")
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        nfcAdapter?.enableReaderMode(activity, ::onTagDiscovered, flags, null)
        Log.d(TAG, "NFC reader mode enabled")
    }

    /**
     * Disable reader mode — call in onPause().
     * Restores HCE capability for this device.
     */
    fun disable() {
        nfcAdapter?.disableReaderMode(activity)
        Log.d(TAG, "NFC reader mode disabled")
    }

    /**
     * Called on a background thread when another NFC device comes into range.
     */
    private fun onTagDiscovered(tag: Tag) {
        Log.d(TAG, "NFC tag discovered: ${tag.techList.joinToString()}")

        when {
            tag.techList.contains(IsoDep::class.java.name) -> readIsoDep(tag)
            else -> {
                Log.w(TAG, "Unsupported tag type — not an ISO-DEP device")
                onError("Unsupported NFC tag type. Make sure the other device has ViCa open.")
            }
        }
    }

    /**
     * Exchange APDUs with the sender's ViCaHCEService.
     *
     * Exchange 1: SELECT AID → 38-byte handshake payload + SW 90 00
     * Exchange 2: GET DATA (INS=CA P1=01) → compact card bytes + SW 90 00
     */
    private fun readIsoDep(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: run {
            onError("Failed to get IsoDep interface from tag")
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 5_000 // 5 s — generous for first-tap latency

            Log.d(TAG, "IsoDep connected. maxTransceiveLength=${isoDep.maxTransceiveLength}")

            // ── Step 1: SELECT AID ────────────────────────────────────────────
            val selectApdu = buildSelectApdu(ViCaSessionManager.VICA_AID)
            Log.d(TAG, "→ SELECT AID: ${selectApdu.toHex()}")
            val selectResponse = isoDep.transceive(selectApdu)
            Log.d(TAG, "← SELECT response: ${selectResponse.toHex()}")

            if (selectResponse.size < 2) {
                onError("Malformed SELECT response (too short)")
                return
            }

            val selectStatus = selectResponse.takeLast(2).toByteArray()
            if (!selectStatus.contentEquals(ViCaSessionManager.SW_SUCCESS)) {
                onError("SELECT AID rejected — status: ${selectStatus.toHex()}")
                return
            }

            // Parse the 38-byte handshake payload embedded in the SELECT response
            val handshakeBytes = selectResponse.dropLast(2).toByteArray()
            if (handshakeBytes.size < 38) {
                onError("Handshake payload too short (${handshakeBytes.size} bytes, need 38)")
                return
            }

            val nfcPayload = parseHandshakePayload(handshakeBytes)

            // ── Step 2: GET DATA (P1=01) for card bytes ───────────────────────
            val getCardApdu = buildGetCardDataApdu()
            Log.d(TAG, "→ GET CARD DATA: ${getCardApdu.toHex()}")
            val cardResponse = isoDep.transceive(getCardApdu)
            Log.d(TAG, "← CARD DATA response (${cardResponse.size} bytes)")

            val cardBytes: ByteArray? = if (cardResponse.size >= 2) {
                val cardStatus = cardResponse.takeLast(2).toByteArray()
                if (cardStatus.contentEquals(ViCaSessionManager.SW_SUCCESS) && cardResponse.size > 2) {
                    cardResponse.dropLast(2).toByteArray()
                } else {
                    Log.w(TAG, "GET DATA P1=01 not supported or empty — BLE fallback will be used")
                    null
                }
            } else null

            // ── Hand off to caller on Main thread ─────────────────────────────
            CoroutineScope(Dispatchers.Main).launch {
                onPayloadReceived(nfcPayload, cardBytes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "IsoDep communication failed", e)
            onError("NFC read error: ${e.message}")
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    /**
     * Parse the 38-byte handshake payload from the HCE SELECT response.
     *
     *   [0..15]  Session token  — 16 bytes (random, BLE encryption seed)
     *   [16..21] BLE MAC        — 6 bytes
     *   [22..37] GATT UUID      — 16 bytes
     */
    private fun parseHandshakePayload(bytes: ByteArray): ViCaNFCPayload {
        val sessionToken = bytes.copyOfRange(0, 16)
        val bleMac       = bytes.copyOfRange(16, 22)
        val uuidBytes    = bytes.copyOfRange(22, 38)

        return ViCaNFCPayload(
            sessionToken    = sessionToken,
            bleMacAddress   = bleMac.toMacString(),
            gattServiceUUID = ViCaSessionManager.bytesToUUID(uuidBytes)
        )
    }

    // ─── APDU builders ────────────────────────────────────────────────────────

    /**
     * ISO 7816-4 SELECT by AID:
     *   CLA=00  INS=A4  P1=04  P2=00  Lc=[aidLen]  [AID]
     */
    private fun buildSelectApdu(aid: ByteArray): ByteArray = byteArrayOf(
        0x00,            // CLA — no secure messaging
        0xA4.toByte(),   // INS — SELECT
        0x04,            // P1  — select by AID
        0x00,            // P2  — first or only occurrence
        aid.size.toByte() // Lc — length of data field
    ) + aid

    /**
     * GET DATA with P1=0x01 to fetch the compact card bytes.
     *   CLA=00  INS=CA  P1=01  P2=00  Le=FF (request up to 255 bytes)
     */
    private fun buildGetCardDataApdu(): ByteArray = byteArrayOf(
        0x00,
        0xCA.toByte(),
        0x01,            // P1=01 → card data (P1=00 is the default GET DATA)
        0x00,
        0xFF.toByte()    // Le — expected response length
    )

    // ─── Extension helpers ────────────────────────────────────────────────────

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
    private fun List<Byte>.toByteArray() = ByteArray(size) { this[it] }
    private fun List<Byte>.toHex() = toByteArray().toHex()

    private fun ByteArray.toMacString(): String =
        joinToString(":") { "%02X".format(it) }
}
