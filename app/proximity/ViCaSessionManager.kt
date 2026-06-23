package com.vica.app.proximity

import java.util.UUID

// ---------------------------------------------------------------------------
// ViCaSessionManager
//
// Manages the shared session state used by BOTH the HCE service and the
// BLE tap detector so they can reference the same ephemeral token and
// GATT UUID during a pairing session.
//
// Lifecycle:
//   1. Call newSession() before the user initiates a share.
//   2. HCE service reads currentToken / currentGattUUID to fill its payload.
//   3. BLE tap detector reads currentGattUUID to advertise the right service.
//   4. Call clearSession() after the card has been exchanged or on timeout.
//
// Thread safety: All fields are @Volatile for safe cross-thread reads.
// ---------------------------------------------------------------------------
object ViCaSessionManager {

    // ViCa AID — must match apduservice.xml and iOS vicaAID bytes
    // F0 56 49 43 41 01 01 01  =  0xF0 + "VICA" + 0x01 0x01 0x01
    val VICA_AID: ByteArray = byteArrayOf(
        0xF0.toByte(), 0x56, 0x49, 0x43, 0x41, 0x01, 0x01, 0x01
    )

    // BLE service UUID shared between HCE payload and BLE advertising
    val GATT_SERVICE_UUID: UUID = UUID.fromString("F0564943-4101-0101-0000-000000000000")

    // APDU status words
    val SW_SUCCESS: ByteArray = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val SW_NOT_FOUND: ByteArray = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    // BLE advertisement packet type constants (mirrors iOS 0x01/0x02/0x03)
    const val PACKET_SCAN: Byte    = 0x01
    const val PACKET_BUMP: Byte    = 0x02
    const val PACKET_CONFIRM: Byte = 0x03

    // Proximity thresholds (mirrors iOS Config defaults)
    const val RSSI_THRESHOLD_DBM: Int = -60        // ~30 cm
    const val ACCEL_THRESHOLD_G: Double = 0.8       // g-force
    const val CONFIRMATION_WINDOW_MS: Long = 300L   // mutual bump window
    const val BUMP_DEBOUNCE_MS: Long = 1_000L       // ignore repeat bumps

    @Volatile var currentToken: ByteArray = generateToken()
        private set

    @Volatile var currentBleMac: ByteArray = byteArrayOf(
        0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
    )  // Will be replaced with real MAC at runtime when possible

    /** Generate a new 16-byte session token and reset state. */
    fun newSession() {
        currentToken = generateToken()
    }

    fun clearSession() {
        currentToken = generateToken()
    }

    // -----------------------------------------------------------------------
    // Payload assembly
    // -----------------------------------------------------------------------

    /**
     * Assemble the 38-byte NFC response payload.
     * Layout mirrors iOS parseAndComplete():
     *   [0..15]  sessionToken (16 bytes)
     *   [16..21] BLE MAC      (6 bytes)
     *   [22..37] GATT UUID    (16 bytes, big-endian)
     */
    fun assemblePayload(): ByteArray {
        val payload = ByteArray(38)
        // 1. Session token
        System.arraycopy(currentToken, 0, payload, 0, 16)
        // 2. BLE MAC
        System.arraycopy(currentBleMac, 0, payload, 16, 6)
        // 3. GATT UUID (16 bytes big-endian)
        val uuidBytes = uuidToBytes(GATT_SERVICE_UUID)
        System.arraycopy(uuidBytes, 0, payload, 22, 16)
        return payload
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun generateToken(): ByteArray {
        val uuid = UUID.randomUUID()
        return uuidToBytes(uuid).copyOf(16)
    }

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
