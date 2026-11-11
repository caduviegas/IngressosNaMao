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

    @Test fun loadingButtonIsDisabledAndAnnounced() {
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

    @Test fun fieldExposesSpecificErrorAndLoadingHasProgressSemantics() {
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

    @Test fun errorActionCallsProvidedCallback() {
        var count = 0
        compose.setContent { IngressosTheme { ErrorState("Falha", { count++ }, retryLabel = "Voltar") } }
        compose.onNodeWithText("Falha").assertExists()
        compose.onNodeWithText("Voltar").performClick()
        compose.runOnIdle { assertEquals(1, count) }
    }

    @Test fun emptyStateActionIsOptionalAndInteractive() {
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

    @Test fun errorCanBeShownWithoutAnAction() {
        compose.setContent { IngressosTheme { ErrorState("Indisponível") } }
        compose.onNodeWithText("Indisponível").assertExists()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test fun clickableComponentsHaveMinimumTouchSizeAndDisabledCardIsDisabled() {
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

    @Test fun buttonStylesAndTopBarExposeAccessibleSemantics() {
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
}
