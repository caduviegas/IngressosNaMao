package com.innaval.ingresosnamao.core.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenLoadingButtonWhenDisplayedThenDisablesAndAnnouncesProgress() {
        var loading by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            IngressosTheme {
                IngressosButton("Pagar", { clicks++ }, loading = loading)
            }
        }
        compose.onNodeWithText("Pagar").assertIsEnabled().performClick()
        compose.runOnIdle { loading = true }
        compose.onNodeWithText("Pagar").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Carregando"))
        compose.onNodeWithText("Pagar").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun givenFieldErrorAndLoadingWhenDisplayedThenExposesSemantics() {
        compose.setContent {
            IngressosTheme {
                androidx.compose.foundation.layout.Column {
                    IngressosTextField("", {}, "E-mail", errorMessage = "Informe um e-mail válido")
                    LoadingState(label = "Buscando eventos")
                }
            }
        }
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Informe um e-mail válido")
        )
        compose.onNodeWithContentDescription("Buscando eventos")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
    }

    @Test fun givenErrorActionWhenClickedThenCallsCallback() {
        var count = 0
        compose.setContent { IngressosTheme { ErrorState("Falha", { count++ }, retryLabel = "Voltar") } }
        compose.onNodeWithText("Falha").assertExists()
        compose.onNodeWithText("Voltar").performClick()
        compose.runOnIdle { assertEquals(1, count) }
    }

    @Test fun givenEmptyStateWhenActionProvidedThenAllowsInteraction() {
        var count = 0
        compose.setContent {
            IngressosTheme {
                EmptyState("Nenhum evento", actionLabel = "Atualizar", onAction = { count++ })
            }
        }
        compose.onNodeWithText("Nenhum evento").assertExists()
        compose.onNodeWithText("Atualizar").performClick()
        compose.runOnIdle { assertEquals(1, count) }
    }

    @Test fun givenErrorWithoutActionWhenDisplayedThenShowsMessage() {
        compose.setContent { IngressosTheme { ErrorState("Indisponível") } }
        compose.onNodeWithText("Indisponível").assertExists()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test fun givenClickableComponentsWhenDisplayedThenMeetTouchSizeAndDisabledState() {
        compose.setContent {
            IngressosTheme {
                androidx.compose.foundation.layout.Column {
                    IngressosButton("Confirmar", {})
                    IngressosCard(onClick = {}, enabled = false) { androidx.compose.material3.Text("Evento") }
                }
            }
        }
        compose.onNodeWithText("Confirmar").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Evento").assertIsNotEnabled().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test fun givenButtonStylesAndTopBarWhenDisplayedThenExposeSemantics() {
        var clicks = 0
        compose.setContent {
            IngressosTheme {
                androidx.compose.foundation.layout.Column {
                    IngressosTopBar("Eventos")
                    IngressosButtonStyle.entries.forEach { style ->
                        IngressosButton(style.name, { clicks++ }, style = style)
                    }
                }
            }
        }
        compose.onNodeWithText("Eventos").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        IngressosButtonStyle.entries.forEach { compose.onNodeWithText(it.name).assertIsEnabled().performClick() }
        compose.runOnIdle { assertEquals(4, clicks) }
    }

    @Test fun givenBackBarWhenClickedThenNavigatesAndRespectsDisabledState() {
        var enabled by mutableStateOf(true)
        var backs = 0
        compose.setContent {
            IngressosTheme {
                IngressosBackScreen("Detalhes", { backs++ }, backEnabled = enabled) {
                    androidx.compose.material3.Text("Conteúdo")
                }
            }
        }
        val back = compose.onNodeWithContentDescription("Voltar")
        back.assertIsEnabled().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        val title = compose.onNodeWithText("Detalhes").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithText("Conteúdo").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(title.bottom <= content.top)
        compose.runOnIdle { enabled = false }
        back.assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test fun givenBothThemesWhenHeaderDisplayedThenShowsNameAboveContent() {
        var dark by mutableStateOf(false)
        compose.setContent {
            IngressosTheme(darkTheme = dark) {
                androidx.compose.foundation.layout.Column {
                    IngressosBrandHeader()
                    androidx.compose.material3.Text("Conteúdo da tela")
                }
            }
        }
        repeat(2) {
            val header = compose.onNodeWithText("Ingressos na Mão").fetchSemanticsNode().boundsInRoot
            val content = compose.onNodeWithText("Conteúdo da tela").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue(header.bottom < content.top)
            compose.runOnIdle { dark = true }
        }
    }
}
