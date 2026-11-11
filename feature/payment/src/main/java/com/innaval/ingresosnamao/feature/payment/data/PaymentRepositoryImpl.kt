package com.innaval.ingresosnamao.feature.payment.data

import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.common.result.AppResult
import com.innaval.ingresosnamao.core.common.result.map
import com.innaval.ingresosnamao.core.common.result.runCatchingApp
import com.innaval.ingresosnamao.feature.payment.domain.PaymentReceipt
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
internal data class PayRequest(val amountCents: Long)

@Serializable
internal data class ReceiptDto(val orderId: String, val paidCents: Long)

internal interface PaymentApi {
    @POST("payments")
    suspend fun pay(@Body request: PayRequest): ReceiptDto
}

internal class PaymentRepositoryImpl(
    private val api: PaymentApi,
    private val dispatchers: DispatcherProvider,
) : PaymentRepository {
    override suspend fun pay(amountCents: Long): AppResult<PaymentReceipt> =
        withContext(dispatchers.io) {
            runCatchingApp { api.pay(PayRequest(amountCents)) }
                .map { PaymentReceipt(it.orderId, it.paidCents) }
        }
}
