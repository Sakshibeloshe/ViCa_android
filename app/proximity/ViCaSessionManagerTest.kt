package com.vica.app.proximity

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

// ============================================================================
// ViCaSessionManagerTest  —  Unit tests (no Android framework needed)
// Run with: ./gradlew test
// ============================================================================
class ViCaSessionManagerTest {

    @Before
    fun resetSession() {
        ViCaSessionManager.newSession()
    }

    // -----------------------------------------------------------------------
    // Token generation
    // -----------------------------------------------------------------------

    @Test
    fun `newSession generates a 16-byte token`() {
        val token = ViCaSessionManager.currentToken
        assertEquals("Token must be exactly 16 bytes", 16, token.size)
    }

    @Test
    fun `newSession rotates the token each call`() {
        val first = ViCaSessionManager.currentToken.copyOf()
        ViCaSessionManager.newSession()
        val second = ViCaSessionManager.currentToken.copyOf()
        assertFalse("Token should change on newSession()", first.contentEquals(second))
    }

    // -----------------------------------------------------------------------
    // Payload assembly
    // -----------------------------------------------------------------------

    @Test
    fun `assemblePayload returns exactly 38 bytes`() {
        val payload = ViCaSessionManager.assemblePayload()
        assertEquals("Payload must be exactly 38 bytes", 38, payload.size)
    }

    @Test
    fun `assemblePayload first 16 bytes match currentToken`() {
        val token = ViCaSessionManager.currentToken
        val payload = ViCaSessionManager.assemblePayload()
        val payloadToken = payload.copyOfRange(0, 16)
        assertArrayEquals("Token bytes must be in payload[0..15]", token, payloadToken)
    }

    @Test
    fun `assemblePayload bytes 16-21 match currentBleMac`() {
        val mac = ViCaSessionManager.currentBleMac
        val payload = ViCaSessionManager.assemblePayload()
        val payloadMac = payload.copyOfRange(16, 22)
        assertArrayEquals("MAC bytes must be in payload[16..21]", mac, payloadMac)
    }

    @Test
    fun `assemblePayload bytes 22-37 encode GATT_SERVICE_UUID correctly`() {
        val expectedUUID = ViCaSessionManager.GATT_SERVICE_UUID
        val payload = ViCaSessionManager.assemblePayload()
        val uuidBytes = payload.copyOfRange(22, 38)
        val decodedUUID = ViCaSessionManager.bytesToUUID(uuidBytes)
        assertEquals("UUID in payload[22..37] must round-trip", expectedUUID, decodedUUID)
    }

    // -----------------------------------------------------------------------
    // UUID ↔ ByteArray round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `uuidToBytes and bytesToUUID are inverse operations`() {
        val original = UUID.randomUUID()
        val bytes = ViCaSessionManager.uuidToBytes(original)
        assertEquals("Must produce 16 bytes", 16, bytes.size)
        val decoded = ViCaSessionManager.bytesToUUID(bytes)
        assertEquals("Round-trip must recover original UUID", original, decoded)
    }

    @Test
    fun `GATT_SERVICE_UUID encodes to expected hex`() {
        // F0564943-4101-0101-0000-000000000000
        val bytes = ViCaSessionManager.uuidToBytes(ViCaSessionManager.GATT_SERVICE_UUID)
        val hex = bytes.joinToString("") { "%02X".format(it) }
        assertEquals(
            "GATT UUID bytes must be F0564943410101010000000000000000",
            "F0564943410101010000000000000000",
            hex
        )
    }

    // -----------------------------------------------------------------------
    // AID bytes
    // -----------------------------------------------------------------------

    @Test
    fun `VICA_AID is 8 bytes and matches F056494341010101`() {
        val aid = ViCaSessionManager.VICA_AID
        assertEquals("AID must be 8 bytes", 8, aid.size)
        val hex = aid.joinToString("") { "%02X".format(it) }
        assertEquals("AID hex must match iOS vicaAID", "F056494341010101", hex)
    }

    // -----------------------------------------------------------------------
    // MAC formatting
    // -----------------------------------------------------------------------

    @Test
    fun `macBytesToString formats correctly`() {
        val mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
                              0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        assertEquals("AA:BB:CC:DD:EE:FF", ViCaSessionManager.macBytesToString(mac))
    }
}
