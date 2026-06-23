package com.vica.app.proximity

import org.junit.Assert.*
import org.junit.Test

// ============================================================================
// ViCaHCEApduTest  —  Unit tests for APDU parsing logic
// Tests the byte-level APDU command interpretation WITHOUT Android framework.
// Run with: ./gradlew test
// ============================================================================
class ViCaHCEApduTest {

    // Replicated from ViCaHCEService (pure logic, no Android dependency)
    private fun isSelectAidCommand(apdu: ByteArray): Boolean {
        return apdu.size >= 5 &&
               apdu[0] == 0x00.toByte() &&
               apdu[1] == 0xA4.toByte() &&
               apdu[2] == 0x04.toByte() &&
               apdu[3] == 0x00.toByte()
    }

    private fun extractAid(apdu: ByteArray): ByteArray? {
        if (!isSelectAidCommand(apdu)) return null
        val aidLength = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + aidLength) return null
        return apdu.copyOfRange(5, 5 + aidLength)
    }

    private fun buildSelectApdu(aid: ByteArray): ByteArray {
        val apdu = ByteArray(5 + aid.size)
        apdu[0] = 0x00  // CLA
        apdu[1] = 0xA4.toByte()  // INS: SELECT
        apdu[2] = 0x04  // P1: by AID
        apdu[3] = 0x00  // P2
        apdu[4] = aid.size.toByte()  // Lc
        System.arraycopy(aid, 0, apdu, 5, aid.size)
        return apdu
    }

    // -----------------------------------------------------------------------

    @Test
    fun `SELECT AID APDU is correctly identified`() {
        val apdu = buildSelectApdu(ViCaSessionManager.VICA_AID)
        assertTrue("Should be identified as SELECT AID", isSelectAidCommand(apdu))
    }

    @Test
    fun `AID is correctly extracted from SELECT APDU`() {
        val apdu = buildSelectApdu(ViCaSessionManager.VICA_AID)
        val extracted = extractAid(apdu)
        assertNotNull("AID should be extractable", extracted)
        assertArrayEquals("Extracted AID must match VICA_AID", ViCaSessionManager.VICA_AID, extracted)
    }

    @Test
    fun `Non-SELECT APDU returns false`() {
        val notSelect = byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00)
        assertFalse("Non-SELECT must not match", isSelectAidCommand(notSelect))
    }

    @Test
    fun `Wrong AID is rejected`() {
        val wrongAid = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x85, 0x01, 0x01)
        val apdu = buildSelectApdu(wrongAid)
        val extracted = extractAid(apdu)
        assertNotNull(extracted)
        assertFalse("Wrong AID must not match VICA_AID",
            extracted!!.contentEquals(ViCaSessionManager.VICA_AID))
    }

    @Test
    fun `Response payload is 40 bytes (38 data + 2 SW)`() {
        val payload = ViCaSessionManager.assemblePayload()  // 38 bytes
        val response = payload + ViCaSessionManager.SW_SUCCESS  // + 0x9000
        assertEquals("Full response must be 40 bytes", 40, response.size)
        assertEquals("SW1 must be 0x90", 0x90.toByte(), response[38])
        assertEquals("SW2 must be 0x00", 0x00.toByte(), response[39])
    }

    @Test
    fun `SW_NOT_FOUND is 6A82`() {
        val sw = ViCaSessionManager.SW_NOT_FOUND
        assertEquals(2, sw.size)
        assertEquals(0x6A.toByte(), sw[0])
        assertEquals(0x82.toByte(), sw[1])
    }
}
