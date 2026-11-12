package com.innaval.ingresosnamao.navigation

import com.innaval.ingresosnamao.feature.payment.domain.AttemptStatus
import com.innaval.ingresosnamao.feature.payment.domain.PaymentAttempt
import com.innaval.ingresosnamao.feature.payment.domain.PaymentItem
import com.innaval.ingresosnamao.feature.payment.domain.PaymentOrder
import org.junit.Assert.*
import org.junit.Test

class SimulatedPurchaseTest {
    private val order = PaymentOrder("alice", "fp", listOf(PaymentItem("event", "Show", 2, 100)), 200)
    private val attempt = PaymentAttempt("ref", order, 1, AttemptStatus.APPROVED, "order")

    @Test fun givenPaymentAttemptWhenMappingPurchaseThenRequiresMatchingDebugApproval() {
        val purchase = requireNotNull(approvedSimulation(true, "alice", "fp", attempt))
        assertEquals("order", purchase.orderId)
        assertEquals(2, purchase.items.single().quantity)
        assertEquals(200L, purchase.totalCents)
        assertNull(approvedSimulation(false, "alice", "fp", attempt))
        assertNull(approvedSimulation(true, "bob", "fp", attempt))
        assertNull(approvedSimulation(true, "alice", "other", attempt))
        assertNull(approvedSimulation(true, "alice", "fp", attempt.copy(orderId = null)))
        for (status in AttemptStatus.entries.filter { it != AttemptStatus.APPROVED }) {
            assertNull(approvedSimulation(true, "alice", "fp", attempt.copy(status = status)))
        }
    }
}
