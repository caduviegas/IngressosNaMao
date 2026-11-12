package com.innaval.ingresosnamao.feature.profile.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenProfileWithMissingFieldsWhenLoggingOutThenShowsConfirmation() {
        var requested = 0
        var confirmed = 0
        compose.setContent {
            IngressosTheme {
                ProfileScreen(
                    ProfileUiState(
                        loading = false,
                        identity = ProfileIdentity("uid", null, null),
                        profile = UserProfile("Ana", null, "Rua 1", true),
                        confirmingLogout = true,
                    ),
                    onRetry = {},
                    onLogout = { requested++ },
                    onCancelLogout = {},
                    onConfirmLogout = { confirmed++ },
                    onBack = {},
                )
            }
        }
        val robot = ProfileRobot(compose)
        robot.result.logoutConfirmationShown()
        robot.action.confirmLogout()
        compose.runOnIdle { assertEquals(1, confirmed); assertEquals(0, requested) }
    }

    @Test fun givenMissingDataAndReadErrorWhenDisplayedThenShowsMessages() {
        var retry = 0
        compose.setContent {
            IngressosTheme {
                ProfileScreen(
                    ProfileUiState(
                        loading = false,
                        identity = ProfileIdentity("uid", null, null),
                        error = ProfileError.READ,
                    ),
                    onRetry = { retry++ },
                    onLogout = {},
                    onCancelLogout = {},
                    onConfirmLogout = {},
                    onBack = {},
                )
            }
        }
        val robot = ProfileRobot(compose)
        robot.result.readErrorShown()
        robot.action.retry()
        robot.result.logoutAvailable()
        compose.runOnIdle { assertEquals(1, retry) }
    }

    @Test fun givenUnavailableAddressAndCachedDataWhenDisplayedThenShowsBoth() {
        compose.setContent {
            IngressosTheme {
                ProfileScreen(
                    ProfileUiState(
                        loading = false,
                        identity = ProfileIdentity("uid", "Ana", "ana@example.com"),
                        profile = UserProfile("Ana", "ana@example.com", null, true),
                    ),
                    onRetry = {},
                    onLogout = {},
                    onCancelLogout = {},
                    onConfirmLogout = {},
                    onBack = {},
                )
            }
        }
        ProfileRobot(compose).result.cachedProfileShown()
    }
}
