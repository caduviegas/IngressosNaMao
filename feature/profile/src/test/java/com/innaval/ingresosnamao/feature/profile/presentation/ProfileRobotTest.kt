package com.innaval.ingresosnamao.feature.profile.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test fun profileShowsMissingFieldsAndLogoutConfirmation() {
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
        compose.onNodeWithText("Sair da conta?").assertIsDisplayed()
        compose.onNodeWithText("Sair").performClick()
        compose.runOnIdle { assertEquals(1, confirmed); assertEquals(0, requested) }
    }

    @Test fun missingDataAndReadErrorAreVisible() {
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
        compose.onNodeWithText("Não foi possível carregar seu perfil. Tente novamente.").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.onNodeWithText("Sair da conta").assertExists()
        compose.runOnIdle { assertEquals(1, retry) }
    }

    @Test fun unavailableAddressAndCachedDataAreVisible() {
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
        compose.onNodeWithText("Ana").assertIsDisplayed()
        compose.onNodeWithText("ana@example.com").assertIsDisplayed()
        compose.onNodeWithText("Não informado").assertIsDisplayed()
        compose.onNodeWithText("Dados salvos no dispositivo; podem estar desatualizados.").assertIsDisplayed()
    }
}
