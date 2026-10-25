package com.gpufreq.patcher

import com.gpufreq.patcher.util.*
import org.junit.Assert.*
import org.junit.Test

class OppEngineUnitTest {

    @Test
    fun testPllReadback() {
        // Known MT6789 freq and posdiv
        assertTrue(OppEngine.pllReadbackOk(1100000, 1))
        assertTrue(OppEngine.pllReadbackOk(700000, 2))
        assertTrue(OppEngine.pllReadbackOk(390000, 3))
        assertFalse(OppEngine.pllReadbackOk(1086000, 7))
        assertFalse(OppEngine.pllReadbackOk(-1, 1))
    }

    @Test
    fun testGenerateOverclockAndFit() {
        val baseRows = listOf(
            OppRow(freq = 1100000, volt = 85000, vsram = 85000, posdiv = 1, vaging = 0),
            OppRow(freq = 900000, volt = 80000, vsram = 80000, posdiv = 1, vaging = 0),
            OppRow(freq = 700000, volt = 75000, vsram = 75000, posdiv = 2, vaging = 0),
            OppRow(freq = 390000, volt = 70000, vsram = 70000, posdiv = 3, vaging = 0)
        )

        val oc = OppEngine.generateOverclock(baseRows, topKhz = 1300000, stepKhz = 50000, voltTop = 90000)
        assertTrue(oc.size > baseRows.size)
        assertEquals(1300000, oc.first().freq)
        assertEquals(90000, oc.first().volt)

        val fitted = OppEngine.fitToSlots(oc, slots = 4, protectedFreqs = setOf(1100000, 700000, 390000))
        assertNotNull(fitted)
        assertEquals(4, fitted!!.size)
        assertTrue(fitted.any { it.freq == 1100000 })
        assertTrue(fitted.any { it.freq == 700000 })
        assertTrue(fitted.any { it.freq == 390000 })
    }

    @Test
    fun testDetectHardwareId() {
        val sample = "test_string_with_mt6789_inside_binary_data".toByteArray(Charsets.ISO_8859_1)
        val hw = PatchUtility.detectHardwareId(sample)
        assertEquals("mt6789", hw)

        val empty = ByteArray(20)
        assertNull(PatchUtility.detectHardwareId(empty))
    }

    @Test
    fun testDetectNaturalStep() {
        val rows25 = listOf(
            OppRow(freq = 1000000, volt = 85000, vsram = 85000, posdiv = 1, vaging = 0),
            OppRow(freq = 975000, volt = 83000, vsram = 83000, posdiv = 1, vaging = 0),
            OppRow(freq = 950000, volt = 81000, vsram = 81000, posdiv = 1, vaging = 0)
        )
        assertEquals(25000, OppEngine.detectNaturalStepKhz(rows25))

        val rows50 = listOf(
            OppRow(freq = 1100000, volt = 85000, vsram = 85000, posdiv = 1, vaging = 0),
            OppRow(freq = 1050000, volt = 83000, vsram = 83000, posdiv = 1, vaging = 0)
        )
        assertEquals(50000, OppEngine.detectNaturalStepKhz(rows50))
    }

    @Test
    fun testFlexibleRowCountValidation() {
        val analysis = Analysis(
            tableOffset = 0,
            slotCount = 45,
            rows = emptyList(),
            avsOffset = null,
            avsAnchors = emptyList(),
            agingOffset = null,
            hardwareId = "mt6789",
            strictProfile = false
        )

        // 47 rows (added 2)
        val rows47 = (0 until 47).map {
            OppRow(freq = 1000000 - it * 10000, volt = 80000, vsram = 80000, posdiv = 1, vaging = 0)
        }
        val issues47 = OppEngine.validate(analysis, rows47)
        assertFalse(issues47.any { it.severity == Severity.ERROR })
        assertTrue(issues47.any { it.severity == Severity.WARNING && it.message.contains("diubah") })

        // 40 rows (reduced 5)
        val rows40 = (0 until 40).map {
            OppRow(freq = 1000000 - it * 10000, volt = 80000, vsram = 80000, posdiv = 1, vaging = 0)
        }
        val issues40 = OppEngine.validate(analysis, rows40)
        assertFalse(issues40.any { it.severity == Severity.ERROR })
        assertTrue(issues40.any { it.severity == Severity.WARNING && it.message.contains("diubah") })
    }

    @Test
    fun testGradualOverclockSteps() {
        val baseRows = listOf(
            OppRow(freq = 1000000, volt = 85000, vsram = 85000, posdiv = 1, vaging = 0),
            OppRow(freq = 975000, volt = 83000, vsram = 83000, posdiv = 1, vaging = 0),
            OppRow(freq = 950000, volt = 81000, vsram = 81000, posdiv = 1, vaging = 0)
        )
        // Step detected as 25000 kHz. Target 1075000 kHz -> should produce 1075000, 1050000, 1025000
        val oc = OppEngine.generateOverclock(baseRows, topKhz = 1075000)
        assertEquals(6, oc.size)
        assertEquals(1075000, oc[0].freq)
        assertEquals(1050000, oc[1].freq)
        assertEquals(1025000, oc[2].freq)
        assertEquals(1000000, oc[3].freq) // base top
        assertEquals(975000, oc[4].freq)
        assertEquals(950000, oc[5].freq)

        // Verify exact stock step between each adjacent tier
        for (i in 0 until oc.size - 1) {
            assertEquals(25000, oc[i].freq - oc[i + 1].freq)
        }
    }
}
