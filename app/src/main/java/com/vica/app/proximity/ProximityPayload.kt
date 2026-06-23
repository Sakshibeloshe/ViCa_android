package com.vica.app.proximity

import java.util.UUID

data class ViCaNFCPayload(
    val sessionToken: ByteArray,
    val bleMacAddress: String,
    val gattServiceUUID: UUID
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

data class TapEvent(
    val peerIdentifier: String,
    val deltaMs: Double,
    val confirmedAt: Long = System.currentTimeMillis()
)

sealed class ProximityResult {
    data class NfcHandshakeComplete(val payload: ViCaNFCPayload) : ProximityResult()
    data class BumpTapConfirmed(val event: TapEvent) : ProximityResult()
    data class Error(val reason: String) : ProximityResult()
}
