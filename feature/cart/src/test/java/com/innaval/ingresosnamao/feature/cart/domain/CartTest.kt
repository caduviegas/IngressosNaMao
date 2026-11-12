package com.innaval.ingresosnamao.feature.cart.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CartTest {
    @Test fun givenCartItemsWhenCalculatingTotalThenUsesExactAmount() {
        val items = listOf(CartItem("1", "Show", 12500, 2), CartItem("2", "Outro", 1, 3))
        assertEquals(CartResult.Success(CartSnapshot(items, 25003)), calculateCart(items))
        assertEquals(CartResult.Success(CartSnapshot(emptyList(), 0)), calculateCart(emptyList()))
    }

    @Test fun givenInvalidItemsOrOverflowWhenCalculatingThenRejectsCart() {
        val good = CartItem("id", "Show", 1, 1)
        listOf(good.copy(eventId = ""), good.copy(eventName = " "), good.copy(quantity = 0),
            good.copy(unitPriceCents = -1)).forEach {
            assertEquals(CartResult.Failure(CartError.INVALID_ITEM), calculateCart(listOf(it)))
        }
        assertEquals(CartResult.Failure(CartError.INVALID_ITEM), calculateCart(listOf(good, good)))
        assertEquals(CartResult.Failure(CartError.LIMIT_EXCEEDED), calculateCart(listOf(good.copy(unitPriceCents = Long.MAX_VALUE, quantity = 2))))
        assertEquals(CartResult.Failure(CartError.LIMIT_EXCEEDED), calculateCart(listOf(good.copy(unitPriceCents = Long.MAX_VALUE), good.copy(eventId = "2"))))
    }

    @Test fun givenCartItemsWhenFingerprintingThenIgnoresOrderAndDetectsChanges() {
        val first = CartItem("1", "Show", 100, 1)
        val second = CartItem("2", "Outro", 200, 1)
        val original = cartFingerprint(CartSnapshot(listOf(first, second), 300))
        assertEquals(original, cartFingerprint(CartSnapshot(listOf(second, first), 300)))
        assertNotEquals(original, cartFingerprint(CartSnapshot(listOf(first.copy(quantity = 2), second), 400)))
    }
}
