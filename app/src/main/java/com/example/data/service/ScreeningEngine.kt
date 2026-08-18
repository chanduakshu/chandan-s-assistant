package com.example.data.service

import com.example.data.api.GeminiClient
import com.example.data.model.CallCategory
import com.example.data.model.DetectedLanguage
import com.example.data.model.DialogueMessage
import com.example.data.model.ScreeningResult
import com.example.data.model.SpeakerType

object ScreeningEngine {

    const val GREETING_EN = "Hi, I'm Chandan's AI Assistant. He's currently busy. May I know who's calling and why, please?"
    const val GREETING_KN = "ನಮಸ್ಕಾರ, ನಾನು ಚಂದನ್ ಅವರ AI Assistant. ಅವರು ಈಗ ಬ್ಯುಸಿಯಾಗಿದ್ದಾರೆ. ಯಾರು ಮಾತನಾಡುತ್ತಿದ್ದೀರಿ ಮತ್ತು ಏಕೆ ಕರೆ ಮಾಡಿದ್ದೀರಿ?"

    // System prompt when using Gemini LLM
    private const val SYSTEM_PROMPT = """
You are a professional, helpful, and polite AI personal secretary screening phone calls on behalf of your user, Chandan.
Your name is Chandan AI Assistant.
Your primary objective is to find out:
1. Who is calling
2. Where they are calling from
3. Why they are calling

Supported languages: English and Kannada.
- Automatically detect whether the caller speaks English, Kannada, or Kannada-English mixed.
- Respond in the detected language.
- Keep every response brief, natural, and professional (normally under 15 words).
- Never break character. Never reveal Chandan's private info (address, passwords, OTPs, financial data).
- Never state you are an LLM.

Rules:
- Spam/Sales/Loans/Credit cards/Insurance/Investment: Say "Thank you, but Chandan isn't interested right now. Have a great day!" (Kannada: "ಧನ್ಯವಾದಗಳು, ಆದರೆ ಚಂದನ್ ಅವರಿಗೆ ಈಗ ಆಸಕ್ತಿ ಇಲ್ಲ. ಒಳ್ಳೆಯ ದಿನವಾಗಲಿ!").
- Courier/Delivery: Say "Got it. Please leave the package with the security guard at the gate. Thank you!" (Kannada: "ಸರಿ. ಪಾರ್ಸೆಲ್ ಅನ್ನು ಗೇಟ್ನಲ್ಲಿರುವ ಸೆಕ್ಯುರಿಟಿ ಗಾರ್ಡ್ ಬಳಿ ಬಿಡಿ. ಧನ್ಯವಾದಗಳು.").
- Personal/Family: Say "Thank you. I've noted your message and will inform Chandan." (Kannada: "ಧನ್ಯವಾದಗಳು. ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ದಾಖಲಿಸಿದ್ದೇನೆ ಮತ್ತು ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ.").
- Work/Professional: Say "Thank you. I've noted the details and will inform Chandan." (Kannada: "ಧನ್ಯವಾದಗಳು. ವಿವರಗಳನ್ನು ದಾಖಲಿಸಿದ್ದೇನೆ ಮತ್ತು ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ.").
- Urgent Work: Say "I understand. I'll mark this as urgent for Chandan." (Kannada: "ಅರ್ಥವಾಯಿತು. ಇದನ್ನು ಚಂದನ್ ಅವರಿಗೆ ತುರ್ತು ವಿಷಯವೆಂದು ತಿಳಿಸುತ್ತೇನೆ.").
- Emergency: Say "Understood. I'll immediately alert Chandan about this." (Kannada: "ಅರ್ಥವಾಯಿತು. ಈ ವಿಷಯವನ್ನು ತಕ್ಷಣ ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ.").
- Vague: Say "Understood. Could you please tell me what this is regarding?" (Kannada: "ಸರಿ. ಈ ಕರೆ ಯಾವ ವಿಷಯಕ್ಕೆ ಸಂಬಂಧಿಸಿದೆ ಎಂದು ದಯವಿಟ್ಟು ತಿಳಿಸುತ್ತೀರಾ?").
- Transfer: Say "One moment, I'll connect you with Chandan." (Kannada: "ಒಂದು ಕ್ಷಣ, ನಿಮ್ಮ ಕರೆಯನ್ನು ಚಂದನ್ ಅವರಿಗೆ ಸಂಪರ್ಕಿಸುತ್ತೇನೆ.").
"""

    fun detectLanguage(text: String): DetectedLanguage {
        var hasKannadaScript = false
        var hasEnglishScript = false

        for (char in text) {
            val code = char.code
            if (code in 0x0C80..0x0CFF) {
                hasKannadaScript = true
            } else if (char.isLetter()) {
                hasEnglishScript = true
            }
        }

        if (hasKannadaScript && hasEnglishScript) {
            return DetectedLanguage.KANNADA_ENGLISH
        }
        if (hasKannadaScript) {
            return DetectedLanguage.KANNADA
        }

        // Check for common Kannada words written in Latin script (Kannada-English)
        val lower = text.lowercase()
        val kannadaLatinKeywords = listOf(
            "namaskara", "hegidira", "chandan", "bagge", "maadbeku", "heli", "gotthu",
            "yaru", "enu", "bekittu", "matte", "kare", "kelasa", "mane", "amma", "appa", "houdu", "illa"
        )
        val matchesLatinKannada = kannadaLatinKeywords.count { lower.contains(it) }
        if (matchesLatinKannada >= 2) {
            return DetectedLanguage.KANNADA_ENGLISH
        }

        return DetectedLanguage.ENGLISH
    }

    suspend fun processCallerUtterance(
        callerText: String,
        conversation: List<DialogueMessage>,
        deliveryInstructionEn: String = "Please leave the package with the security guard at the gate. Thank you!",
        deliveryInstructionKn: String = "ಪಾರ್ಸೆಲ್ ಅನ್ನು ಗೇಟ್ನಲ್ಲಿರುವ ಸೆಕ್ಯುರಿಟಿ ಗಾರ್ಡ್ ಬಳಿ ಬಿಡಿ. ಧನ್ಯವಾದಗಳು."
    ): ScreeningResponse {
        val detectedLang = detectLanguage(callerText)
        val textLower = callerText.lowercase()

        // Try Gemini AI first if configured
        val historyForGemini = conversation.map { msg ->
            val role = if (msg.sender == SpeakerType.AI_ASSISTANT) "AI" else "User"
            role to msg.text
        } + listOf("User" to callerText)

        val geminiOutput = try {
            GeminiClient.generateScreeningResponse(SYSTEM_PROMPT, historyForGemini)
        } catch (e: Exception) {
            null
        }

        if (!geminiOutput.isNullOrBlank()) {
            val isSpam = geminiOutput.contains("isn't interested", ignoreCase = true) ||
                    geminiOutput.contains("ಆಸಕ್ತಿ ಇಲ್ಲ")
            val isDelivery = geminiOutput.contains("package", ignoreCase = true) ||
                    geminiOutput.contains("ಪಾರ್ಸೆಲ್") ||
                    textLower.contains("delivery") || textLower.contains("zomato") || textLower.contains("swiggy")
            val isEmergency = textLower.contains("emergency") || textLower.contains("accident") ||
                    textLower.contains("hospital") || textLower.contains("ತುರ್ತು")
            val isUrgent = isEmergency || textLower.contains("urgent") || textLower.contains("asap") ||
                    textLower.contains("ಬೇಗ")

            val category = when {
                isSpam -> CallCategory.SPAM
                isDelivery -> CallCategory.DELIVERY
                isEmergency || isUrgent -> CallCategory.URGENT
                else -> inferCategory(callerText)
            }

            return ScreeningResponse(
                aiText = geminiOutput,
                language = detectedLang,
                category = category,
                shouldEndCall = isSpam,
                isUrgent = isUrgent
            )
        }

        // Deterministic High-Precision Rule Engine (offline / guaranteed fallback)
        return evaluateRuleEngine(callerText, detectedLang, deliveryInstructionEn, deliveryInstructionKn)
    }

    fun evaluateRuleResponse(
        callerText: String,
        deliveryInstructionEn: String = "Please leave the package with the security guard at the gate. Thank you!",
        deliveryInstructionKn: String = "ಪಾರ್ಸೆಲ್ ಅನ್ನು ಗೇಟ್ನಲ್ಲಿರುವ ಸೆಕ್ಯುರಿಟಿ ಗಾರ್ಡ್ ಬಳಿ ಬಿಡಿ. ಧನ್ಯವಾದಗಳು."
    ): ScreeningResponse {
        val detectedLang = detectLanguage(callerText)
        return evaluateRuleEngine(callerText, detectedLang, deliveryInstructionEn, deliveryInstructionKn)
    }

    private fun evaluateRuleEngine(
        callerText: String,
        lang: DetectedLanguage,
        deliveryInstructionEn: String,
        deliveryInstructionKn: String
    ): ScreeningResponse {
        val text = callerText.lowercase()

        // 1. Emergency / Urgent Priority
        if (text.contains("emergency") || text.contains("hospital") || text.contains("accident") ||
            text.contains("ತುರ್ತು") || text.contains("ಆಸ್ಪತ್ರೆ") || text.contains("ತಕ್ಷಣ") ||
            text.contains("ambulance")
        ) {
            val responseText = when (lang) {
                DetectedLanguage.KANNADA -> "ಅರ್ಥವಾಯಿತು. ಈ ವಿಷಯವನ್ನು ತಕ್ಷಣ ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ."
                else -> "Understood. I'll immediately alert Chandan about this."
            }
            return ScreeningResponse(
                aiText = responseText,
                language = lang,
                category = CallCategory.URGENT,
                shouldEndCall = false,
                isUrgent = true
            )
        }

        // 2. Spam / Telemarketing / Loans / Credit cards / Insurance / Real estate / Offers
        if (text.contains("loan") || text.contains("credit card") || text.contains("insurance") ||
            text.contains("investment") || text.contains("mutual fund") || text.contains("real estate") ||
            text.contains("offer") || text.contains("pre-approved") || text.contains("preapproved") ||
            text.contains("ಕಡಿಮೆ ಬಡ್ಡಿದರ") || text.contains("ಸಾಲ") || text.contains("ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್") ||
            text.contains("ವಿಮೆ") || text.contains("ಆಫರ್")
        ) {
            val responseText = when (lang) {
                DetectedLanguage.KANNADA -> "ಧನ್ಯವಾದಗಳು, ಆದರೆ ಚಂದನ್ ಅವರಿಗೆ ಈಗ ಆಸಕ್ತಿ ಇಲ್ಲ. ಒಳ್ಳೆಯ ದಿನವಾಗಲಿ!"
                else -> "Thank you, but Chandan isn't interested right now. Have a great day!"
            }
            return ScreeningResponse(
                aiText = responseText,
                language = lang,
                category = CallCategory.SPAM,
                shouldEndCall = true,
                isUrgent = false
            )
        }

        // 3. Courier / Delivery
        if (text.contains("delivery") || text.contains("courier") || text.contains("package") ||
            text.contains("parcel") || text.contains("amazon") || text.contains("flipkart") ||
            text.contains("zomato") || text.contains("swiggy") || text.contains("bluedart") ||
            text.contains("dtdc") || text.contains("ಪಾರ್ಸೆಲ್") || text.contains("ಡೆಲಿವರಿ")
        ) {
            val responseText = when (lang) {
                DetectedLanguage.KANNADA -> "ಸರಿ. $deliveryInstructionKn"
                else -> "Got it. $deliveryInstructionEn"
            }
            return ScreeningResponse(
                aiText = responseText,
                language = lang,
                category = CallCategory.DELIVERY,
                shouldEndCall = false,
                isUrgent = false
            )
        }

        // 4. Personal / Family
        if (text.contains("amma") || text.contains("appa") || text.contains("mother") ||
            text.contains("father") || text.contains("brother") || text.contains("sister") ||
            text.contains("friend") || text.contains("ಅಮ್ಮ") || text.contains("ಅಪ್ಪ") ||
            text.contains("ಮನೆಗೆ") || text.contains("ಊಟ") || text.contains("dinner") ||
            text.contains("family") || text.contains("ನೆಂಟರು")
        ) {
            val responseText = when (lang) {
                DetectedLanguage.KANNADA -> "ಧನ್ಯವಾದಗಳು. ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ದಾಖಲಿಸಿದ್ದೇನೆ ಮತ್ತು ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ."
                else -> "Thank you. I've noted your message and will inform Chandan."
            }
            return ScreeningResponse(
                aiText = responseText,
                language = lang,
                category = CallCategory.PERSONAL,
                shouldEndCall = false,
                isUrgent = false
            )
        }

        // 5. Work / Professional
        if (text.contains("office") || text.contains("meeting") || text.contains("project") ||
            text.contains("manager") || text.contains("team") || text.contains("deployment") ||
            text.contains("code") || text.contains("review") || text.contains("client") ||
            text.contains("sync") || text.contains("sprint") || text.contains("ಕಚೇರಿ") ||
            text.contains("ಮೀಟಿಂಗ್") || text.contains("ಕೆಲಸ")
        ) {
            val isWorkUrgent = text.contains("urgent") || text.contains("asap") ||
                    text.contains("today itself") || text.contains("ತುರ್ತು") || text.contains("ಬೇಗ")
            val responseText = if (isWorkUrgent) {
                when (lang) {
                    DetectedLanguage.KANNADA -> "ಅರ್ಥವಾಯಿತು. ಇದನ್ನು ಚಂದನ್ ಅವರಿಗೆ ತುರ್ತು ವಿಷಯವೆಂದು ತಿಳಿಸುತ್ತೇನೆ."
                    else -> "I understand. I'll mark this as urgent for Chandan."
                }
            } else {
                when (lang) {
                    DetectedLanguage.KANNADA -> "ಧನ್ಯವಾದಗಳು. ವಿವರಗಳನ್ನು ದಾಖಲಿಸಿದ್ದೇನೆ ಮತ್ತು ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ."
                    else -> "Thank you. I've noted the details and will inform Chandan."
                }
            }
            return ScreeningResponse(
                aiText = responseText,
                language = lang,
                category = if (isWorkUrgent) CallCategory.URGENT else CallCategory.WORK,
                shouldEndCall = false,
                isUrgent = isWorkUrgent
            )
        }

        // 6. Vague / Unclear
        val vagueResponse = when (lang) {
            DetectedLanguage.KANNADA -> "ಸರಿ. ಈ ಕರೆ ಯಾವ ವಿಷಯಕ್ಕೆ ಸಂಬಂಧಿಸಿದೆ ಎಂದು ದಯವಿಟ್ಟು ತಿಳಿಸುತ್ತೀರಾ?"
            else -> "Understood. Could you please tell me what this is regarding?"
        }
        return ScreeningResponse(
            aiText = vagueResponse,
            language = lang,
            category = CallCategory.UNKNOWN,
            shouldEndCall = false,
            isUrgent = false
        )
    }

    private fun inferCategory(text: String): CallCategory {
        val lower = text.lowercase()
        return when {
            lower.contains("loan") || lower.contains("card") || lower.contains("offer") || lower.contains("insurance") -> CallCategory.SPAM
            lower.contains("delivery") || lower.contains("courier") || lower.contains("amazon") || lower.contains("zomato") -> CallCategory.DELIVERY
            lower.contains("emergency") || lower.contains("hospital") || lower.contains("accident") || lower.contains("urgent") -> CallCategory.URGENT
            lower.contains("meeting") || lower.contains("office") || lower.contains("project") || lower.contains("work") -> CallCategory.WORK
            lower.contains("amma") || lower.contains("appa") || lower.contains("family") || lower.contains("home") -> CallCategory.PERSONAL
            else -> CallCategory.UNKNOWN
        }
    }

    fun generatePostCallSummary(
        callerName: String,
        phoneNumber: String,
        organization: String,
        conversation: List<DialogueMessage>,
        category: CallCategory,
        primaryLang: DetectedLanguage
    ): ScreeningResult {
        val callerMessages = conversation.filter { it.sender == SpeakerType.CALLER }
        val combinedCallerSpeech = callerMessages.joinToString(" ") { it.text }

        val detectedOrg = if (organization.isNotBlank()) organization else {
            when (category) {
                CallCategory.DELIVERY -> "Delivery Services"
                CallCategory.SPAM -> "Financial Telemarketing"
                CallCategory.WORK -> "Work / Professional"
                CallCategory.PERSONAL -> "Personal Contact"
                CallCategory.URGENT -> "Priority / Urgent Contact"
                CallCategory.UNKNOWN -> "Unknown Origin"
            }
        }

        val summary = when (category) {
            CallCategory.DELIVERY -> "Caller contacted Chandan regarding a package delivery. AI provided designated drop-off instructions to leave package with security."
            CallCategory.SPAM -> "Telemarketing / Promotional call detected. AI screened and politely declined the offer on behalf of Chandan."
            CallCategory.PERSONAL -> "Personal contact reached out with an update. Message has been transcribed and noted for Chandan."
            CallCategory.WORK -> "Professional call regarding ongoing project discussions and meetings. Full transcript captured."
            CallCategory.URGENT -> "High-priority / urgent call received. Flagged immediately for Chandan's prompt attention."
            CallCategory.UNKNOWN -> "Caller screened by Chandan AI Assistant. Initial queries exchanged."
        }

        val importantDetails = if (callerMessages.isNotEmpty()) {
            "Caller said: \"${callerMessages.last().text}\""
        } else {
            "No specific details recorded."
        }

        return ScreeningResult(
            callerName = callerName,
            organization = detectedOrg,
            purpose = summary,
            category = category,
            detectedLanguage = primaryLang,
            summary = summary,
            importantDetails = importantDetails,
            nextAiResponse = ""
        )
    }
}

data class ScreeningResponse(
    val aiText: String,
    val language: DetectedLanguage,
    val category: CallCategory,
    val shouldEndCall: Boolean,
    val isUrgent: Boolean
)
