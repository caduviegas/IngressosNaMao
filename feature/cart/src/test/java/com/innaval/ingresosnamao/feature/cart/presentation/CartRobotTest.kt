package com.innaval.ingresosnamao.feature.cart.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

private class CartRobot(private val compose: ComposeTestRule) {
    fun assertEmpty() = compose.onNodeWithText("Seu carrinho está vazio.").assertExists()
    fun assertCheckoutDisabled() = compose.onNodeWithText("Finalizar compra").assertIsNotEnabled()
    fun increase() = compose.onNodeWithContentDescription("Aumentar quantidade de Show").performClick()
    fun remove() = compose.onNodeWithText("Remover").performClick()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CartRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyCartBlocksCheckoutAndItemActionsAreForwarded() {
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
        robot.assertEmpty()
        robot.assertCheckoutDisabled()
        compose.runOnIdle { state = state.copy(cart = CartSnapshot(listOf(item), 100)) }
        robot.increase()
        robot.remove()
        compose.runOnIdle {
            assertEquals(1, quantityChange)
            assertEquals(item, removed)
        }
    }

    @Test fun storageErrorOffersRetry() {
        var retries = 0
        compose.setContent {
            IngressosTheme {
                CartScreen(
                    CartUiState(loading = false, error = CartError.STORAGE),
                    { _, _ -> }, {}, {}, {}, { retries++ },
                )
            }
        }
        compose.onNodeWithText("Não foi possível acessar o carrinho. Tente novamente.").assertExists()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
