package com.innaval.ingresosnamao.feature.cart.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class CartRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun increase() = compose.onNodeWithContentDescription("Aumentar quantidade de Show").performClick()
        fun remove() = compose.onNodeWithText("Remover").performClick()
        fun retry() = compose.onNodeWithText("Tentar novamente").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun emptyShown() = compose.onNodeWithText("Seu carrinho está vazio.").assertExists()
        fun checkoutDisabled() = compose.onNodeWithText("Finalizar compra").assertIsNotEnabled()
        fun storageErrorShown() =
            compose.onNodeWithText("Não foi possível acessar o carrinho. Tente novamente.").assertExists()
    }
}
