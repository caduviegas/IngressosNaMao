package com.innaval.ingresosnamao.navigation

import com.innaval.ingresosnamao.feature.payment.domain.AttemptStatus
import com.innaval.ingresosnamao.feature.payment.domain.PaymentAttempt
import com.innaval.ingresosnamao.feature.ticket.domain.PurchaseItem
import com.innaval.ingresosnamao.feature.ticket.domain.SimulatedPurchase

internal fun approvedSimulation(
    enabled: Boolean,
    userId: String,
    fingerprint: String,
    attempt: PaymentAttempt?,
): SimulatedPurchase? {
    val orderId = attempt?.orderId
    if (!enabled || attempt?.status != AttemptStatus.APPROVED ||
        attempt.order.userId != userId || attempt.order.fingerprint != fingerprint || orderId.isNullOrBlank()
    ) return null
    return SimulatedPurchase(
        reference = attempt.reference,
        orderId = orderId,
        userId = userId,
        fingerprint = fingerprint,
        totalCents = attempt.order.totalCents,
        items = attempt.order.items.map { PurchaseItem(it.sku, it.name, it.unitPriceCents, it.quantity) },
    )
}
