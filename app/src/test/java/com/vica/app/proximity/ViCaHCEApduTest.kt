package com.vica.app.proximity

import org.junit.Assert.*
import org.junit.Test

class ViCaHCEApduTest {

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
        apdu[0] = 0x00; apdu[1] = 0xA4.toByte(); apdu[2] = 0x04
        apdu[3] = 0x00; apdu[4] = aid.size.toByte()
        System.arraycopy(aid, 0, apdu, 5, aid.size)
        return apdu
    }

    @Test fun `SELECT AID APDU is correctly identified`() {
        assertTrue(isSelectAidCommand(buildSelectApdu(ViCaSessionManager.VICA_AID)))
    }

    @Test fun `AID is correctly extracted from SELECT APDU`() {
        val extracted = extractAid(buildSelectApdu(ViCaSessionManager.VICA_AID))
        assertNotNull(extracted)
        assertArrayEquals(ViCaSessionManager.VICA_AID, extracted)
    }

    @Test fun `Non-SELECT APDU returns false`() {
        assertFalse(isSelectAidCommand(byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00)))
    }

    @Test fun `Response payload is 40 bytes (38 data + 2 SW)`() {
        val response = ViCaSessionManager.assemblePayload() + ViCaSessionManager.SW_SUCCESS
        assertEquals(40, response.size)
        assertEquals(0x90.toByte(), response[38])
        assertEquals(0x00.toByte(), response[39])
    }
}
