package com.innaval.ingresosnamao.feature.events.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class EventsRobot(private val compose: ComposeTestRule) {
    fun assertEmpty() = compose.onNodeWithText("Nenhum evento disponível no momento.").assertExists()
    fun assertError() = compose.onNodeWithText("A lista de eventos recebida é inválida.").assertExists()
    fun retry() = compose.onNodeWithText("Tentar novamente").performClick()
    fun refresh() = compose.onNodeWithText("Atualizar").performClick()
    fun selectEvent() = compose.onNode(hasText("Show") and hasClickAction()).performClick()
    fun assertEventDisabled() = compose.onNode(hasText("Show") and hasClickAction()).assertIsNotEnabled()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EventsRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyCacheWarningAndErrorOfferAppropriateActions() {
        var state: EventsUiState by mutableStateOf(EventsUiState.Empty(possiblyStale = true))
        var retries = 0
        compose.setContent { IngressosTheme { EventsScreen(state, { _, _, _ -> }, { retries++ }) } }
        val robot = EventsRobot(compose)
        robot.assertEmpty()
        robot.refresh()
        compose.runOnIdle { state = EventsUiState.Error(EventsError.INVALID_DATA) }
        robot.assertError()
        robot.retry()
        compose.runOnIdle { assertEquals(2, retries) }
    }

    @Test fun selectingAnEventForwardsItsIdentityAndIsBlockedWhileAdding() {
        val event = EventUiModel("event-1", "file:///missing-image", "Show", "R$ 10,00", 1000, 5)
        var adding by mutableStateOf(false)
        var selected: Triple<String, String, Long>? = null
        compose.setContent {
            IngressosTheme {
                EventsScreen(
                    EventsUiState.Content(listOf(event), possiblyStale = false),
                    { id, name, price -> selected = Triple(id, name, price) },
                    {}, adding,
                )
            }

        }
        val robot = EventsRobot(compose)
        robot.selectEvent()
        compose.runOnIdle {
            assertEquals(Triple("event-1", "Show", 1000L), selected)
            adding = true
        }
        robot.assertEventDisabled()
    }

    @Test fun soldOutEventShowsAvailabilityAndCannotBeSelected() {
        val event = EventUiModel("10", "file:///missing-image", "Show", "R$ 10,00", 1000, 0)
        compose.setContent {
            IngressosTheme { EventsScreen(EventsUiState.Content(listOf(event), false), { _, _, _ -> }, {}) }
        }
        compose.onNodeWithText("Esgotado").assertExists()
        EventsRobot(compose).assertEventDisabled()
    }
}
