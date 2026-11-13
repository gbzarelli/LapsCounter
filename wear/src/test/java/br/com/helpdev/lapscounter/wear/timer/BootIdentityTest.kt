package br.com.helpdev.lapscounter.wear.timer

import org.junit.Assert.*
import org.junit.Test

class BootIdentityTest {
    @Test fun absentOrUnreadableBootCountUsesProcessIdentity() {
        val missing = readBootIdentity { -1 }
        assertTrue(missing.startsWith("process:"))
        assertEquals(missing, readBootIdentity { throw SecurityException("restricted") })
        assertEquals("boot:4", readBootIdentity { 4 })
        assertNotEquals(readBootIdentity { 4 }, readBootIdentity { 5 })
        assertNotEquals(missing, readBootIdentity { 0 })
    }
}
