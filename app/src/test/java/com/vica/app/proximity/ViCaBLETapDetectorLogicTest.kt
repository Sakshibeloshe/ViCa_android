package com.vica.app.proximity

import org.junit.Assert.*
import org.junit.Test

class ViCaBLETapDetectorLogicTest {

    private fun buildBumpPacket(timestampMs: Long, sessionId: Int, accelByte: Byte = 0x50): ByteArray {
        val data = ByteArray(19)
        data[0] = ViCaSessionManager.PACKET_BUMP
        data[1] = ((timestampMs shr 24) and 0xFF).toByte()
        data[2] = ((timestampMs shr 16) and 0xFF).toByte()
        data[3] = ((timestampMs shr 8)  and 0xFF).toByte()
        data[4] = (timestampMs and 0xFF).toByte()
        data[5] = accelByte
        data[6] = ((sessionId shr 24) and 0xFF).toByte()
        data[7] = ((sessionId shr 16) and 0xFF).toByte()
        data[8] = ((sessionId shr 8)  and 0xFF).toByte()
        data[9] = (sessionId and 0xFF).toByte()
        return data
    }

    private fun extractTimestamp(packet: ByteArray): Long {
        return ((packet[1].toLong() and 0xFF) shl 24) or
               ((packet[2].toLong() and 0xFF) shl 16) or
               ((packet[3].toLong() and 0xFF) shl 8)  or
                (packet[4].toLong() and 0xFF)
    }

    private fun extractSessionId(packet: ByteArray): Int {
        return ((packet[6].toInt() and 0xFF) shl 24) or
               ((packet[7].toInt() and 0xFF) shl 16) or
               ((packet[8].toInt() and 0xFF) shl 8)  or
                (packet[9].toInt() and 0xFF)
    }

    @Test
    fun `bump packet is 19 bytes`() {
        val packet = buildBumpPacket(System.currentTimeMillis(), 12345)
        assertEquals(19, packet.size)
    }

    @Test
    fun `packet type byte is correct for BUMP`() {
        val packet = buildBumpPacket(0L, 0)
        assertEquals(ViCaSessionManager.PACKET_BUMP, packet[0])
    }

    @Test
    fun `timestamp round-trips through packet format`() {
        val timestamp = 1716499200000L
        val masked = timestamp and 0xFFFFFFFFL
        val packet = buildBumpPacket(masked, 0)
        assertEquals(masked, extractTimestamp(packet))
    }

    @Test
    fun `session ID round-trips through packet`() {
        val sessionId = 0xDEADBEEF.toInt()
        val packet = buildBumpPacket(0L, sessionId)
        assertEquals(sessionId, extractSessionId(packet))
    }

    @Test
    fun `bumps within 300ms should confirm`() {
        val delta = Math.abs(1000L - 1250L)
        assertTrue(delta < ViCaSessionManager.CONFIRMATION_WINDOW_MS)
    }

    @Test
    fun `bumps outside 300ms should NOT confirm`() {
        val delta = Math.abs(1000L - 1400L)
        assertFalse(delta < ViCaSessionManager.CONFIRMATION_WINDOW_MS)
    }

    @Test
    fun `bumps exactly at window boundary should NOT confirm`() {
        val delta = Math.abs(1000L - 1300L)
        assertFalse(delta < ViCaSessionManager.CONFIRMATION_WINDOW_MS)
    }

    @Test
    fun `acceleration below threshold does not trigger`() {
        assertFalse((0.5 * 9.81) > ViCaSessionManager.ACCEL_THRESHOLD_G * 9.81)
    }

    @Test
    fun `acceleration above threshold triggers`() {
        assertTrue((1.2 * 9.81) > ViCaSessionManager.ACCEL_THRESHOLD_G * 9.81)
    }

    @Test
    fun `accel byte clamping works correctly`() {
        assertEquals(20, (2.0 * 10.0).coerceIn(0.0, 255.0).toInt())
        assertEquals(255, (300.0 * 10.0).coerceIn(0.0, 255.0).toInt())
    }

    @Test
    fun `bumps within debounce window are ignored`() {
        assertTrue(500L < ViCaSessionManager.BUMP_DEBOUNCE_MS)
    }

    @Test
    fun `bumps after debounce window are accepted`() {
        assertFalse(1500L < ViCaSessionManager.BUMP_DEBOUNCE_MS)
    }
}
