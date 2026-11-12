package com.innaval.ingresosnamao.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import com.innaval.ingresosnamao.feature.events.presentation.EventsScreen
import com.innaval.ingresosnamao.feature.events.presentation.EventsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventsInstrumentedRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenStaleEmptyEventsWhenRefreshedThenShowsError() {
        var state: EventsUiState by mutableStateOf(EventsUiState.Empty(possiblyStale = true))
        compose.setContent {
            IngressosTheme {
                EventsScreen(state, onEventClick = { _, _, _ -> }, onRetry = {
                    state = EventsUiState.Error(EventsError.INVALID_DATA)
                })
            }
        }
        val robot = EventsInstrumentedRobot(compose)
        robot.result.emptyShown()
        robot.action.refresh()
        robot.result.errorShown()
    }
}
