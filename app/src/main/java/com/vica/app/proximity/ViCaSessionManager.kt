package com.vica.app.proximity

import java.util.UUID

object ViCaSessionManager {

    val VICA_AID: ByteArray = byteArrayOf(
        0xF0.toByte(), 0x56, 0x49, 0x43, 0x41, 0x01, 0x01, 0x01
    )

    val GATT_SERVICE_UUID: UUID = UUID.fromString("F0564943-4101-0101-0000-000000000000")

    val SW_SUCCESS: ByteArray = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val SW_NOT_FOUND: ByteArray = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    const val PACKET_SCAN: Byte    = 0x01
    const val PACKET_BUMP: Byte    = 0x02
    const val PACKET_CONFIRM: Byte = 0x03

    const val RSSI_THRESHOLD_DBM: Int = -60
    const val ACCEL_THRESHOLD_G: Double = 0.8
    const val CONFIRMATION_WINDOW_MS: Long = 300L
    const val BUMP_DEBOUNCE_MS: Long = 1_000L

    @Volatile var currentToken: ByteArray = generateToken()
        private set

    @Volatile var currentBleMac: ByteArray = byteArrayOf(
        0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
    )

    fun newSession() { currentToken = generateToken() }
    fun clearSession() { currentToken = generateToken() }

    fun assemblePayload(): ByteArray {
        val payload = ByteArray(38)
        System.arraycopy(currentToken, 0, payload, 0, 16)
        System.arraycopy(currentBleMac, 0, payload, 16, 6)
        val uuidBytes = uuidToBytes(GATT_SERVICE_UUID)
        System.arraycopy(uuidBytes, 0, payload, 22, 16)
        return payload
    }

    private fun generateToken(): ByteArray = uuidToBytes(UUID.randomUUID()).copyOf(16)

    fun uuidToBytes(uuid: UUID): ByteArray {
        val bytes = ByteArray(16)
        val most  = uuid.mostSignificantBits
        val least = uuid.leastSignificantBits
        for (i in 0..7) {
            bytes[i]     = (most  ushr ((7 - i) * 8)).toByte()
            bytes[8 + i] = (least ushr ((7 - i) * 8)).toByte()
        }
        return bytes
    }

    fun bytesToUUID(bytes: ByteArray): UUID {
        require(bytes.size >= 16) { "UUID needs 16 bytes" }
        var most  = 0L
        var least = 0L
        for (i in 0..7)  most  = (most  shl 8) or (bytes[i].toLong() and 0xFF)
        for (i in 8..15) least = (least shl 8) or (bytes[i].toLong() and 0xFF)
        return UUID(most, least)
    }

    fun macBytesToString(mac: ByteArray): String =
        mac.take(6).joinToString(":") { "%02X".format(it) }
}
