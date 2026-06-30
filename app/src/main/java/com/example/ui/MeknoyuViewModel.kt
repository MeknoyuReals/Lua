package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScriptDatabase
import com.example.data.ScriptItem
import com.example.data.ScriptRepository
import com.example.lua.LuaEngine
import com.example.network.GeminiDeobfuscator
import com.example.network.ScriptDownloader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeknoyuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScriptRepository

    init {
        val database = ScriptDatabase.getDatabase(application)
        repository = ScriptRepository(database.scriptDao())
    }

    // Expose Room database history list reactively
    val historyState: StateFlow<List<ScriptItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Tab / Screen state ---
    private val _currentTab = MutableStateFlow(0) // 0: Obfuscator, 1: Deobfuscator, 2: History, 3: API Status
    val currentTab = _currentTab.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // --- Obfuscator View States ---
    val obfuscatorInput = MutableStateFlow("")
    val obfuscatorOutput = MutableStateFlow("")
    
    // Obfuscator options
    val optRemoveComments = MutableStateFlow(true)
    val optRenameVariables = MutableStateFlow(true)
    val optEncryptStrings = MutableStateFlow(true)
    val optInjectJunk = MutableStateFlow(false)
    val optMinify = MutableStateFlow(false)

    // --- Deobfuscator View States ---
    val deobfuscatorInput = MutableStateFlow("")
    val deobfuscatorOutput = MutableStateFlow("")
    val deobfuscatorExplanation = MutableStateFlow("") // For AI analysis markdown
    val downloadUrlInput = MutableStateFlow("")

    // --- App Global Status / Loading / Notifications ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage: SharedFlow<String> = _statusMessage.asSharedFlow()

    private fun showToast(message: String) {
        viewModelScope.launch {
            _statusMessage.emit(message)
        }
    }

    // --- Operations ---

    /**
     * Perform local string/AST pattern-based obfuscation.
     */
    fun performObfuscation() {
        val input = obfuscatorInput.value.trim()
        if (input.isEmpty()) {
            showToast("Harap masukkan skrip Lua terlebih dahulu!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = LuaEngine.obfuscate(
                    code = input,
                    removeComments = optRemoveComments.value,
                    encryptStrings = optEncryptStrings.value,
                    renameVariables = optRenameVariables.value,
                    injectJunk = optInjectJunk.value,
                    minify = optMinify.value
                )
                obfuscatorOutput.value = result

                // Save to local SQLite database
                repository.insert(
                    ScriptItem(
                        title = "Obfuscate (${getSnippetTitle(input)})",
                        type = "OBFUSCATE",
                        originalScript = input,
                        processedScript = result,
                        timestamp = System.currentTimeMillis()
                    )
                )
                showToast("Skrip berhasil disensor/di-obfuscate!")
            } catch (e: Exception) {
                showToast("Error Obfuscation: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Perform local pattern-based deobfuscation & beautification.
     */
    fun performLocalDeobfuscation() {
        val input = deobfuscatorInput.value.trim()
        if (input.isEmpty()) {
            showToast("Harap masukkan skrip yang ingin dideobfuscate!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = LuaEngine.deobfuscatePattern(input)
                deobfuscatorOutput.value = result
                deobfuscatorExplanation.value = "### Penjelasan Deobfuscator Pola Lokal\n\nSkrip ini telah diformat dan didekode secara lokal menggunakan metode pencocokan pola regular expression. String heksadesimal, string escape byte (`\\104\\101...`), dan format string `string.char` berhasil dipulihkan menjadi teks asli."

                // Save to SQLite DB
                repository.insert(
                    ScriptItem(
                        title = "Deobfuscate Lokal (${getSnippetTitle(input)})",
                        type = "DEOBFUSCATE",
                        originalScript = input,
                        processedScript = result,
                        timestamp = System.currentTimeMillis(),
                        analysis = deobfuscatorExplanation.value
                    )
                )
                showToast("Berhasil merapikan & mendekode skrip secara lokal!")
            } catch (e: Exception) {
                showToast("Error Deobfuscation Lokal: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Advanced AI-powered Deobfuscation & Semantic Variable Recovery using Gemini API.
     */
    fun performAIDeobfuscation() {
        val input = deobfuscatorInput.value.trim()
        if (input.isEmpty()) {
            showToast("Harap masukkan skrip yang ingin dianalisis!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Call Gemini Client API Integration
                val result = GeminiDeobfuscator.deobfuscateWithAI(input)
                deobfuscatorOutput.value = result.first
                deobfuscatorExplanation.value = result.second

                // Save to local database
                repository.insert(
                    ScriptItem(
                        title = "AI Deobfuscate (${getSnippetTitle(input)})",
                        type = "DEOBFUSCATE",
                        originalScript = input,
                        processedScript = result.first,
                        timestamp = System.currentTimeMillis(),
                        analysis = result.second
                    )
                )
                showToast("Deobfuscasi & analisis AI selesai dengan aman!")
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Gagal memproses deobfuscasi AI.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Download script from external URL (GitHub raw, Pastebin, etc.)
     */
    fun downloadScriptFromUrl() {
        val url = downloadUrlInput.value.trim()
        if (url.isEmpty()) {
            showToast("Masukkan tautan URL skrip terlebih dahulu!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val downloadedText = ScriptDownloader.downloadFromUrl(url)
                deobfuscatorInput.value = downloadedText
                showToast("Skrip berhasil diunduh dari situs web!")
            } catch (e: Exception) {
                showToast("Gagal mengunduh: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set a script from history into the active editor
     */
    fun loadHistoryItem(item: ScriptItem) {
        if (item.type == "OBFUSCATE") {
            obfuscatorInput.value = item.originalScript
            obfuscatorOutput.value = item.processedScript
            setTab(0) // Go to Obfuscator Tab
        } else {
            deobfuscatorInput.value = item.originalScript
            deobfuscatorOutput.value = item.processedScript
            deobfuscatorExplanation.value = item.analysis ?: ""
            setTab(1) // Go to Deobfuscator Tab
        }
        showToast("Proyek '${item.title}' berhasil dimuat!")
    }

    /**
     * Delete item from SQLite DB.
     */
    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            showToast("Proyek berhasil dihapus!")
        }
    }

    /**
     * Clear all database items.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            showToast("Semua riwayat berhasil dibersihkan!")
        }
    }

    /**
     * Helper to get a short identifier/summary of a long script for the title.
     */
    private fun getSnippetTitle(script: String): String {
        val cleaned = script.trim().replace(Regex("\\s+"), " ")
        return if (cleaned.length > 15) {
            cleaned.substring(0, 15) + "..."
        } else {
            cleaned
        }
    }

    // Class factory for custom ViewModel creation
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MeknoyuViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MeknoyuViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
