package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.theme.PlayPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authScreen_togglesSignInAndSignUpModes_andTriggersGoogleAuth() {
        var capturedMode: AuthMode? = null
        var authSucceeded = false

        composeRule.setContent {
            PlayPulseTheme(darkTheme = true) {
                AuthScreen(
                    onAuthSuccess = { authSucceeded = true },
                    enableAutoSignIn = false,
                    onInteractiveGoogleAuth = { mode, onOk, _, _ ->
                        capturedMode = mode
                        onOk(Unit)
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Access your analytics workspace").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with Google").performScrollTo().assertIsDisplayed()

        // Switch to Sign Up mode
        composeRule.onNodeWithTag("auth_mode_sign_up").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create your PlayPulse account").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sign up with Google").performScrollTo().assertIsDisplayed()

        // Trigger Google Sign-Up action
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AuthMode.SIGN_UP, capturedMode)
        assertTrue(authSucceeded)
    }

    @Test
    fun authScreen_displaysAndDismissesErrorBanner() {
        composeRule.setContent {
            PlayPulseTheme(darkTheme = true) {
                AuthScreen(
                    onAuthSuccess = {},
                    enableAutoSignIn = false,
                    onInteractiveGoogleAuth = { _, _, onErr, _ ->
                        onErr("Network unavailable. Check connection.")
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("auth_error_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Network unavailable. Check connection.").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("auth_mode_sign_up").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_error_banner").assertDoesNotExist()
    }
}
