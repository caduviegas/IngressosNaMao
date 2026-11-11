package com.innaval.ingresosnamao.feature.payment.domain

import com.innaval.ingresosnamao.core.common.result.AppResult

data class PaymentReceipt(val orderId: String, val paidCents: Long)

interface PaymentRepository {
    suspend fun pay(amountCents: Long): AppResult<PaymentReceipt>
}

class PayUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(amountCents: Long): AppResult<PaymentReceipt> {
        require(amountCents > 0) { "amount must be positive" }
        return repository.pay(amountCents)
    }
}
