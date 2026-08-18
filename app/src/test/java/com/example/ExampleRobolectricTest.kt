package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallCategory
import com.example.data.model.DetectedLanguage
import com.example.data.service.ScreeningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Chandan AI Assistant", appName)
    }

    @Test
    fun `detect Kannada language correctly`() {
        val knText = "ನಮಸ್ಕಾರ ಸರ್, ಝೊಮಾಟೋ ಫುಡ್ ಡೆಲಿವರಿ ತಂದಿದ್ದೇನೆ."
        val detected = ScreeningEngine.detectLanguage(knText)
        assertEquals(DetectedLanguage.KANNADA, detected)
    }

    @Test
    fun `detect English language correctly`() {
        val enText = "Hello! I am Suresh from Amazon delivery."
        val detected = ScreeningEngine.detectLanguage(enText)
        assertEquals(DetectedLanguage.ENGLISH, detected)
    }

    @Test
    fun `rule engine identifies spam in Kannada`() {
        val response = ScreeningEngine.evaluateRuleResponse(
            "ರಾಜಾಜಿನಗರ ಫೈನಾನ್ಸ್‌ನಿಂದ ಕರೆ ಮಾಡುತ್ತಿದ್ದೇವೆ. ಕಡಿಮೆ ಬಡ್ಡಿದರದಲ್ಲಿ ವೈಯಕ್ತಿಕ ಸಾಲ ಮತ್ತು ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್ ಆಫರ್ ಇದೆ."
        )
        assertEquals(CallCategory.SPAM, response.category)
        assertTrue(response.shouldEndCall)
    }

    @Test
    fun `rule engine handles food delivery in Kannada`() {
        val response = ScreeningEngine.evaluateRuleResponse(
            "ನಮಸ್ಕಾರ ಸರ್, ಝೊಮಾಟೋ ಡೆಲಿವರಿ ತಂದಿದ್ದೇನೆ. ಗೇಟ್ ಹತ್ರ ಇದ್ದೀನಿ."
        )
        assertEquals(CallCategory.DELIVERY, response.category)
        assertTrue(response.aiText.contains("ಸೆಕ್ಯುರಿಟಿ") || response.aiText.contains("ಗೇಟ್"))
    }
}
