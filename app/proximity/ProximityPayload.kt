package com.vica.app.proximity

import java.util.UUID

// ---------------------------------------------------------------------------
// ViCaNFCPayload
// Mirrors the iOS ViCaNFCPayload struct.
//
// Parsed from the 38-byte payload returned by ViCaHCEService:
//   [0..15]  sessionToken   — 16-byte random token (BLE encryption seed)
//   [16..21] bleMacAddress  — 6-byte MAC address of the Android device
//   [22..37] gattServiceUUID — 16-byte GATT service UUID (big-endian)
// ---------------------------------------------------------------------------
data class ViCaNFCPayload(
    val sessionToken: ByteArray,        // 16 bytes
    val bleMacAddress: String,          // e.g. "AA:BB:CC:DD:EE:FF"
    val gattServiceUUID: UUID           // parsed from last 16 bytes
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ViCaNFCPayload
        return sessionToken.contentEquals(other.sessionToken) &&
               bleMacAddress == other.bleMacAddress &&
               gattServiceUUID == other.gattServiceUUID
    }
    override fun hashCode(): Int {
        var result = sessionToken.contentHashCode()
        result = 31 * result + bleMacAddress.hashCode()
        result = 31 * result + gattServiceUUID.hashCode()
        return result
    }
}

// ---------------------------------------------------------------------------
// TapEvent
// Fired when both devices bump within the confirmation window.
// Mirrors iOS ViCaBLETapDetector.TapEvent.
// ---------------------------------------------------------------------------
data class TapEvent(
    val peerIdentifier: String,   // BLE device address
    val deltaMs: Double,          // time delta between bumps in ms
    val confirmedAt: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// ProximityResult
// Sealed class wrapping all possible outcomes of a proximity exchange.
// ---------------------------------------------------------------------------
sealed class ProximityResult {
    /** NFC path — iOS device read our HCE tag and sent back its BLE info. */
    data class NfcHandshakeComplete(val payload: ViCaNFCPayload) : ProximityResult()

    /** BLE+Accel path — mutual bump detected within confirmation window. */
    data class BumpTapConfirmed(val event: TapEvent) : ProximityResult()

    /** Either path failed gracefully. */
    data class Error(val reason: String) : ProximityResult()
}
