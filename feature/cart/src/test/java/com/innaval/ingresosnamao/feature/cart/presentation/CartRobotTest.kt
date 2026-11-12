package com.innaval.ingresosnamao.feature.cart.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartSnapshot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CartRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenCartStatesWhenInteractedWithThenBlocksEmptyAndForwardsActions() {
        val item = CartItem("event", "Show", 100, 1)
        var state by mutableStateOf(CartUiState(loading = false, cart = CartSnapshot(emptyList(), 0)))
        var quantityChange = 0
        var removed: CartItem? = null
        compose.setContent {
            IngressosTheme {
                CartScreen(state, { _, delta -> quantityChange = delta }, { removed = it }, {}, {}, {})
            }
        }
        val robot = CartRobot(compose)
        robot.result.emptyShown()
        robot.result.checkoutDisabled()
        compose.runOnIdle { state = state.copy(cart = CartSnapshot(listOf(item), 100)) }
        robot.action.increase()
        robot.action.remove()
        compose.runOnIdle {
            assertEquals(1, quantityChange)
            assertEquals(item, removed)
        }
    }

    @Test fun givenStorageErrorWhenDisplayedThenOffersRetry() {
        var retries = 0
        compose.setContent {
            IngressosTheme {
                CartScreen(
                    CartUiState(loading = false, error = CartError.STORAGE),
                    { _, _ -> }, {}, {}, {}, { retries++ },
                )
            }
        }
        val robot = CartRobot(compose)
        robot.result.storageErrorShown()
        robot.action.retry()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
