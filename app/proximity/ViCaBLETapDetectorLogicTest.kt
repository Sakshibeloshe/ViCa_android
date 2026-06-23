package com.vica.app.proximity

import org.junit.Assert.*
import org.junit.Test

// ============================================================================
// ViCaBLETapDetectorLogicTest  —  Unit tests for tap detection logic
// Tests the timestamp math and confirmation window WITHOUT Bluetooth/sensors.
// Run with: ./gradlew test
// ============================================================================
class ViCaBLETapDetectorLogicTest {

    // Replicate the bump packet format from ViCaBLETapDetector
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

    // -----------------------------------------------------------------------
    // Packet format
    // -----------------------------------------------------------------------

    @Test
    fun `bump packet is 19 bytes`() {
        val packet = buildBumpPacket(System.currentTimeMillis(), 12345)
        assertEquals("Packet must be 19 bytes", 19, packet.size)
    }

    @Test
    fun `packet type byte is correct for BUMP`() {
        val packet = buildBumpPacket(0L, 0)
        assertEquals("Packet type must be PACKET_BUMP", ViCaSessionManager.PACKET_BUMP, packet[0])
    }

    @Test
    fun `timestamp round-trips through packet format`() {
        val timestamp = 1716499200000L  // a realistic epoch ms value
        // Mask to 32 bits (UInt32 range) as the protocol uses
        val masked = timestamp and 0xFFFFFFFFL
        val packet = buildBumpPacket(masked, 0)
        val decoded = extractTimestamp(packet)
        assertEquals("Timestamp must survive packet round-trip", masked, decoded)
    }

    @Test
    fun `session ID round-trips through packet`() {
        val sessionId = 0xDEADBEEF.toInt()
        val packet = buildBumpPacket(0L, sessionId)
        assertEquals("Session ID must survive packet round-trip", sessionId, extractSessionId(packet))
    }

    // -----------------------------------------------------------------------
    // Confirmation window logic
    // -----------------------------------------------------------------------

    @Test
    fun `bumps within 300ms should confirm`() {
        val localBump = 1000L
        val peerBump  = 1250L   // 250ms later — within 300ms window
        val delta = Math.abs(localBump - peerBump)
        assertTrue("Delta $delta ms should be within confirmation window",
            delta < ViCaSessionManager.CONFIRMATION_WINDOW_MS)
    }

    @Test
    fun `bumps outside 300ms should NOT confirm`() {
        val localBump = 1000L
        val peerBump  = 1400L   // 400ms later — outside window
        val delta = Math.abs(localBump - peerBump)
        assertFalse("Delta $delta ms should NOT be within confirmation window",
            delta < ViCaSessionManager.CONFIRMATION_WINDOW_MS)
        }

    @Test
    fun `bumps exactly at window boundary should NOT confirm`() {
        val localBump = 1000L
        val peerBump  = 1300L   // exactly 300ms — NOT within (strict less-than)
        val delta = Math.abs(localBump - peerBump)
        assertFalse("Exactly at boundary should NOT confirm",
            delta < ViCaSessionManager.CONFIRMATION_WINDOW_MS)
    }

    // -----------------------------------------------------------------------
    // Accelerometer threshold
    // -----------------------------------------------------------------------

    @Test
    fun `acceleration below threshold does not trigger`() {
        val lowAccel = 0.5 * 9.81  // 0.5g in m/s²
        val threshold = ViCaSessionManager.ACCEL_THRESHOLD_G * 9.81
        assertFalse("0.5g should NOT trigger", lowAccel > threshold)
    }

    @Test
    fun `acceleration above threshold triggers`() {
        val highAccel = 1.2 * 9.81  // 1.2g in m/s²
        val threshold = ViCaSessionManager.ACCEL_THRESHOLD_G * 9.81
        assertTrue("1.2g should trigger", highAccel > threshold)
    }

    @Test
    fun `accel byte clamping works correctly`() {
        // magnitude 2.0 m/s² → byte = 2.0 * 10 = 20
        val magnitude = 2.0
        val accelByte = (magnitude * 10.0).coerceIn(0.0, 255.0).toInt()
        assertEquals(20, accelByte)

        // Very large value clamps to 255
        val largeMagnitude = 300.0
        val clamped = (largeMagnitude * 10.0).coerceIn(0.0, 255.0).toInt()
        assertEquals(255, clamped)
    }

    // -----------------------------------------------------------------------
    // Debounce
    // -----------------------------------------------------------------------

    @Test
    fun `bumps within debounce window are ignored`() {
        val firstBump = System.currentTimeMillis()
        val secondBump = firstBump + 500L  // 500ms later — within 1s debounce
        val timeSinceLast = secondBump - firstBump
        assertTrue("Second bump within 1s should be debounced",
            timeSinceLast < ViCaSessionManager.BUMP_DEBOUNCE_MS)
    }

    @Test
    fun `bumps after debounce window are accepted`() {
        val firstBump = System.currentTimeMillis()
        val secondBump = firstBump + 1500L  // 1.5s later — outside debounce
        val timeSinceLast = secondBump - firstBump
        assertFalse("Second bump after 1s should NOT be debounced",
            timeSinceLast < ViCaSessionManager.BUMP_DEBOUNCE_MS)
    }
}
