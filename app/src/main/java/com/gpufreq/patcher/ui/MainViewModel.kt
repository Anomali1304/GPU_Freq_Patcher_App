package com.gpufreq.patcher.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpufreq.patcher.data.AppDatabase
import com.gpufreq.patcher.data.ModulePatch
import com.gpufreq.patcher.data.ModuleRepository
import com.gpufreq.patcher.util.Analysis
import com.gpufreq.patcher.util.Issue
import com.gpufreq.patcher.util.OppEngine
import com.gpufreq.patcher.util.OppRow
import com.gpufreq.patcher.util.Options
import com.gpufreq.patcher.util.PatchUtility
import com.gpufreq.patcher.util.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ModuleRepository
    val patches: StateFlow<List<ModulePatch>>

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    // OppEngine state
    var analysis by mutableStateOf<Analysis?>(null)
        private set
    val rows = mutableStateListOf<OppRow>()
    var stockRows by mutableStateOf<List<OppRow>>(emptyList())
        private set
    var issues by mutableStateOf<List<Issue>>(emptyList())
        private set
    var allowAnchorChange by mutableStateOf(false)
        private set

    val hasErrors: Boolean get() = issues.any { it.severity == Severity.ERROR }

    private var currentModuleData: ByteArray? = null
    private var currentFileName: String = ""

    fun toggleTheme() {
        val current = _isDarkTheme.value ?: false
        val next = !current
        _isDarkTheme.value = next
        prefs.edit().putBoolean("is_dark_theme", next).apply()
    }

    init {
        // Load theme preference - default to Dark (true)
        if (prefs.contains("is_dark_theme")) {
            _isDarkTheme.value = prefs.getBoolean("is_dark_theme", true)
        } else {
            _isDarkTheme.value = true
        }

        val dao = AppDatabase.getDatabase(application).moduleDao()
        repository = ModuleRepository(dao)
        patches = repository.allPatches.let { flow ->
            val state = MutableStateFlow<List<ModulePatch>>(emptyList())
            viewModelScope.launch {
                flow.collect { state.value = it }
            }
            state.asStateFlow()
        }
    }

    // Auto-adjusting Overclock State
    var autoOcTargetKhz by mutableStateOf<Int?>(null)
        private set
    var autoOcStepKhz by mutableStateOf<Int?>(null)
        private set
    var autoOcVoltTop by mutableStateOf<Int?>(null)
        private set
    var isAutoOcEnabled by mutableStateOf(false)
        private set

    val stockMaxFreq: Int? get() = stockRows.firstOrNull()?.freq
    val naturalStepKhz: Int get() = OppEngine.detectNaturalStepKhz(if (stockRows.isNotEmpty()) stockRows else rows.toList())

    fun revalidate() {
        issues = analysis?.let { OppEngine.validate(it, rows.toList(), Options(allowAnchorChange)) } ?: emptyList()
    }

    fun updateAllowAnchorChange(allow: Boolean) {
        allowAnchorChange = allow
        revalidate()
    }

    fun updateRow(i: Int, r: OppRow) {
        if (i in rows.indices) {
            // Guarantee vsram >= volt invariant
            val safeVsram = maxOf(r.volt, r.vsram)
            // Auto-compute posdiv if current posdiv fails PLL readback for new frequency
            val safePosdiv = if (!OppEngine.pllReadbackOk(r.freq, r.posdiv)) {
                (0..3).firstOrNull { OppEngine.pllReadbackOk(r.freq, it) } ?: r.posdiv
            } else {
                r.posdiv
            }
            rows[i] = r.copy(vsram = safeVsram, posdiv = safePosdiv)
            revalidate()
        }
    }

    fun autoSortDescending() {
        if (rows.size > 1) {
            val sorted = rows.sortedByDescending { it.freq }
            rows.clear()
            rows.addAll(sorted)
            revalidate()
        }
    }

    fun autoFixPllPosdiv() {
        for (i in rows.indices) {
            val r = rows[i]
            if (!OppEngine.pllReadbackOk(r.freq, r.posdiv)) {
                val validDiv = (0..3).firstOrNull { OppEngine.pllReadbackOk(r.freq, it) }
                if (validDiv != null) {
                    rows[i] = r.copy(posdiv = validDiv)
                }
            }
        }
        revalidate()
    }

    fun autoFixVsram() {
        for (i in rows.indices) {
            val r = rows[i]
            if (r.vsram < r.volt) {
                rows[i] = r.copy(vsram = r.volt)
            }
        }
        revalidate()
    }

    fun applyOverclock(topKhz: Int, stepKhz: Int, voltTop: Int, autoSync: Boolean = true) {
        val a = analysis ?: return
        val sMax = stockMaxFreq ?: rows.firstOrNull()?.freq ?: return
        autoOcTargetKhz = topKhz
        autoOcStepKhz = stepKhz
        autoOcVoltTop = voltTop
        isAutoOcEnabled = autoSync

        // Filter out existing OC rows to get base table
        val baseRows = rows.filter { it.freq <= sMax }
        val generated = OppEngine.generateOverclock(baseRows, topKhz, stepKhz, voltTop)
        // Strictly fit to slotCount so table size NEVER changes
        val fitted = OppEngine.fitToSlots(generated, a.slotCount, a.anchorFreqs.toSet()) ?: generated
        rows.clear()
        rows.addAll(fitted)
        revalidate()
    }

    fun applyOppTemplate(templateRows: List<OppRow>) {
        val a = analysis ?: return
        val fitted = OppEngine.fitToSlots(templateRows, a.slotCount, a.anchorFreqs.toSet()) ?: templateRows
        rows.clear()
        rows.addAll(fitted)
        isAutoOcEnabled = false
        autoOcTargetKhz = null
        revalidate()
    }

    fun loadExternalKoTemplate(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val result = withContext(Dispatchers.IO) {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: throw Exception("Failed to read external .ko file")
                    val a = OppEngine.analyze(bytes) { PatchUtility.findOppTable(it) }
                        ?: throw Exception("No valid OPP table found in external .ko file.")
                    a.rows
                }
                applyOppTemplate(result)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal membaca .ko lain")
            }
        }
    }

    private fun autoAdjustOc() {
        val a = analysis ?: return
        val target = autoOcTargetKhz ?: return
        val step = autoOcStepKhz ?: naturalStepKhz
        val volt = autoOcVoltTop ?: OppEngine.snap625Up((rows.firstOrNull()?.volt ?: 85000) + 50000)
        val sMax = stockMaxFreq ?: return
        val baseRows = rows.filter { it.freq <= sMax }
        if (baseRows.isNotEmpty()) {
            val generated = OppEngine.generateOverclock(baseRows, target, step, volt)
            val fitted = OppEngine.fitToSlots(generated, a.slotCount, a.anchorFreqs.toSet()) ?: generated
            rows.clear()
            rows.addAll(fitted)
        }
        revalidate()
    }

    fun setAutoOcSyncEnabled(enabled: Boolean) {
        isAutoOcEnabled = enabled
        if (enabled && autoOcTargetKhz != null) {
            autoAdjustOc()
        }
    }

    fun clearOcTiers() {
        val sMax = stockMaxFreq ?: return
        val baseRows = rows.filter { it.freq <= sMax }
        if (baseRows.isNotEmpty()) {
            rows.clear()
            rows.addAll(baseRows)
            isAutoOcEnabled = false
            autoOcTargetKhz = null
            revalidate()
        }
    }

    fun applyVoltageOffset(offsetMv: Int) {
        val delta = offsetMv * 100 // convert mV to 10uV units
        for (i in rows.indices) {
            val r = rows[i]
            val newVolt = (r.volt + delta).coerceIn(50000, 1500000)
            val newVsram = maxOf(newVolt, (r.vsram + delta).coerceIn(50000, 1500000))
            rows[i] = r.copy(volt = OppEngine.snap625Up(newVolt), vsram = OppEngine.snap625Up(newVsram))
        }
        revalidate()
    }

    fun fitSlots() {
        val a = analysis ?: return
        OppEngine.fitToSlots(rows.toList(), a.slotCount, a.anchorFreqs.toSet())?.let {
            rows.clear()
            rows.addAll(it)
        }
        revalidate()
    }

    fun loadModule(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading
            try {
                val context = getApplication<Application>()
                val result = withContext(Dispatchers.IO) {
                    // Resolve filename
                    var name: String? = null
                    if (uri.scheme == "content") {
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        try {
                            if (cursor != null && cursor.moveToFirst()) {
                                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (index != -1) {
                                    name = cursor.getString(index)
                                }
                            }
                        } finally {
                            cursor?.close()
                        }
                    }
                    if (name == null) {
                        val path = uri.path
                        val cut = path?.lastIndexOf('/') ?: -1
                        name = if (cut != -1) path?.substring(cut + 1) else path
                    }
                    val finalFileName = name ?: "mtk_gpufreq_mt6789.ko"

                    // Read bytes
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: throw Exception("Failed to read file")
                    
                    val a = OppEngine.analyze(bytes) { PatchUtility.findOppTable(it) }
                        ?: throw Exception("No valid OPP table found in module.")

                    val hwId = a.hardwareId ?: PatchUtility.detectHardwareId(bytes)
                    Triple(bytes, finalFileName, Pair(a, hwId))
                }

                val bytes = result.first
                val finalFileName = result.second
                val analyzed = result.third.first
                val hwId = result.third.second

                currentModuleData = bytes
                currentFileName = finalFileName
                analysis = analyzed
                stockRows = analyzed.rows

                rows.clear()
                rows.addAll(analyzed.rows)
                isAutoOcEnabled = false
                autoOcTargetKhz = null
                revalidate()

                _uiState.value = EditorUiState.Loaded(
                    fileName = finalFileName,
                    detectedHardwareId = hwId,
                    slotCount = analyzed.slotCount,
                    anchorFreqs = analyzed.anchorFreqs
                )
            } catch (e: Exception) {
                _uiState.value = EditorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetToStock() {
        if (stockRows.isNotEmpty()) {
            rows.clear()
            rows.addAll(stockRows)
            isAutoOcEnabled = false
            autoOcTargetKhz = null
            revalidate()
        }
    }

    fun patchAndSave(uri: Uri) {
        val data = currentModuleData ?: return
        val a = analysis ?: return
        val updatedRows = rows.toList()

        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val out = OppEngine.apply(data, a, updatedRows, Options(allowAnchorChange))
                    val outputStream: OutputStream? = getApplication<Application>().contentResolver.openOutputStream(uri)
                    outputStream?.use { it.write(out) }
                }

                if (updatedRows.isNotEmpty()) {
                    val maxRow = updatedRows.first()
                    val minRow = updatedRows.last()
                    
                    val pathString = uri.path?.let { android.net.Uri.decode(it) } ?: ""
                    val simplePath = if (pathString.contains(":")) {
                        "/sdcard/" + pathString.substringAfterLast(":")
                    } else {
                        currentFileName
                    }

                    val patch = ModulePatch(
                        fileName = currentFileName,
                        originalFreq = maxRow.freq,
                        originalVolt = maxRow.volt,
                        targetFreq = minRow.freq,
                        targetVolt = minRow.volt,
                        outputPath = simplePath
                    )
                    repository.insert(patch)
                }

                _uiState.value = EditorUiState.Success("Module kernel (.ko) berhasil di-patch dan disimpan sebagai $currentFileName!")
            } catch (e: Exception) {
                _uiState.value = EditorUiState.Error(e.message ?: "Gagal menyimpan modul kernel")
            }
        }
    }

    fun reset() {
        _uiState.value = EditorUiState.Idle
        currentModuleData = null
        currentFileName = ""
        analysis = null
        rows.clear()
        stockRows = emptyList()
        issues = emptyList()
        isAutoOcEnabled = false
        autoOcTargetKhz = null
    }

    fun deletePatch(patch: ModulePatch) {
        viewModelScope.launch {
            repository.deleteById(patch.id)
        }
    }

    fun deleteAllPatches() {
        viewModelScope.launch {
            repository.deleteAllPatches()
        }
    }
}

sealed class EditorUiState {
    object Idle : EditorUiState()
    object Loading : EditorUiState()
    data class Loaded(
        val fileName: String,
        val detectedHardwareId: String? = null,
        val slotCount: Int = 0,
        val anchorFreqs: List<Int> = emptyList()
    ) : EditorUiState()
    data class Success(val message: String) : EditorUiState()
    data class Error(val message: String) : EditorUiState()
}
