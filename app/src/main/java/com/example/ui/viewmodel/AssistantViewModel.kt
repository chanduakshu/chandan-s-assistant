package com.example.ui.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CallEntity
import com.example.data.model.CallCategory
import com.example.data.model.DetectedLanguage
import com.example.data.model.DialogueMessage
import com.example.data.model.ScreeningPresetScenario
import com.example.data.model.ScreeningResult
import com.example.data.model.SpeakerType
import com.example.data.service.ScreeningEngine
import com.example.data.service.TtsManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreeningCallState {
    IDLE,
    RINGING,
    ACTIVE_SCREENING,
    HANDED_OFF_TO_CHANDAN,
    CALL_COMPLETED
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val callDao = database.callDao()
    private val ttsManager = TtsManager(application)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, MessageJson::class.java)
    private val jsonAdapter = moshi.adapter<List<MessageJson>>(listType)

    // Call History Filters
    val allCalls = callDao.getAllCalls().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCategoryFilter = MutableStateFlow<CallCategory?>(null)
    val selectedCategoryFilter: StateFlow<CallCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredCalls = combine(allCalls, _selectedCategoryFilter, _searchQuery) { calls, category, query ->
        calls.filter { call ->
            val matchesCategory = category == null || call.category.startsWith(category.displayName.substringBefore(" /"))
            val matchesQuery = query.isBlank() ||
                    call.callerName.contains(query, ignoreCase = true) ||
                    call.organization.contains(query, ignoreCase = true) ||
                    call.summary.contains(query, ignoreCase = true) ||
                    call.phoneNumber.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Live Call Screening State
    private val _callState = MutableStateFlow(ScreeningCallState.IDLE)
    val callState: StateFlow<ScreeningCallState> = _callState.asStateFlow()

    private val _activeCallerName = MutableStateFlow("Ramesh Kumar")
    val activeCallerName: StateFlow<String> = _activeCallerName.asStateFlow()

    private val _activePhoneNumber = MutableStateFlow("+91 98450 12345")
    val activePhoneNumber: StateFlow<String> = _activePhoneNumber.asStateFlow()

    private val _activeOrganization = MutableStateFlow("ABC Logistics / BlueDart")
    val activeOrganization: StateFlow<String> = _activeOrganization.asStateFlow()

    private val _activeCategory = MutableStateFlow(CallCategory.UNKNOWN)
    val activeCategory: StateFlow<CallCategory> = _activeCategory.asStateFlow()

    private val _activeLanguage = MutableStateFlow(DetectedLanguage.ENGLISH)
    val activeLanguage: StateFlow<DetectedLanguage> = _activeLanguage.asStateFlow()

    private val _conversation = MutableStateFlow<List<DialogueMessage>>(emptyList())
    val conversation: StateFlow<List<DialogueMessage>> = _conversation.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _latestSummaryResult = MutableStateFlow<ScreeningResult?>(null)
    val latestSummaryResult: StateFlow<ScreeningResult?> = _latestSummaryResult.asStateFlow()

    private val _savedCallId = MutableStateFlow<Long?>(null)
    val savedCallId: StateFlow<Long?> = _savedCallId.asStateFlow()

    // Settings State
    val deliveryInstructionEn = MutableStateFlow("Please leave the package with the security guard at the gate. Thank you!")
    val deliveryInstructionKn = MutableStateFlow("ಪಾರ್ಸೆಲ್ ಅನ್ನು ಗೇಟ್ನಲ್ಲಿರುವ ಸೆಕ್ಯುರಿಟಿ ಗಾರ್ಡ್ ಬಳಿ ಬಿಡಿ. ಧನ್ಯವಾದಗಳು.")
    val isSecurityGuardPresent = MutableStateFlow(true)
    val autoBlockSpam = MutableStateFlow(true)
    val alertEmergency = MutableStateFlow(true)
    val saveTranscripts = MutableStateFlow(true)
    val saveAudio = MutableStateFlow(true)

    // Timer handler for active call
    private var callStartTime = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_callState.value == ScreeningCallState.ACTIVE_SCREENING || _callState.value == ScreeningCallState.HANDED_OFF_TO_CHANDAN) {
                val elapsed = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
                _callDurationSeconds.value = elapsed
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    // Scenarios Library for testing
    val presetScenarios = listOf(
        ScreeningPresetScenario(
            title = "Zomato Food Delivery",
            subtitle = "Kannada Delivery Agent",
            callerName = "Raju Delivery Partner",
            phoneNumber = "+91 97410 88234",
            organization = "Zomato Bangalore",
            category = CallCategory.DELIVERY,
            initialLanguage = DetectedLanguage.KANNADA,
            initialCallerSpeech = "ನಮಸ್ಕಾರ ಸರ್, ಝೊಮಾಟೋ ಫುಡ್ ಡೆಲಿವರಿ ತಂದಿದ್ದೇನೆ. ಗೇಟ್ ಹತ್ರ ಇದ್ದೀನಿ.",
            followUpScript = listOf(
                "ಸರ್, ಸೆಕ್ಯುರಿಟಿ ಬಳಿ ಇಡಬೇಕಾ?",
                "ಸರಿ ಸರ್, ಗೇಟ್ ಸೆಕ್ಯುರಿಟಿ ಬಳಿ ಇಟ್ಟಿದ್ದೇನೆ. ಧನ್ಯವಾದಗಳು."
            )
        ),
        ScreeningPresetScenario(
            title = "Amazon Parcel Delivery",
            subtitle = "English Courier Agent",
            callerName = "Suresh Amazon Courier",
            phoneNumber = "+91 98450 12345",
            organization = "Amazon Logistics",
            category = CallCategory.DELIVERY,
            initialLanguage = DetectedLanguage.ENGLISH,
            initialCallerSpeech = "Hello! I am Suresh from Amazon delivery. I have a package for Chandan.",
            followUpScript = listOf(
                "Can you confirm where to drop off the parcel?",
                "Got it. Package handed to security guard, thank you!"
            )
        ),
        ScreeningPresetScenario(
            title = "HDFC Credit Card Offer",
            subtitle = "Spam / Telemarketer (English)",
            callerName = "Pooja Sharma",
            phoneNumber = "+91 80 4912 3456",
            organization = "HDFC Credit Services",
            category = CallCategory.SPAM,
            initialLanguage = DetectedLanguage.ENGLISH,
            initialCallerSpeech = "Good day! I am calling from HDFC Bank to offer a pre-approved lifetime free credit card with 5 lakh loan limit.",
            followUpScript = listOf(
                "Sir, it only takes 2 minutes for digital KYC verification.",
                "Okay sir, disconnecting."
            )
        ),
        ScreeningPresetScenario(
            title = "Personal Loan Sales",
            subtitle = "Spam / Telemarketer (Kannada)",
            callerName = "Manjunath Finance",
            phoneNumber = "+91 80 2345 6789",
            organization = "Rajajinagar Loans & Finance",
            category = CallCategory.SPAM,
            initialLanguage = DetectedLanguage.KANNADA,
            initialCallerSpeech = "ನಮಸ್ಕಾರ ಸರ್, ರಾಜಾಜಿನಗರ ಫೈನಾನ್ಸ್‌ನಿಂದ ಕರೆ ಮಾಡುತ್ತಿದ್ದೇವೆ. ಕಡಿಮೆ ಬಡ್ಡಿದರದಲ್ಲಿ ವೈಯಕ್ತಿಕ ಸಾಲ ಮತ್ತು ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್ ಆಫರ್ ಇದೆ.",
            followUpScript = listOf(
                "ಕೇವಲ ಆಧಾರ್ ಕಾರ್ಡ್ ಸಾಕು ಸರ್, 5 ನಿಮಿಷದಲ್ಲಿ ಹಣ ಸಿಗುತ್ತೆ...",
                "ಸರಿ ಸರ್, ಧನ್ಯವಾದಗಳು."
            )
        ),
        ScreeningPresetScenario(
            title = "Amma (Mother)",
            subtitle = "Family Call in Kannada",
            callerName = "Amma",
            phoneNumber = "+91 94481 65432",
            organization = "Home / Family",
            category = CallCategory.PERSONAL,
            initialLanguage = DetectedLanguage.KANNADA,
            initialCallerSpeech = "ಚಂದನ್, ಅಮ್ಮ ಕಣೋ. ಸಂಜೆ ಮನೆಗೆ ಬರುವಾಗ ಹಣ್ಣು ತಗೊಂಡು ಬಾ ಅಂತ ಹೇಳೋಕೆ ಕರೆ ಮಾಡಿದೆ.",
            followUpScript = listOf(
                "ಮಲ್ಲೇಶ್ವರಂ ಮಾರ್ಕೆಟ್ ಹತ್ರ ತಗೋ ಕಣೋ.",
                "ಸರಿ, ಬೇಗ ಮನೆಗೆ ಬಾ ಊಟ ರೆಡಿ ಇದೆ."
            )
        ),
        ScreeningPresetScenario(
            title = "Tech Lead Deployment Sync",
            subtitle = "Work Call (Kannada-English)",
            callerName = "Vikram Tech Lead",
            phoneNumber = "+91 99001 77889",
            organization = "InnovateX Engineering",
            category = CallCategory.WORK,
            initialLanguage = DetectedLanguage.KANNADA_ENGLISH,
            initialCallerSpeech = "Hey Chandan, Vikram here. Deployment window schedule bagge urgent PR review maadbekittu.",
            followUpScript = listOf(
                "PR #402 check maadi ASAP, release block aagide.",
                "Meeting 5:30 PM ge reschedule aagide, please inform him."
            )
        ),
        ScreeningPresetScenario(
            title = "Hospital Emergency Lab Alert",
            subtitle = "Urgent / Emergency Call",
            callerName = "Dr. Ananya",
            phoneNumber = "+91 80 2200 9999",
            organization = "Manipal Hospital Emergency",
            category = CallCategory.URGENT,
            initialLanguage = DetectedLanguage.ENGLISH,
            initialCallerSpeech = "Hello! This is Dr. Ananya from Manipal Hospital emergency desk regarding an urgent medical report for Chandan's family.",
            followUpScript = listOf(
                "Please tell Chandan to call back immediately on extension 304.",
                "It is urgent."
            )
        )
    )

    fun setCategoryFilter(category: CallCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Call Simulation Lifecycle
    fun triggerIncomingCall(
        scenario: ScreeningPresetScenario? = null,
        customName: String = "Unknown Caller",
        customNumber: String = "+91 98860 11223",
        customOrg: String = "Incoming Call",
        initialSpeech: String = ""
    ) {
        ttsManager.stop()
        val name = scenario?.callerName ?: customName
        val number = scenario?.phoneNumber ?: customNumber
        val org = scenario?.organization ?: customOrg
        val lang = scenario?.initialLanguage ?: ScreeningEngine.detectLanguage(initialSpeech)

        _activeCallerName.value = name
        _activePhoneNumber.value = number
        _activeOrganization.value = org
        _activeLanguage.value = lang
        _activeCategory.value = scenario?.category ?: CallCategory.UNKNOWN
        _conversation.value = emptyList()
        _callDurationSeconds.value = 0
        _latestSummaryResult.value = null
        _savedCallId.value = null

        _callState.value = ScreeningCallState.RINGING
    }

    fun acceptAndScreenWithAi(initialCallerUtterance: String? = null) {
        _callState.value = ScreeningCallState.ACTIVE_SCREENING
        callStartTime = System.currentTimeMillis()
        timerHandler.post(timerRunnable)

        // Turn 1 AI Greeting according to prompt
        val greeting = if (_activeLanguage.value == DetectedLanguage.KANNADA) {
            ScreeningEngine.GREETING_KN
        } else {
            ScreeningEngine.GREETING_EN
        }

        val greetingMsg = DialogueMessage(
            sender = SpeakerType.AI_ASSISTANT,
            text = greeting,
            language = _activeLanguage.value
        )
        _conversation.value = listOf(greetingMsg)

        // Speak Greeting
        ttsManager.speak(greeting, _activeLanguage.value) {
            // After greeting is spoken, if initial caller utterance is provided, post it after short delay
            if (!initialCallerUtterance.isNullOrBlank()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    sendCallerSpeech(initialCallerUtterance)
                }, 1200)
            }
        }
    }

    fun sendCallerSpeech(text: String) {
        if (text.isBlank()) return

        val detectedLang = ScreeningEngine.detectLanguage(text)
        _activeLanguage.value = detectedLang

        val callerMsg = DialogueMessage(
            sender = SpeakerType.CALLER,
            text = text,
            language = detectedLang
        )
        _conversation.value = _conversation.value + callerMsg

        // AI processes speech
        _isAiThinking.value = true
        viewModelScope.launch {
            val response = ScreeningEngine.processCallerUtterance(
                callerText = text,
                conversation = _conversation.value,
                deliveryInstructionEn = deliveryInstructionEn.value,
                deliveryInstructionKn = deliveryInstructionKn.value
            )

            _isAiThinking.value = false
            _activeCategory.value = response.category

            val aiMsg = DialogueMessage(
                sender = SpeakerType.AI_ASSISTANT,
                text = response.aiText,
                language = response.language
            )
            _conversation.value = _conversation.value + aiMsg

            // Speak AI response
            ttsManager.speak(response.aiText, response.language)

            // Auto-disconnect if Spam
            if (response.shouldEndCall && autoBlockSpam.value) {
                Handler(Looper.getMainLooper()).postDelayed({
                    endCallAndGenerateSummary("Spam Blocked")
                }, 3500)
            }
        }
    }

    fun handoffToChandan() {
        ttsManager.stop()
        val handoffMsg = if (_activeLanguage.value == DetectedLanguage.KANNADA) {
            "ಒಂದು ಕ್ಷಣ, ನಿಮ್ಮ ಕರೆಯನ್ನು ಚಂದನ್ ಅವರಿಗೆ ಸಂಪರ್ಕಿಸುತ್ತೇನೆ."
        } else {
            "One moment, I'll connect you with Chandan."
        }

        val aiMsg = DialogueMessage(
            sender = SpeakerType.AI_ASSISTANT,
            text = handoffMsg,
            language = _activeLanguage.value
        )
        _conversation.value = _conversation.value + aiMsg
        ttsManager.speak(handoffMsg, _activeLanguage.value)

        _callState.value = ScreeningCallState.HANDED_OFF_TO_CHANDAN
    }

    fun sendChandanSpeech(text: String) {
        if (text.isBlank()) return
        val chandanMsg = DialogueMessage(
            sender = SpeakerType.CHANDAN,
            text = text,
            language = _activeLanguage.value
        )
        _conversation.value = _conversation.value + chandanMsg
    }

    fun endCallAndGenerateSummary(screeningStatusOverride: String? = null) {
        ttsManager.stop()
        timerHandler.removeCallbacks(timerRunnable)

        val duration = _callDurationSeconds.value.coerceAtLeast(12)
        val status = screeningStatusOverride ?: if (_callState.value == ScreeningCallState.HANDED_OFF_TO_CHANDAN) {
            "Handed off to Chandan"
        } else {
            "Screened by AI"
        }

        _callState.value = ScreeningCallState.CALL_COMPLETED

        // Generate Post-Call Summary
        val summary = ScreeningEngine.generatePostCallSummary(
            callerName = _activeCallerName.value,
            phoneNumber = _activePhoneNumber.value,
            organization = _activeOrganization.value,
            conversation = _conversation.value,
            category = _activeCategory.value,
            primaryLang = _activeLanguage.value
        )
        _latestSummaryResult.value = summary

        // Persist to Room Database
        viewModelScope.launch {
            val jsonMessages = _conversation.value.map {
                MessageJson(sender = it.sender.name, text = it.text, timestamp = it.timestamp)
            }
            val transcriptJsonString = if (saveTranscripts.value) {
                jsonAdapter.toJson(jsonMessages)
            } else {
                "[]"
            }

            val newEntity = CallEntity(
                callerName = _activeCallerName.value,
                phoneNumber = _activePhoneNumber.value,
                organization = summary.organization,
                timestamp = System.currentTimeMillis(),
                durationSeconds = duration,
                language = _activeLanguage.value.displayName,
                category = summary.category.displayName,
                summary = summary.summary,
                importantDetails = summary.importantDetails,
                transcriptJson = transcriptJsonString,
                hasRecording = saveAudio.value,
                recordingDuration = duration,
                isTrusted = summary.category == CallCategory.PERSONAL || summary.category == CallCategory.WORK,
                isSpam = summary.category == CallCategory.SPAM,
                screeningStatus = status
            )

            val id = callDao.insertCall(newEntity)
            _savedCallId.value = id
        }
    }

    fun dismissCallSimulator() {
        ttsManager.stop()
        timerHandler.removeCallbacks(timerRunnable)
        _callState.value = ScreeningCallState.IDLE
        _conversation.value = emptyList()
        _callDurationSeconds.value = 0
    }

    // Call details database actions
    fun toggleTrusted(callId: Long, current: Boolean) {
        viewModelScope.launch {
            callDao.updateTrusted(callId, !current)
        }
    }

    fun markAsSpam(callId: Long, isSpam: Boolean) {
        viewModelScope.launch {
            callDao.markAsSpam(callId, isSpam)
        }
    }

    fun deleteTranscript(callId: Long) {
        viewModelScope.launch {
            callDao.deleteTranscript(callId)
        }
    }

    fun deleteRecording(callId: Long) {
        viewModelScope.launch {
            callDao.deleteRecording(callId)
        }
    }

    fun deleteCall(callId: Long) {
        viewModelScope.launch {
            callDao.deleteCallById(callId)
        }
    }

    suspend fun getCallById(callId: Long): CallEntity? {
        return callDao.getCallById(callId)
    }

    fun parseTranscript(json: String): List<DialogueMessage> {
        return try {
            val list = jsonAdapter.fromJson(json) ?: return emptyList()
            list.map {
                val senderType = when (it.sender) {
                    "CALLER" -> SpeakerType.CALLER
                    "CHANDAN" -> SpeakerType.CHANDAN
                    else -> SpeakerType.AI_ASSISTANT
                }
                DialogueMessage(
                    sender = senderType,
                    text = it.text,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        timerHandler.removeCallbacks(timerRunnable)
    }
}

data class MessageJson(
    val sender: String,
    val text: String,
    val timestamp: Long
)
