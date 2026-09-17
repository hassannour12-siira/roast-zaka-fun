package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleCVs
import com.example.data.SampleProfile
import com.example.data.local.AppDatabase
import com.example.data.local.RoastEntity
import com.example.model.AiProvider
import com.example.model.FullAnalysisResult
import com.example.model.JobAdAnalysisResult
import com.example.model.RoastIntensity
import android.content.Context
import com.example.network.AnalysisException
import com.example.network.AnalysisService
import com.example.network.ClaudeAnalysisService
import com.example.network.GeminiAnalysisService
import com.example.network.OpenAiAnalysisService
import com.example.util.CvExporter
import com.example.util.CvRewriter
import com.example.util.DocumentExtractor
import com.example.util.DocxWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class InputMethod {
    UPLOAD_FILE, LINKEDIN_PROFILE, PASTE_TEXT
}

/** Who is using the app right now. The two sides analyse different documents. */
/** Where "Apply fixes & download" has got to. */
sealed interface ExportState {
    data object Working : ExportState

    data class Saved(
        val destination: CvExporter.Destination,
        val applied: Int,
        val notApplied: List<String>
    ) : ExportState

    data class Failed(val message: String) : ExportState
}

enum class AppMode {
    /** A candidate roasting their own CV. */
    ROAST_CV,

    /** A recruiter roasting their own job advert. */
    ROAST_JOB_AD
}

class RoastViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val roastDao = db.roastDao()

    // API keys were previously held only in memory, so every app restart silently wiped
    // whatever the user typed into settings and pushed them back onto the build-time key.
    private val prefs = application.getSharedPreferences("cv_roast_prefs", Context.MODE_PRIVATE)

    val historyList: StateFlow<List<RoastEntity>> = roastDao.getAllRoasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appMode = MutableStateFlow(AppMode.ROAST_CV)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _jobAdText = MutableStateFlow("")
    val jobAdText: StateFlow<String> = _jobAdText.asStateFlow()

    private val _jobAdResult = MutableStateFlow<JobAdAnalysisResult?>(null)
    val jobAdResult: StateFlow<JobAdAnalysisResult?> = _jobAdResult.asStateFlow()

    private val _inputMethod = MutableStateFlow(InputMethod.UPLOAD_FILE)
    val inputMethod: StateFlow<InputMethod> = _inputMethod.asStateFlow()

    private val _cvText = MutableStateFlow("")
    val cvText: StateFlow<String> = _cvText.asStateFlow()

    private val _uploadedFileName = MutableStateFlow<String?>(null)
    val uploadedFileName: StateFlow<String?> = _uploadedFileName.asStateFlow()

    private val _targetJob = MutableStateFlow("")
    val targetJob: StateFlow<String> = _targetJob.asStateFlow()

    private val _intensity = MutableStateFlow(RoastIntensity.SPICY)
    val intensity: StateFlow<RoastIntensity> = _intensity.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Recruiter glasses on... 🧐")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _analysisResult = MutableStateFlow<FullAnalysisResult?>(null)
    val analysisResult: StateFlow<FullAnalysisResult?> = _analysisResult.asStateFlow()

    private val _activeResultTab = MutableStateFlow(0) // 0 = Roast, 1 = Rescue
    val activeResultTab: StateFlow<Int> = _activeResultTab.asStateFlow()

    private val _selectedProvider = MutableStateFlow(
        runCatching { AiProvider.valueOf(prefs.getString(KEY_PROVIDER, null) ?: "") }
            .getOrDefault(AiProvider.CLAUDE)
    )
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _openAiApiKey = MutableStateFlow(prefs.getString(KEY_OPENAI, "").orEmpty())
    val openAiApiKey: StateFlow<String> = _openAiApiKey.asStateFlow()

    private val _claudeApiKey = MutableStateFlow(prefs.getString(KEY_CLAUDE, "").orEmpty())
    val claudeApiKey: StateFlow<String> = _claudeApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString(KEY_GEMINI, "").orEmpty())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _showProviderSettingsDialog = MutableStateFlow(false)
    val showProviderSettingsDialog: StateFlow<Boolean> = _showProviderSettingsDialog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog: StateFlow<Boolean> = _showShareDialog.asStateFlow()

    private val _showCompareDialog = MutableStateFlow(false)
    val showCompareDialog: StateFlow<Boolean> = _showCompareDialog.asStateFlow()

    private val _previousScore = MutableStateFlow<Int?>(null)
    val previousScore: StateFlow<Int?> = _previousScore.asStateFlow()

    /** Outcome of "Apply fixes & download", or null when it has not been run. */
    private val _exportState = MutableStateFlow<ExportState?>(null)
    val exportState: StateFlow<ExportState?> = _exportState.asStateFlow()

    private var loadingCycleJob: Job? = null

    private val jobAdLoadingMessages = listOf(
        "Reading your advert like a candidate would... 👀",
        "Counting the words that mean nothing...",
        "Checking if anyone mentioned the salary...",
        "Looking for how many years of experience you asked for...",
        "Working out how many jobs this actually is...",
        "Asking a tired job seeker for their opinion...",
        "Writing you a better version... ✍️"
    )

    private val loadingMessages = listOf(
        "Recruiter glasses on... 🧐",
        "Scanning for corporate buzzwords...",
        "Counting how many times you wrote 'responsible for'...",
        "Looking for actual numbers in your bullets...",
        "Asking the ATS for emotional support...",
        "Checking if 'synergy' is mentioned more than 3 times...",
        "Converting caffeine into constructive critique...",
        "Polishing the rescue plan... 🛟"
    )

    fun setInputMethod(method: InputMethod) {
        _inputMethod.value = method
    }

    fun onCvTextChanged(text: String) {
        _cvText.value = text
        _errorMessage.value = null
    }

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
        _errorMessage.value = null
    }

    fun onJobAdTextChanged(text: String) {
        _jobAdText.value = text
        _errorMessage.value = null
    }

    fun onTargetJobChanged(job: String) {
        _targetJob.value = job
    }

    fun onIntensitySelected(intensity: RoastIntensity) {
        _intensity.value = intensity
    }

    fun setActiveResultTab(tab: Int) {
        _activeResultTab.value = tab
    }

    fun setShowShareDialog(show: Boolean) {
        _showShareDialog.value = show
    }

    fun setShowCompareDialog(show: Boolean) {
        _showCompareDialog.value = show
    }

    fun onProviderSelected(provider: AiProvider) {
        _selectedProvider.value = provider
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    fun onOpenAiApiKeyChanged(key: String) {
        _openAiApiKey.value = key
        prefs.edit().putString(KEY_OPENAI, key).apply()
    }

    fun onClaudeApiKeyChanged(key: String) {
        _claudeApiKey.value = key
        prefs.edit().putString(KEY_CLAUDE, key).apply()
    }

    fun onGeminiApiKeyChanged(key: String) {
        _geminiApiKey.value = key
        prefs.edit().putString(KEY_GEMINI, key).apply()
    }

    fun setShowProviderSettingsDialog(show: Boolean) {
        _showProviderSettingsDialog.value = show
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun removeUploadedFile() {
        _uploadedFileName.value = null
        if (_appMode.value == AppMode.ROAST_JOB_AD) _jobAdText.value = "" else _cvText.value = ""
    }

    fun loadSample(sample: SampleProfile) {
        _cvText.value = sample.cvText
        _targetJob.value = sample.targetRole
        _uploadedFileName.value = "${sample.title.replace(" ", "_")}.pdf"
        _inputMethod.value = InputMethod.PASTE_TEXT
        _errorMessage.value = null
    }

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = DocumentExtractor.extractTextFromUri(context, uri)
            result.onSuccess { doc ->
                _uploadedFileName.value = doc.fileName
                if (_appMode.value == AppMode.ROAST_JOB_AD) {
                    _jobAdText.value = doc.text
                } else {
                    _cvText.value = doc.text
                }
                _errorMessage.value = null
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to read file."
            }
        }
    }

    fun startAnalysis() {
        val text = _cvText.value.trim()
        if (text.length < 30) {
            _errorMessage.value = "Please provide more CV or profile text (at least 30 characters) so we have something real to roast!"
            return
        }

        _isAnalyzing.value = true
        _errorMessage.value = null
        startLoadingMessageCycle()

        viewModelScope.launch {
            try {
                // Ensure a nice demo pacing so the user experiences the witty loading messages
                val minDelayJob = launch { delay(2500) }

                val (service: AnalysisService, customKey: String) = when (_selectedProvider.value) {
                    AiProvider.OPENAI -> Pair(OpenAiAnalysisService, _openAiApiKey.value)
                    AiProvider.CLAUDE -> Pair(ClaudeAnalysisService, _claudeApiKey.value)
                    AiProvider.GEMINI -> Pair(GeminiAnalysisService, _geminiApiKey.value)
                }

                val result = service.analyzeProfile(
                    cvText = text,
                    targetRole = _targetJob.value.trim(),
                    intensity = _intensity.value,
                    customApiKey = customKey.ifBlank { null }
                )
                minDelayJob.join()

                _analysisResult.value = result
                _activeResultTab.value = 0 // Start on PART ONE: THE ROAST

                // Save to Room DB
                val entity = RoastEntity(
                    id = result.id,
                    candidateName = result.candidate.name,
                    candidateHeadline = result.candidate.headline,
                    openingLine = result.roast.openingLine,
                    overallScore = result.scores.overall,
                    previousScore = _previousScore.value,
                    biggestRedFlag = result.roast.biggestRedFlag,
                    intensity = result.intensity.name,
                    targetRole = result.jobTarget,
                    cvSnippet = text.take(150),
                    timestamp = result.timestamp
                )
                roastDao.insertRoast(entity)
            } catch (e: AnalysisException) {
                // A message we wrote and are willing to show verbatim.
                _errorMessage.value = e.userMessage
            } catch (e: Exception) {
                _errorMessage.value = "Analysis failed: ${e.message ?: "unknown error"}"
            } finally {
                loadingCycleJob?.cancel()
                _isAnalyzing.value = false
            }
        }
    }

    /** Roast a recruiter's job advert. Mirrors startAnalysis, different document. */
    fun startJobAdAnalysis() {
        val text = _jobAdText.value.trim()
        if (text.length < 80) {
            _errorMessage.value =
                "Paste a bit more of the job advert (at least 80 characters) so there is something to review."
            return
        }

        _isAnalyzing.value = true
        _errorMessage.value = null
        startLoadingMessageCycle(jobAdLoadingMessages)

        viewModelScope.launch {
            try {
                val minDelayJob = launch { delay(2000) }

                val (service: AnalysisService, customKey: String) = when (_selectedProvider.value) {
                    AiProvider.OPENAI -> Pair(OpenAiAnalysisService, _openAiApiKey.value)
                    AiProvider.CLAUDE -> Pair(ClaudeAnalysisService, _claudeApiKey.value)
                    AiProvider.GEMINI -> Pair(GeminiAnalysisService, _geminiApiKey.value)
                }

                val result = service.analyzeJobAd(
                    jobAdText = text,
                    intensity = _intensity.value,
                    customApiKey = customKey.ifBlank { null }
                )
                minDelayJob.join()
                _jobAdResult.value = result
            } catch (e: AnalysisException) {
                _errorMessage.value = e.userMessage
            } catch (e: Exception) {
                _errorMessage.value = "Analysis failed: ${e.message ?: "unknown error"}"
            } finally {
                loadingCycleJob?.cancel()
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Rewrite the uploaded CV with the accepted fixes and save it as a .docx.
     *
     * The edit is a find-and-replace over the user's own text, so the download can only
     * ever contain lines they already saw on the results screen.
     */
    fun applyFixesAndDownload() {
        val analysis = _analysisResult.value ?: return
        val originalCv = _cvText.value
        if (originalCv.isBlank()) {
            _exportState.value = ExportState.Failed(
                "We no longer have the original CV text, so there is nothing to rewrite."
            )
            return
        }

        _exportState.value = ExportState.Working

        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                val rewrite = CvRewriter.apply(originalCv, analysis)
                if (rewrite.appliedCount == 0) {
                    return@withContext ExportState.Failed(
                        "None of the suggested lines could be found in your CV, so there was " +
                            "nothing to change."
                    )
                }

                val fileName = CvExporter.fileNameFor(analysis.candidate.name)
                val bytes = DocxWriter.build(rewrite.text)

                CvExporter.save(getApplication(), fileName, bytes).fold(
                    onSuccess = { destination ->
                        ExportState.Saved(
                            destination = destination,
                            applied = rewrite.appliedCount,
                            notApplied = rewrite.notApplied
                        )
                    },
                    onFailure = { ExportState.Failed(it.message ?: "Could not save the file.") }
                )
            }
            _exportState.value = state
        }
    }

    fun clearExportState() {
        _exportState.value = null
    }

    private fun startLoadingMessageCycle(messages: List<String> = loadingMessages) {
        loadingCycleJob?.cancel()
        loadingCycleJob = viewModelScope.launch {
            var index = 0
            while (true) {
                _loadingMessage.value = messages[index % messages.size]
                index++
                delay(1800)
            }
        }
    }

    fun resetForNewRoast() {
        _exportState.value = null
        _analysisResult.value = null
        _jobAdResult.value = null
        _jobAdText.value = ""
        _cvText.value = ""
        _uploadedFileName.value = null
        _previousScore.value = null
        _errorMessage.value = null
        _activeResultTab.value = 0
    }

    fun prepareImproveThisOneAgain() {
        val currentScore = _analysisResult.value?.scores?.overall ?: 65
        _previousScore.value = currentScore
        _showCompareDialog.value = true
    }

    fun submitRevisedCv(newCvText: String) {
        _cvText.value = newCvText
        _showCompareDialog.value = false
        startAnalysis()
    }

    /** Explicit, user-chosen offline demo. Never substituted for a failed real call. */
    fun runOfflineSample() {
        val text = _cvText.value.trim().ifBlank { SampleCVs.samples.first().cvText }
        _errorMessage.value = null
        _analysisResult.value = com.example.network.OfflineSampleAnalysis.generateTailoredAnalysis(
            cvText = text,
            targetRole = _targetJob.value.trim(),
            intensity = _intensity.value
        )
        _activeResultTab.value = 0
    }

    private companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_OPENAI = "openai_key"
        const val KEY_CLAUDE = "claude_key"
        const val KEY_GEMINI = "gemini_key"
    }
}
