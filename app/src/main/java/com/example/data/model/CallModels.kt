package com.example.data.model

data class DialogueMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: SpeakerType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: DetectedLanguage = DetectedLanguage.ENGLISH
)

enum class SpeakerType {
    CALLER,
    AI_ASSISTANT,
    CHANDAN // Human handoff
}

enum class DetectedLanguage(val displayName: String, val nativeName: String) {
    ENGLISH("English", "English"),
    KANNADA("Kannada", "ಕನ್ನಡ"),
    KANNADA_ENGLISH("Kannada-English", "ಕನ್ನಡ-English");

    companion object {
        fun fromString(value: String): DetectedLanguage {
            return when (value.lowercase()) {
                "kannada" -> KANNADA
                "kannada-english", "kannada_english", "mixed" -> KANNADA_ENGLISH
                else -> ENGLISH
            }
        }
    }
}

enum class CallCategory(val displayName: String, val colorHex: Long) {
    SPAM("Spam / Telemarketing", 0xFFEF4444),
    DELIVERY("Delivery / Courier", 0xFF10B981),
    PERSONAL("Personal / Family", 0xFF8B5CF6),
    WORK("Work / Professional", 0xFF3B82F6),
    URGENT("Emergency / Urgent", 0xFFF59E0B),
    UNKNOWN("Unknown / General", 0xFF64748B);

    companion object {
        fun fromString(value: String): CallCategory {
            return when (value.uppercase()) {
                "SPAM" -> SPAM
                "DELIVERY" -> DELIVERY
                "PERSONAL" -> PERSONAL
                "WORK" -> WORK
                "URGENT", "EMERGENCY" -> URGENT
                else -> UNKNOWN
            }
        }
    }
}

data class ScreeningPresetScenario(
    val title: String,
    val subtitle: String,
    val callerName: String,
    val phoneNumber: String,
    val organization: String,
    val category: CallCategory,
    val initialLanguage: DetectedLanguage,
    val initialCallerSpeech: String,
    val followUpScript: List<String>
)

data class ScreeningResult(
    val callerName: String,
    val organization: String,
    val purpose: String,
    val category: CallCategory,
    val detectedLanguage: DetectedLanguage,
    val summary: String,
    val importantDetails: String,
    val nextAiResponse: String,
    val shouldEndCall: Boolean = false,
    val isUrgent: Boolean = false
)
