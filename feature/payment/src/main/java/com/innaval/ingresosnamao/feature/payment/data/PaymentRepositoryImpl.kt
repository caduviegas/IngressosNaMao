package com.innaval.ingresosnamao.feature.payment.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import com.innaval.ingresosnamao.feature.payment.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import java.util.UUID

internal class PaymentRepositoryImpl(
    context: Context,
    private val gateway: PaymentGateway,
) : PaymentRepository {
    private val preferences = context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE)
    private val enabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableState = MutableStateFlow<PaymentState>(load())
    override val state: StateFlow<PaymentState> = mutableState.asStateFlow()

    private fun load(): PaymentState {
        return try {
            val saved = preferences.getString("attempt", null) ?: return PaymentState.Ready
            val attempt = json.decodeFromString<PaymentAttempt>(saved)
            if (!validOrder(attempt.order) || attempt.reference.isBlank()) {
                PaymentState.StorageError("Tentativa armazenada inválida; conciliação necessária.")
            } else PaymentState.Active(attempt)
        } catch (_: SerializationException) {
            PaymentState.StorageError("Não foi possível restaurar a tentativa; conciliação necessária.")
        } catch (_: IllegalArgumentException) {
            PaymentState.StorageError("Não foi possível restaurar a tentativa; conciliação necessária.")
        } catch (_: ClassCastException) {
            PaymentState.StorageError("Não foi possível restaurar a tentativa; conciliação necessária.")
        }
    }

    private fun save(attempt: PaymentAttempt): Boolean {
        if (!preferences.edit().putString("attempt", json.encodeToString(attempt)).commit()) {
            mutableState.value = PaymentState.StorageError("Falha ao salvar tentativa; não realize outro pagamento.")
            return false
        }
        mutableState.value = PaymentState.Active(attempt)
        return true
    }

    @Synchronized
    override fun start(order: PaymentOrder, credentials: TestCredentials): StartResult {
        if (!enabled) return StartResult.Failure("Pagamento Cielo disponível somente em debug.")
        if (state.value != PaymentState.Ready) return StartResult.Failure("Existe uma tentativa anterior que exige conciliação.")
        if (!validOrder(order)) return StartResult.Failure("Carrinho ou total inválido.")
        if (credentials.clientId.isBlank() || credentials.accessToken.isBlank()) {
            return StartResult.Failure("Informe as credenciais de teste da Cielo.")
        }
        val attempt = PaymentAttempt(UUID.randomUUID().toString(), order, System.currentTimeMillis(), AttemptStatus.WAITING)
        if (!save(attempt)) return StartResult.Failure("Falha ao salvar a tentativa; pagamento não iniciado.")
        val result = gateway.launch(attempt, credentials)
        if (result is StartResult.Failure) save(attempt.copy(status = AttemptStatus.UNKNOWN))
        return result
    }

    @Synchronized
    override fun acceptCallback(encodedResponse: String?) {
        if (!enabled) return
        val attempt = (state.value as? PaymentState.Active)?.attempt ?: return
        if (attempt.status == AttemptStatus.APPROVED) return
        val response = try {
            val bytes = Base64.decode(encodedResponse ?: "", Base64.DEFAULT)
            json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        } catch (_: IllegalArgumentException) {
            markUnknown(attempt)
            return
        } catch (_: SerializationException) {
            markUnknown(attempt)
            return
        }
        try {
            val code = response["code"]?.jsonPrimitive?.intOrNull
            if (code != null) {
                save(attempt.copy(status = if (code == 1) AttemptStatus.CANCELLED else AttemptStatus.ERROR))
                return
            }
            val reference = response["reference"]?.jsonPrimitive?.contentOrNull
            val orderId = response["id"]?.jsonPrimitive?.contentOrNull
            val paid = response["paidAmount"]?.jsonPrimitive?.longOrNull
            val pending = response["pendingAmount"]?.jsonPrimitive?.longOrNull
            val payments = response["payments"]?.jsonArray
            val authorized = payments?.isNotEmpty() == true && payments.all {
                val payment = it.jsonObject
                val status = payment["paymentFields"]?.jsonObject?.get("statusCode")?.jsonPrimitive?.contentOrNull
                val amount = payment["amount"]?.jsonPrimitive?.longOrNull
                status in setOf("0", "1") && amount != null && amount > 0
            }
            val paymentTotal = payments?.fold(0L) { total, payment ->
                Math.addExact(total, payment.jsonObject["amount"]?.jsonPrimitive?.longOrNull ?: 0L)
            }
            if (reference == attempt.reference && !orderId.isNullOrBlank() &&
                paid == attempt.order.totalCents && pending == 0L && authorized && paymentTotal == paid
            ) {
                save(attempt.copy(status = AttemptStatus.APPROVED, orderId = orderId))
            } else markUnknown(attempt)
        } catch (_: IllegalArgumentException) {
            markUnknown(attempt)
        } catch (_: ArithmeticException) {
            markUnknown(attempt)
        }
    }

    private fun markUnknown(attempt: PaymentAttempt) {
        save(attempt.copy(status = AttemptStatus.UNKNOWN))
    }

    @Synchronized
    override fun expire() {
        val attempt = (state.value as? PaymentState.Active)?.attempt ?: return
        if (attempt.status == AttemptStatus.WAITING && System.currentTimeMillis() - attempt.startedAtMillis >= 120_000L) {
            markUnknown(attempt)
        }
    }

    @Synchronized
    override fun acknowledgeApproved(userId: String, fingerprint: String): Boolean {
        val attempt = (state.value as? PaymentState.Active)?.attempt ?: return false
        if (attempt.status != AttemptStatus.APPROVED || attempt.order.userId != userId ||
            attempt.order.fingerprint != fingerprint
        ) return false
        if (!preferences.edit().remove("attempt").commit()) {
            mutableState.value = PaymentState.StorageError("Falha ao concluir a tentativa de teste.")
            return false
        }
        mutableState.value = PaymentState.Ready
        return true
    }
}
