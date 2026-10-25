package com.gpufreq.patcher.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PatchUtility {

    data class OppInfo(
        val freq: Int,
        val volt: Int,
        val vsram: Int = volt,
        val posdiv: Int = 1,
        val vaging: Int = 0,
        val power: Int = 0,
        val offset: Int
    )

    private const val ENTRY_STRIDE = 24

    private val KNOWN_TABLE_NAMES = listOf(
        "g_default_gpu",
        "g_gpu_default_opp_table",
        "g_signed_gpu",
        "g_working_gpu",
        "g_opp_table_gpu"
    )

    private data class ElfSymbol(
        val name: String,
        val value: Long,
        val size: Long,
        val secIdx: Int,
        val isObject: Boolean
    )

    private data class ElfSection(
        val idx: Int,
        val name: String,
        val fileOffset: Long,
        val size: Long,
        val isNobits: Boolean
    )

    data class ConfidenceResult(
        val score: Int,
        val entryCount: Int
    )

    /**
     * Calculates confidence score (0-4) for a candidate OPP table slice.
     */
    fun confidenceScore(data: ByteArray, offset: Int, size: Int): ConfidenceResult {
        val n = size / ENTRY_STRIDE
        if (n < 4 || offset < 0 || offset + size > data.size) {
            return ConfidenceResult(0, 0)
        }

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val freqs = IntArray(n)
        val volts = IntArray(n)
        val vsrams = IntArray(n)
        val posdivs = IntArray(n)

        for (i in 0 until n) {
            val pos = offset + i * ENTRY_STRIDE
            freqs[i] = buffer.getInt(pos)
            volts[i] = buffer.getInt(pos + 4)
            vsrams[i] = buffer.getInt(pos + 8)
            posdivs[i] = buffer.getInt(pos + 12)
        }

        // Check 1: freqs between 50_000 and 5_000_000, and non-increasing
        if (freqs.any { it !in 50_000..5_000_000 }) {
            return ConfidenceResult(0, 0)
        }

        var freqNonInc = 0
        for (i in 1 until n) {
            if (freqs[i] <= freqs[i - 1]) freqNonInc++
        }
        if (freqNonInc < n - 2) {
            return ConfidenceResult(0, 0)
        }

        var score = 1 // freq column valid & monotonically descending

        // Check 2: volts column valid (50_000 to 1_500_000 uV) and non-increasing
        if (volts.all { it in 50_000..1_500_000 }) {
            var voltNonInc = 0
            for (i in 1 until n) {
                if (volts[i] <= volts[i - 1]) voltNonInc++
            }
            if (voltNonInc >= n - 3) {
                score++
            }
        }

        // Check 3: vsram >= volt
        if ((0 until n).all { volts[it] <= vsrams[it] }) {
            score++
        }

        // Check 4: posdiv in {1, 2, 4, 8, 16}
        val validPosdivs = setOf(1, 2, 4, 8, 16)
        if (posdivs.all { it in validPosdivs }) {
            score++
        }

        return ConfidenceResult(score, n)
    }

    private fun looksLikeOppTable(data: ByteArray, offset: Int, size: Int, minConfidence: Int = 3): Boolean {
        return confidenceScore(data, offset, size).score >= minConfidence
    }

    private fun parseElf(data: ByteArray): Pair<Map<Int, ElfSection>, List<ElfSymbol>>? {
        if (data.size < 64) return null
        if (data[0] != 0x7F.toByte() || data[1] != 'E'.code.toByte() || data[2] != 'L'.code.toByte() || data[3] != 'F'.code.toByte()) {
            return null
        }

        val is64Bit = data[4].toInt() == 2
        val isLittleEndian = data[5].toInt() == 1
        if (!isLittleEndian) return null

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val e_shoff: Long
        val e_shentsize: Int
        val e_shnum: Int
        val e_shstrndx: Int

        if (is64Bit) {
            e_shoff = buffer.getLong(0x28)
            e_shentsize = buffer.getShort(0x3A).toInt() and 0xFFFF
            e_shnum = buffer.getShort(0x3C).toInt() and 0xFFFF
            e_shstrndx = buffer.getShort(0x3E).toInt() and 0xFFFF
        } else {
            e_shoff = buffer.getInt(0x20).toLong() and 0xFFFFFFFFL
            e_shentsize = buffer.getShort(0x2E).toInt() and 0xFFFF
            e_shnum = buffer.getShort(0x30).toInt() and 0xFFFF
            e_shstrndx = buffer.getShort(0x32).toInt() and 0xFFFF
        }

        if (e_shoff <= 0 || e_shentsize <= 0 || e_shnum <= 0) return null
        if (e_shoff + e_shnum.toLong() * e_shentsize > data.size) return null

        val sections = mutableMapOf<Int, ElfSection>()
        var symtabSec: ElfSection? = null
        var strtabSecIndex = -1

        val shstrHeaderOff = (e_shoff + e_shstrndx * e_shentsize).toInt()
        val shstrOff = if (is64Bit) {
            buffer.getLong(shstrHeaderOff + 0x18).toInt()
        } else {
            buffer.getInt(shstrHeaderOff + 0x10)
        }

        for (i in 0 until e_shnum) {
            val off = (e_shoff + i * e_shentsize).toInt()
            if (off + e_shentsize > data.size) break
            val sh_name = buffer.getInt(off + 0x00)
            val sh_type = buffer.getInt(off + 0x04)
            val sh_offset: Long
            val sh_size: Long
            val sh_link: Int

            if (is64Bit) {
                sh_offset = buffer.getLong(off + 0x18)
                sh_size = buffer.getLong(off + 0x20)
                sh_link = buffer.getInt(off + 0x28)
            } else {
                sh_offset = buffer.getInt(off + 0x10).toLong() and 0xFFFFFFFFL
                sh_size = buffer.getInt(off + 0x14).toLong() and 0xFFFFFFFFL
                sh_link = buffer.getInt(off + 0x18)
            }

            val name = readNullTerminatedString(data, shstrOff + sh_name)
            val isNobits = sh_type == 8
            val sec = ElfSection(i, name, sh_offset, sh_size, isNobits)
            sections[i] = sec

            if (sh_type == 2) { // SHT_SYMTAB
                symtabSec = sec
                strtabSecIndex = sh_link
            }
        }

        if (symtabSec == null || strtabSecIndex !in sections.keys) return null
        val strtabSec = sections[strtabSecIndex] ?: return null

        val symbols = mutableListOf<ElfSymbol>()
        val symOff = symtabSec.fileOffset.toInt()
        val symSize = symtabSec.size.toInt()
        val entSize = if (is64Bit) 24 else 16
        val strOff = strtabSec.fileOffset.toInt()

        val numSyms = symSize / entSize
        for (i in 0 until numSyms) {
            val off = symOff + i * entSize
            if (off + entSize > data.size) break

            val st_name: Int
            val st_value: Long
            val st_size: Long
            val st_info: Int
            val st_shndx: Int

            if (is64Bit) {
                st_name = buffer.getInt(off + 0x00)
                st_info = buffer.get(off + 0x04).toInt() and 0xFF
                st_shndx = buffer.getShort(off + 0x06).toInt() and 0xFFFF
                st_value = buffer.getLong(off + 0x08)
                st_size = buffer.getLong(off + 0x10)
            } else {
                st_name = buffer.getInt(off + 0x00)
                st_value = buffer.getInt(off + 0x04).toLong() and 0xFFFFFFFFL
                st_size = buffer.getInt(off + 0x08).toLong() and 0xFFFFFFFFL
                st_info = buffer.get(off + 0x0C).toInt() and 0xFF
                st_shndx = buffer.getShort(off + 0x0E).toInt() and 0xFFFF
            }

            val name = readNullTerminatedString(data, strOff + st_name)
            val type = st_info and 0x0F
            val isObject = type == 1 // STT_OBJECT

            symbols.add(ElfSymbol(name, st_value, st_size, st_shndx, isObject))
        }

        return Pair(sections, symbols)
    }

    private fun readNullTerminatedString(data: ByteArray, offset: Int): String {
        if (offset < 0 || offset >= data.size) return ""
        var end = offset
        while (end < data.size && data[end] != 0.toByte()) {
            end++
        }
        return String(data, offset, end - offset, Charsets.UTF_8)
    }

    private fun fileOffsetOf(sym: ElfSymbol, sections: Map<Int, ElfSection>, dataSize: Int): Int? {
        val sec = sections[sym.secIdx] ?: return null
        if (sec.isNobits) return null
        val fileOff = sec.fileOffset + sym.value
        if (fileOff < 0 || fileOff >= dataSize) return null
        return fileOff.toInt()
    }

    fun findOppTable(data: ByteArray): Pair<Int, Int>? {
        val parsedElf = try { parseElf(data) } catch (e: Exception) { null }

        if (parsedElf != null) {
            val (sections, symbols) = parsedElf

            // 1. Check known table symbol names
            for (wanted in KNOWN_TABLE_NAMES) {
                for (sym in symbols) {
                    if (sym.name == wanted) {
                        val off = fileOffsetOf(sym, sections, data.size) ?: continue
                        if (looksLikeOppTable(data, off, sym.size.toInt())) {
                            return Pair(off, sym.size.toInt())
                        }
                    }
                }
            }

            // 2. Fallback: scan all OBJECT symbols in data/rodata
            val candidates = mutableListOf<Pair<Int, Int>>()
            for (sym in symbols) {
                if (sym.size < ENTRY_STRIDE * 4) continue
                val off = fileOffsetOf(sym, sections, data.size) ?: continue
                if (looksLikeOppTable(data, off, sym.size.toInt())) {
                    candidates.add(Pair(off, sym.size.toInt()))
                }
            }

            if (candidates.isNotEmpty()) {
                // Select candidate with maximum size (entry count)
                return candidates.maxByOrNull { it.second }
            }
        }

        // 3. Fallback: scan raw binary data for contiguous OPP table pattern
        var bestOffset = -1
        var bestSize = 0
        var bestScore = 0

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i <= data.size - ENTRY_STRIDE * 4) {
            val f = try { buffer.getInt(i) } catch (e: Exception) { 0 }
            if (f in 50_000..5_000_000) {
                var len = 0
                var currPos = i

                while (currPos + ENTRY_STRIDE <= data.size) {
                    val cf = buffer.getInt(currPos)
                    val cv = buffer.getInt(currPos + 4)
                    val cvs = buffer.getInt(currPos + 8)
                    val cpd = buffer.getInt(currPos + 12)

                    // Sanity check longgar di sini saja (0..16 mencakup literal
                    // power-of-2 MAUPUN index enum gen baru spt mt6897). Validasi
                    // ketat (literal vs enum + korelasi struktural) tetap dilakukan
                    // belakangan oleh confidenceScore() terhadap seluruh run --
                    // kalau strict di sini, run terpotong prematur & entry freq
                    // rendah di chip ber-encoding-enum (spt mt6897) hilang.
                    if (cf in 50_000..5_000_000 &&
                        cv in 50_000..1_500_000 &&
                        cvs in 50_000..1_500_000 &&
                        cpd in 0..16
                    ) {
                        len++
                        currPos += ENTRY_STRIDE
                    } else {
                        break
                    }
                }

                if (len >= 4) {
                    val candidateSize = len * ENTRY_STRIDE
                    val conf = confidenceScore(data, i, candidateSize)
                    if (conf.score >= 3) {
                        if (conf.score > bestScore || (conf.score == bestScore && candidateSize > bestSize)) {
                            bestScore = conf.score
                            bestOffset = i
                            bestSize = candidateSize
                        }
                    }
                    i += len * ENTRY_STRIDE
                    continue
                }
            }
            i += 4
        }

        if (bestOffset != -1 && bestSize > 0) {
            return Pair(bestOffset, bestSize)
        }

        return null
    }

    fun detectHardwareId(data: ByteArray): String? {
        val counts = mutableMapOf<String, Int>()
        val len = data.size
        var i = 0
        while (i <= len - 6) {
            val b0 = data[i].toInt().toChar().lowercaseChar()
            val b1 = data[i + 1].toInt().toChar().lowercaseChar()
            if (b0 == 'm' && b1 == 't') {
                val d1 = data[i + 2].toInt().toChar()
                val d2 = data[i + 3].toInt().toChar()
                val d3 = data[i + 4].toInt().toChar()
                val d4 = data[i + 5].toInt().toChar()
                if (d1.isDigit() && d2.isDigit() && d3.isDigit() && d4.isDigit()) {
                    val key = "mt$d1$d2$d3$d4"
                    counts[key] = (counts[key] ?: 0) + 1
                    i += 5
                }
            }
            i++
        }
        return counts.maxByOrNull { it.value }?.key
    }

    fun detectAllOpps(data: ByteArray): List<OppInfo> {
        val (tableOffset, tableSize) = findOppTable(data) ?: return emptyList()
        val n = tableSize / ENTRY_STRIDE
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val opps = mutableListOf<OppInfo>()
        for (i in 0 until n) {
            val pos = tableOffset + i * ENTRY_STRIDE
            val f = buffer.getInt(pos)
            val v = buffer.getInt(pos + 4)
            val vs = buffer.getInt(pos + 8)
            val pd = buffer.getInt(pos + 12)
            val va = buffer.getInt(pos + 16)
            val pw = buffer.getInt(pos + 20)

            opps.add(
                OppInfo(
                    freq = f,
                    volt = v,
                    vsram = vs,
                    posdiv = pd,
                    vaging = va,
                    power = pw,
                    offset = pos
                )
            )
        }

        return opps
    }

    /**
     * PENTING: semua field di sini nullable & default-nya `null`, BUKAN
     * "diturunkan" dari field lain (mis. vsram default = volt). Kalau
     * parameter tidak diisi, byte aslinya di `data` TIDAK disentuh sama
     * sekali. Ini mencegah kasus: user cuma niat ubah freq, tapi vsram
     * ikut ketimpa diam-diam karena defaultnya disamakan dengan volt --
     * padahal di banyak entry (terutama freq rendah), vsram != volt.
     */
    fun patchModule(
        data: ByteArray,
        offset: Int,
        newFreq: Int? = null,
        newVolt: Int? = null,
        newVsram: Int? = null,
        posdiv: Int? = null,
        vaging: Int? = null,
        power: Int? = null
    ): ByteArray {
        val result = data.copyOf()
        val buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)

        // putInt(index, value) = absolute write, tidak bergantung buffer
        // position -> tiap field independen, tidak ada risiko urutan salah.
        newFreq?.let  { buffer.putInt(offset, it) }
        newVolt?.let  { buffer.putInt(offset + 4, it) }
        newVsram?.let { buffer.putInt(offset + 8, it) }
        posdiv?.let   { buffer.putInt(offset + 12, it) }
        vaging?.let   { buffer.putInt(offset + 16, it) }
        power?.let    { buffer.putInt(offset + 20, it) }

        return result
    }
}
