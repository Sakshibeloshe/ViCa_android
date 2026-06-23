package com.vica.app.proximity

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ViCaSessionManagerTest {

    @Before
    fun resetSession() { ViCaSessionManager.newSession() }

    @Test
    fun `newSession generates a 16-byte token`() {
        assertEquals(16, ViCaSessionManager.currentToken.size)
    }

    @Test
    fun `newSession rotates the token each call`() {
        val first = ViCaSessionManager.currentToken.copyOf()
        ViCaSessionManager.newSession()
        assertFalse(first.contentEquals(ViCaSessionManager.currentToken))
    }

    @Test
    fun `assemblePayload returns exactly 38 bytes`() {
        assertEquals(38, ViCaSessionManager.assemblePayload().size)
    }

    @Test
    fun `assemblePayload first 16 bytes match currentToken`() {
        val token = ViCaSessionManager.currentToken
        val payload = ViCaSessionManager.assemblePayload()
        assertArrayEquals(token, payload.copyOfRange(0, 16))
    }

    @Test
    fun `uuidToBytes and bytesToUUID are inverse operations`() {
        val original = UUID.randomUUID()
        val bytes = ViCaSessionManager.uuidToBytes(original)
        assertEquals(16, bytes.size)
        assertEquals(original, ViCaSessionManager.bytesToUUID(bytes))
    }

    @Test
    fun `GATT_SERVICE_UUID encodes to expected hex`() {
        val bytes = ViCaSessionManager.uuidToBytes(ViCaSessionManager.GATT_SERVICE_UUID)
        val hex = bytes.joinToString("") { "%02X".format(it) }
        assertEquals("F0564943410101010000000000000000", hex)
    }

    @Test
    fun `VICA_AID is 8 bytes and matches F056494341010101`() {
        val aid = ViCaSessionManager.VICA_AID
        assertEquals(8, aid.size)
        assertEquals("F056494341010101", aid.joinToString("") { "%02X".format(it) })
    }

    @Test
    fun `macBytesToString formats correctly`() {
        val mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
                              0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        assertEquals("AA:BB:CC:DD:EE:FF", ViCaSessionManager.macBytesToString(mac))
    }
}
