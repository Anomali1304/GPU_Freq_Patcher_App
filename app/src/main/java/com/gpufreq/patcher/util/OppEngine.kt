package com.gpufreq.patcher.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * OppEngine - pengganti PatchUtility.patchModule() yang sadar struktur driver gpufreq.
 *
 * Yang ditambahkan dibanding patch per-entri:
 *  - edit SEMUA kolom (freq, volt, vsram, posdiv, vaging) + tambah / hapus / geser baris (jumlah slot tetap)
 *  - anchor AVS (g_avs_adj) otomatis ikut pindah ke baris dengan freq anchor yang sama
 *  - g_aging_table ikut disinkronkan (kalau strukturnya terbukti sama dengan kolom vaging)
 *  - validasi aturan driver MT6789 (urutan freq, readback PLL, vsram, anchor) SEBELUM menulis file
 *
 * Tidak bergantung pada Android; hanya java.nio + kotlin stdlib.
 *
 * CATATAN PENTING: jumlah baris (slot) TIDAK bisa diubah dengan patch data saja, karena jumlah OPP
 * tertanam sebagai immediate di kode driver. "Tambah baris" = pakai slot lain (lihat fitToSlots).
 */
data class OppRow(
    val freq: Int,    // kHz
    val volt: Int,    // satuan 10 uV (85000 = 850 mV)
    val vsram: Int,   // satuan 10 uV
    val posdiv: Int,  // log2 pembagi PLL (1 = /2)
    val vaging: Int,  // satuan 10 uV, dikurangkan dari volt bila aging aktif
    val power: Int = 0
)

enum class Severity { ERROR, WARNING }

data class Issue(val severity: Severity, val row: Int?, val message: String) {
    override fun toString(): String =
        (if (severity == Severity.ERROR) "ERROR" else "WARN") + (row?.let { " [#$it]" } ?: "") + ": " + message
}

class Analysis(
    val tableOffset: Int,
    val slotCount: Int,
    val rows: List<OppRow>,
    val avsOffset: Int?,          // file offset g_avs_adj (3 struct x 20 byte) atau null
    val avsAnchors: List<Int>,    // idx anchor asli (mis. 0, 32, 44)
    val agingOffset: Int?,        // file offset g_aging_table (set pertama = vaging per idx) atau null
    val hardwareId: String?,
    val strictProfile: Boolean    // aturan driver MT6789 berlaku sebagai ERROR
) {
    /** Freq anchor asli: dicek driver terhadap efuse saat boot. */
    val anchorFreqs: List<Int> get() = avsAnchors.map { rows[it].freq }
}

class Options(val allowAnchorFreqChange: Boolean = false)

object OppEngine {

    val KNOWN_TABLE_NAMES = listOf(
        "g_default_gpu", "g_gpu_default_opp_table", "g_signed_gpu", "g_working_gpu", "g_opp_table_gpu"
    )

    // ------------------------------------------------------------------ ELF
    private class Sym(val off: Int, val size: Int)

    private fun symbols(data: ByteArray): Map<String, Sym>? {
        if (data.size < 64 || data[0] != 0x7f.toByte() || data[1] != 'E'.code.toByte() ||
            data[2] != 'L'.code.toByte() || data[3] != 'F'.code.toByte()
        ) return null
        if (data[4].toInt() != 2 || data[5].toInt() != 1) return null // hanya ELF64 little-endian
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val shoff = b.getLong(0x28)
        val shentsize = b.getShort(0x3A).toInt() and 0xffff
        val shnum = b.getShort(0x3C).toInt() and 0xffff
        if (shoff <= 0 || shentsize < 64 || shnum <= 0 || shoff + shnum.toLong() * shentsize > data.size) return null
        class Sec(val type: Int, val off: Long, val size: Long, val link: Int)
        val secs = (0 until shnum).map {
            val o = (shoff + it.toLong() * shentsize).toInt()
            Sec(b.getInt(o + 4), b.getLong(o + 24), b.getLong(o + 32), b.getInt(o + 40))
        }
        val symtab = secs.firstOrNull { it.type == 2 } ?: return null
        val strtab = secs.getOrNull(symtab.link) ?: return null
        val out = HashMap<String, Sym>()
        var o = symtab.off
        val end = symtab.off + symtab.size
        while (o + 24 <= end && o + 24 <= data.size) {
            val p = o.toInt()
            val nameOff = b.getInt(p)
            val info = data[p + 4].toInt() and 0xff
            val shndx = b.getShort(p + 6).toInt() and 0xffff
            val value = b.getLong(p + 8)
            val size = b.getLong(p + 16)
            if ((info and 0xf) == 1 && shndx in 1 until shnum) {
                val sec = secs[shndx]
                if (sec.type != 8) { // bukan NOBITS
                    val fo = sec.off + value
                    if (fo in 0 until data.size && size in 0..Int.MAX_VALUE.toLong()) {
                        var s = (strtab.off + nameOff).toInt()
                        val st = s
                        while (s < data.size && data[s] != 0.toByte()) s++
                        val name = String(data, st, s - st, Charsets.UTF_8)
                        out.putIfAbsent(name, Sym(fo.toInt(), size.toInt()))
                    }
                }
            }
            o += 24
        }
        return out
    }

    private fun detectHardwareId(data: ByteArray): String? = PatchUtility.detectHardwareId(data)

    private fun readRows(b: ByteBuffer, off: Int, n: Int): List<OppRow> = (0 until n).map {
        val p = off + it * 24
        OppRow(b.getInt(p), b.getInt(p + 4), b.getInt(p + 8), b.getInt(p + 12), b.getInt(p + 16), b.getInt(p + 20))
    }

    // -------------------------------------------------------------- analyze
    /** [fallback] = locator heuristik lama (PatchUtility.findOppTable) untuk .ko yang simbolnya di-strip. */
    fun analyze(data: ByteArray, fallback: ((ByteArray) -> Pair<Int, Int>?)? = null): Analysis? {
        val syms = runCatching { symbols(data) }.getOrNull()
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        var table: Pair<Int, Int>? = null
        if (syms != null) for (n in KNOWN_TABLE_NAMES) {
            val s = syms[n] ?: continue
            if (s.size >= 96 && s.size % 24 == 0 && s.off + s.size <= data.size && b.getInt(s.off) in 50000..5000000) {
                table = s.off to s.size; break
            }
        }
        if (table == null) table = fallback?.invoke(data)
        val (off, size) = table ?: return null
        val n = size / 24
        val rows = readRows(b, off, n)
        val hw = detectHardwareId(data)

        // g_avs_adj: 3 struct {idx, freq, volt, vsram, vaging} (20 byte). Terima hanya kalau bentuknya cocok.
        var avsOff: Int? = null
        var anchors = emptyList<Int>()
        syms?.get("g_avs_adj")?.let { s ->
            if (s.size >= 60 && s.size % 20 == 0 && s.off + 60 <= data.size) {
                val idx = (0 until 3).map { b.getInt(s.off + it * 20) }
                val zeros = (0 until 3).all { k -> (1..4).all { b.getInt(s.off + k * 20 + it * 4) == 0 } }
                if (zeros && idx.all { it in 0 until n } && idx[0] < idx[1] && idx[1] < idx[2]) {
                    avsOff = s.off; anchors = idx
                }
            }
        }
        // g_aging_table: dianggap sinkron hanya kalau set pertama == kolom vaging (bukti struktur sama).
        var agingOff: Int? = null
        syms?.get("g_aging_table")?.let { s ->
            if (s.size >= n * 4 && s.off + n * 4 <= data.size &&
                (0 until n).all { b.getInt(s.off + it * 4) == rows[it].vaging }
            ) agingOff = s.off
        }
        return Analysis(off, n, rows, avsOff, anchors, agingOff, hw, hw == "mt6789")
    }

    // ------------------------------------------------------------- validate
    /** Readback PLL persis seperti __gpufreq_freq_scale_gpu / __gpufreq_init_opp_idx (MT6789). */
    fun pllReadbackOk(freqKhz: Int, posdiv: Int): Boolean {
        if (posdiv !in 0..7 || freqKhz <= 0) return false
        val u = ((freqKhz / 100).toLong() shl posdiv) shl 14
        if (u >= (1L shl 32)) return false
        val pcw = (u / 26 + 5) / 10
        if (pcw >= (1L shl 22)) return false
        val rb = (((pcw * 0x104 + 0x82) shr 14) shr posdiv) * 100
        return rb == freqKhz.toLong()
    }

    fun validate(a: Analysis, rows: List<OppRow>, opt: Options = Options()): List<Issue> {
        val out = ArrayList<Issue>()
        fun err(r: Int?, m: String) = out.add(Issue(Severity.ERROR, r, m))
        fun warn(r: Int?, m: String) = out.add(Issue(Severity.WARNING, r, m))

        if (rows.size != a.slotCount) {
            val diff = rows.size - a.slotCount
            val detail = if (diff > 0) "+$diff baris dari stock (${a.slotCount} slot)" else "$diff baris dari stock (${a.slotCount} slot)"
            warn(null, "Jumlah baris tabel diubah ($detail). Tabel akan disimpan dengan ukuran ${rows.size} baris.")
        }
        for (i in rows.indices) {
            val r = rows[i]
            if (i > 0 && r.freq >= rows[i - 1].freq) err(i, "freq harus turun ketat (${rows[i - 1].freq} -> ${r.freq})")
            if (r.vsram < r.volt) err(i, "vsram (${r.vsram}) < volt (${r.volt})")
            if (r.freq !in 50000..5000000) err(i, "freq ${r.freq} kHz di luar rentang wajar")
            if (r.volt !in 50000..1500000) err(i, "volt ${r.volt} di luar rentang wajar (500 mV..15 V?)")
            if (i > 0 && r.volt > rows[i - 1].volt) warn(i, "volt lebih tinggi dari OPP di atasnya (tidak monoton)")
            if (a.strictProfile) {
                if (r.freq % 100 != 0) err(i, "freq harus kelipatan 100 kHz (readback PLL membulatkan 100 kHz)")
                if (r.freq !in 375000..1900000) err(i, "freq di luar batas driver 375000..1900000 kHz")
                else if (!pllReadbackOk(r.freq, r.posdiv))
                    err(i, "readback PLL tidak persis untuk freq ${r.freq} posdiv ${r.posdiv} -> __gpufreq_abort saat DVFS")
                if (r.vsram - r.volt > 20000) err(i, "vsram - volt > 200 mV (tabel mapping vsram driver tidak menemukan pasangan)")
                if (r.volt % 625 != 0) warn(i, "volt bukan kelipatan 6.25 mV (step PMIC); akan dibulatkan regulator")
                if (r.vsram < 75000) warn(i, "vsram di bawah lantai 750 mV yang dipakai stock")
                val vco = r.freq.toLong() * (1L shl r.posdiv.coerceIn(0, 7)) / 1000
                if (vco < 1500 || vco > 3800) warn(i, "VCO $vco MHz di luar rentang yang dipakai stock (~1560..3792)")
            }
        }
        // anchor AVS: freq harus tetap ada (dicek ke efuse saat boot)
        if (a.avsOffset != null) {
            val set = rows.map { it.freq }.toSet()
            for ((k, f) in a.anchorFreqs.withIndex()) if (f !in set) {
                val m = "anchor AVS #$k ($f kHz) hilang/berubah: driver membandingkan dengan efuse saat boot " +
                    "(abort kalau efuse terisi & tidak cocok)"
                if (opt.allowAnchorFreqChange) warn(null, m) else err(null, m)
            }
        }
        return out
    }

    // ---------------------------------------------------------------- apply
    /** Tulis tabel + anchor AVS + aging. Melempar IllegalArgumentException berisi semua ERROR kalau tidak valid. */
    fun apply(data: ByteArray, a: Analysis, rows: List<OppRow>, opt: Options = Options()): ByteArray {
        val errors = validate(a, rows, opt).filter { it.severity == Severity.ERROR }
        require(errors.isEmpty()) { errors.joinToString("\n") }
        
        val totalSlots = maxOf(a.slotCount, rows.size)
        val neededSize = a.tableOffset + totalSlots * 24
        val out = if (neededSize > data.size) data.copyOf(neededSize) else data.copyOf()
        val b = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        
        // Tulis semua baris yang ada
        rows.forEachIndexed { i, r ->
            val p = a.tableOffset + i * 24
            b.putInt(p, r.freq)
            b.putInt(p + 4, r.volt)
            b.putInt(p + 8, r.vsram)
            b.putInt(p + 12, r.posdiv)
            b.putInt(p + 16, r.vaging)
            b.putInt(p + 20, r.power)
        }
        
        // Jika baris berkurang (misal 45 -> 40), bersihkan slot sisa dengan nilai 0 agar driver berhenti
        if (rows.size < a.slotCount) {
            for (i in rows.size until a.slotCount) {
                val p = a.tableOffset + i * 24
                b.putInt(p, 0); b.putInt(p + 4, 0); b.putInt(p + 8, 0)
                b.putInt(p + 12, 0); b.putInt(p + 16, 0); b.putInt(p + 20, 0)
            }
        }

        a.avsOffset?.let { off ->
            val freqs = a.anchorFreqs
            val newIdx = freqs.map { f -> rows.indexOfFirst { it.freq == f } }
            if (newIdx.all { it >= 0 }) newIdx.forEachIndexed { k, idx -> b.putInt(off + k * 20, idx) }
        }

        a.agingOffset?.let { off ->
            val count = minOf(rows.size, (out.size - off) / 4)
            for (i in 0 until count) {
                b.putInt(off + i * 4, rows[i].vaging)
            }
            if (rows.size < a.slotCount && off + a.slotCount * 4 <= out.size) {
                for (i in rows.size until a.slotCount) {
                    b.putInt(off + i * 4, 0)
                }
            }
        }
        return out
    }

    // ------------------------------------------------------- helper untuk UI
    fun snap625Up(v: Int) = ((v + 624) / 625) * 625

    /** Deteksi interval step alami (kHz) dari entri-entri teratas stock table. */
    fun detectNaturalStepKhz(base: List<OppRow>): Int {
        if (base.size >= 2) {
            val diff0 = base[0].freq - base[1].freq
            if (diff0 in 5000..200000 && diff0 % 100 == 0) {
                if (base.size >= 3) {
                    val diff1 = base[1].freq - base[2].freq
                    if (diff1 in 5000..200000 && diff1 % 100 == 0) {
                        return if (diff0 == diff1) diff0 else minOf(diff0, diff1)
                    }
                }
                return diff0
            }
        }
        return 25000 // default 25 MHz jika tidak terdeteksi
    }

    /**
     * Hasilkan OPP bertahap di atas freq tertinggi stock dengan step yang sama persis seperti stock.
     * Membuat entri: ref.freq + step, ref.freq + 2*step, ... sampai mencapai topKhz.
     * Mengembalikan daftar entri baru (descending) disambung dengan [base].
     */
    fun generateOverclock(
        base: List<OppRow>, 
        topKhz: Int, 
        stepKhz: Int = detectNaturalStepKhz(base), 
        voltTop: Int = snap625Up((base.firstOrNull()?.volt ?: 85000) + 50000)
    ): List<OppRow> {
        if (base.isEmpty()) return emptyList()
        val ref = base.first()
        val step = if (stepKhz >= 100 && stepKhz % 100 == 0) stepKhz else detectNaturalStepKhz(base)
        if (topKhz <= ref.freq) return base

        // Buat daftar frekuensi bertahap naik dari ref.freq + step ke target topKhz
        val newFreqs = mutableListOf<Int>()
        var currFreq = ref.freq + step
        while (currFreq <= topKhz) {
            newFreqs.add(currFreq)
            currFreq += step
        }
        if (newFreqs.isEmpty() || newFreqs.last() < topKhz) {
            if (topKhz > (newFreqs.lastOrNull() ?: ref.freq)) {
                newFreqs.add(topKhz)
            }
        }

        // Urutkan menurun (paling tinggi di atas)
        newFreqs.reverse()

        val ocRows = ArrayList<OppRow>()
        for (f in newFreqs) {
            val t = (f - ref.freq).toDouble() / (topKhz - ref.freq).coerceAtLeast(1)
            val v = snap625Up((ref.volt + (voltTop - ref.volt) * t).toInt())
            val vs = maxOf(v, snap625Up((ref.vsram + (voltTop - ref.vsram) * t).toInt()))
            val pd = (0..3).firstOrNull { pllReadbackOk(f, it) } ?: ref.posdiv
            ocRows.add(OppRow(freq = f, volt = v, vsram = vs, posdiv = pd, vaging = 0))
        }

        return ocRows + base
    }

    /**
     * Sesuaikan jumlah baris ke [slots] tanpa menyentuh baris terlindungi (anchor), baris pertama & terakhir.
     * Kelebihan: buang baris yang paling "rapat" (span tetangga terkecil). Kekurangan: sisipkan titik tengah
     * di celah terlebar. Hasil harus tetap divalidasi.
     */
    fun fitToSlots(rows: List<OppRow>, slots: Int, protectedFreqs: Set<Int>): List<OppRow>? {
        val r = rows.toMutableList()
        while (r.size > slots) {
            var best = -1; var bestSpan = Int.MAX_VALUE
            for (i in 1 until r.size - 1) {
                if (r[i].freq in protectedFreqs) continue
                val span = r[i - 1].freq - r[i + 1].freq
                if (span < bestSpan) { bestSpan = span; best = i }
            }
            if (best < 0) return null
            r.removeAt(best)
        }
        while (r.size < slots) {
            var gi = -1; var gap = 0
            for (i in 1 until r.size) { val g = r[i - 1].freq - r[i].freq; if (g > gap) { gap = g; gi = i } }
            val hi = r[gi - 1]; val lo = r[gi]
            val f = ((hi.freq + lo.freq) / 2 / 1000) * 1000
            if (f <= lo.freq || f >= hi.freq) return null
            val t = (hi.freq - f).toDouble() / (hi.freq - lo.freq)
            val v = snap625Up((hi.volt + (lo.volt - hi.volt) * t).toInt())
            val vs = maxOf(v, snap625Up((hi.vsram + (lo.vsram - hi.vsram) * t).toInt()))
            val pd = listOf(hi.posdiv, lo.posdiv).firstOrNull { pllReadbackOk(f, it) } ?: lo.posdiv
            r.add(gi, OppRow(f, v, vs, pd, lo.vaging))
        }
        return r
    }
}
