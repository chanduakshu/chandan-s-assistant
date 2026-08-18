package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.CallEntity
import com.example.ui.components.CallCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun callCard_screenshot() {
        val sampleCall = CallEntity(
            id = 1,
            callerName = "Raju Delivery Partner",
            phoneNumber = "+91 97410 88234",
            organization = "Zomato Food Delivery",
            timestamp = 1700000000000L,
            durationSeconds = 45,
            language = "Kannada",
            category = "Food & Package Delivery",
            summary = "Zomato delivery partner instructed to leave food parcel with gate security guard.",
            importantDetails = "Left with security. Food order delivered.",
            transcriptJson = "[]",
            hasRecording = true,
            recordingDuration = 45,
            isTrusted = false,
            isSpam = false,
            screeningStatus = "Screened by AI"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CallCard(call = sampleCall, onClick = {})
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/call_card.png")
    }
}
