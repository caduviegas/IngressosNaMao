package com.innaval.ingresosnamao.feature.payment.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.innaval.ingresosnamao.feature.payment.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PaymentRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gateway = mockk<PaymentGateway>()
    private val order = PaymentOrder("user", "fingerprint", listOf(PaymentItem("sku", "Evento", 2, 500)), 1000)
    private val credentials = TestCredentials("client", "test-token")

    @Before fun setup() {
        context.applicationInfo.flags = context.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).edit().clear().commit()
        every { gateway.launch(any(), any()) } returns StartResult.Started
    }

    @Test fun givenNewPaymentWhenStartedThenPersistsAndBlocksOtherAttempts() {
        val repo = PaymentRepositoryImpl(context, gateway)
        every { gateway.launch(any(), any()) } answers {
            assertNotNull(context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).getString("attempt", null))
            StartResult.Started
        }
        assertEquals(StartResult.Started, repo.start(order, credentials))
        val restarted = PaymentRepositoryImpl(context, gateway)
        assertTrue(restarted.start(order.copy(userId = "other"), credentials) is StartResult.Failure)
        verify(exactly = 1) { gateway.launch(any(), any()) }
        assertFalse(context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).all.toString().contains("test-token"))
    }

    @Test fun givenMissingCredentialsWhenStartingPaymentThenReturnsError() {
        val repo = PaymentRepositoryImpl(context, gateway)
        assertEquals(
            StartResult.Failure("Credenciais da Cielo não configuradas."),
            repo.start(order, TestCredentials("", "")),
        )
        assertEquals(PaymentState.Ready, repo.state.value)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    @Test fun givenPaymentCallbackWhenCompleteAndMatchingThenApproves() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        val reference = (repo.state.value as PaymentState.Active).attempt.reference
        fun response(ref: String, paid: Long) = encoded(
            """{"reference":"$ref","id":"order","paidAmount":$paid,"pendingAmount":0,"payments":[{"amount":1000,"paymentFields":{"statusCode":"1"}}]}"""
        )
        repo.acceptCallback(response("wrong", 1000))
        assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        repo.acceptCallback(response(reference, 999))
        assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        repo.acceptCallback(response(reference, 1000))
        assertEquals(AttemptStatus.APPROVED, (repo.state.value as PaymentState.Active).attempt.status)
        assertFalse(repo.acknowledgeApproved("other", "fingerprint"))
        assertTrue(repo.acknowledgeApproved("user", "fingerprint"))
        assertEquals(PaymentState.Ready, repo.state.value)
    }

    @Test fun givenZeroPaidAmountWhenTransactionsMatchThenApproves() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        val reference = (repo.state.value as PaymentState.Active).attempt.reference
        fun response(ref: String, paid: Long, amount: Long, pending: Long, status: String) = encoded(
            """{"reference":"$ref","id":"order","paidAmount":$paid,"pendingAmount":$pending,""" +
                """"payments":[{"amount":$amount,"paymentFields":{"statusCode":"$status"}}]}"""
        )
        for (invalid in listOf(
            response("wrong", 0, 1000, 0, "1"),
            response(reference, 0, 999, 0, "1"),
            response(reference, 0, 1000, 1, "1"),
            response(reference, 0, 1000, 0, "2"),
            response(reference, 999, 1000, 0, "1"),
        )) {
            repo.acceptCallback(invalid)
            assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        }
        repo.acceptCallback(response(reference, 0, 1000, 0, "1"))
        assertEquals(AttemptStatus.APPROVED, (repo.state.value as PaymentState.Active).attempt.status)
        assertEquals("order", (repo.state.value as PaymentState.Active).attempt.orderId)
    }

    @Test fun givenCancelledOrMalformedCallbackWhenReceivedThenRemainsBlocked() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        repo.acceptCallback(encoded("""{"code":1,"reason":"cancelado"}"""))
        assertEquals(AttemptStatus.CANCELLED, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        repo.acceptCallback("not-base64")
        assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
    }

    @Test fun givenInvalidTotalOrMissingCredentialsWhenStartingThenRejectsPayment() {
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.start(order.copy(totalCents = 1), credentials) is StartResult.Failure)
        assertTrue(repo.start(order, credentials.copy(accessToken = "")) is StartResult.Failure)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    @Test fun givenLaunchFailureWhenStartingPaymentThenRemainsBlocked() {
        every { gateway.launch(any(), any()) } returns StartResult.Failure("indisponível")
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
    }

    @Test fun givenTimedOutAttemptWhenRestartedThenRemainsBlocked() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        val attempt = (repo.state.value as PaymentState.Active).attempt.copy(startedAtMillis = 0)
        context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).edit()
            .putString("attempt", Json.encodeToString(attempt)).commit()
        val restarted = PaymentRepositoryImpl(context, gateway)
        restarted.expire()
        assertEquals(AttemptStatus.UNKNOWN, (restarted.state.value as PaymentState.Active).attempt.status)
        assertTrue(restarted.start(order, credentials) is StartResult.Failure)
    }

    @Test fun givenOrderWhenBuildingRequestThenIncludesItemsAndCallback() {
        val attempt = PaymentAttempt("ref", order, 0, AttemptStatus.WAITING)
        val uri = paymentUri(attempt, credentials)
        assertEquals("lio", uri.scheme)
        assertEquals("payment", uri.host)
        assertEquals("ingressosnamao-payment://response", uri.getQueryParameter("urlCallback"))
        val request = Json.parseToJsonElement(String(Base64.decode(uri.getQueryParameter("request"), Base64.DEFAULT))).jsonObject
        assertEquals("1000", request.getValue("value").jsonPrimitive.content)
        assertEquals("ref", request.getValue("reference").jsonPrimitive.content)
        val item = request.getValue("items").jsonArray.single().jsonObject
        assertEquals("sku", item.getValue("sku").jsonPrimitive.content)
        assertEquals("2", item.getValue("quantity").jsonPrimitive.content)
        assertEquals("500", item.getValue("unitPrice").jsonPrimitive.content)
    }

    @Test fun givenReleaseBuildWhenStartingPaymentThenRejectsRequest() {
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    @Test fun givenPaymentErrorWhenCallbackReceivedThenDoesNotPermitRetry() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        repo.acceptCallback(encoded("""{"code":3,"reason":"erro no pagamento"}"""))
        assertEquals(AttemptStatus.ERROR, (repo.state.value as PaymentState.Active).attempt.status)
        assertEquals(3, (PaymentRepositoryImpl(context, gateway).state.value as PaymentState.Active).attempt.errorCode)
        val saved = context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).all.toString()
        assertFalse(saved.contains("erro no pagamento"))
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
    }

    @Test fun givenAttemptWhenDiscardingThenRequiresUnchangedState() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        val oldState = repo.state.value
        repo.acceptCallback(encoded("""{"code":4,"reason":"sensitive details"}"""))
        assertTrue(repo.discardSimulation(oldState) is DiscardSimulationResult.Failure)
        assertTrue(repo.discardSimulation(PaymentState.Ready) is DiscardSimulationResult.Failure)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        assertEquals(DiscardSimulationResult.Discarded, repo.discardSimulation(repo.state.value))
        assertEquals(PaymentState.Ready, PaymentRepositoryImpl(context, gateway).state.value)
        assertEquals(StartResult.Started, repo.start(order, credentials))
    }

    @Test fun givenAttemptStatesWhenRestoredThenCanDiscardEach() {
        for (status in AttemptStatus.entries) {
            val attempt = PaymentAttempt("ref", order, 1, status, orderId = "order")
            context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).edit()
                .putString("attempt", Json.encodeToString(attempt)).commit()
            val repo = PaymentRepositoryImpl(context, gateway)
            assertEquals(DiscardSimulationResult.Discarded, repo.discardSimulation(repo.state.value))
            assertEquals(PaymentState.Ready, PaymentRepositoryImpl(context, gateway).state.value)
        }
    }

    @Test fun givenCancelledAttemptWhenDiscardedThenReleaseStillRejectsDiscard() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        repo.acceptCallback(encoded("""{"code":1}"""))
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val releaseRepo = PaymentRepositoryImpl(context, gateway)
        assertTrue(releaseRepo.discardSimulation(releaseRepo.state.value) is DiscardSimulationResult.Failure)
        assertEquals(DiscardSimulationResult.Discarded, repo.discardSimulation(repo.state.value))
    }

    @Test fun givenLegacyAttemptWithoutErrorCodeWhenRestoredThenLoadsAttempt() {
        val saved = """{"reference":"ref","order":{"userId":"user","fingerprint":"fingerprint","items":[""" +
            """{"sku":"sku","name":"Evento","quantity":2,"unitPriceCents":500}],"totalCents":1000},""" +
            """"startedAtMillis":0,"status":"ERROR"}"""
        context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).edit()
            .putString("attempt", saved).commit()
        val attempt = (PaymentRepositoryImpl(context, gateway).state.value as PaymentState.Active).attempt
        assertEquals(AttemptStatus.ERROR, attempt.status)
        assertNull(attempt.errorCode)
        assertEquals(
            DiscardSimulationResult.Discarded,
            PaymentRepositoryImpl(context, gateway).let { it.discardSimulation(it.state.value) },
        )
    }

    @Test fun givenDiscardStorageFailureWhenDiscardingThenKeepsPaymentBlocked() {
        val attempt = PaymentAttempt("ref", order, 0, AttemptStatus.ERROR)
        val mockContext = mockk<Context>()
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        val applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_DEBUGGABLE }
        every { mockContext.applicationInfo } returns applicationInfo
        every { mockContext.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString("attempt", null) } returns Json.encodeToString(attempt)
        every { preferences.edit() } returns editor
        every { editor.remove("attempt") } returns editor
        every { editor.commit() } returns false
        val repo = PaymentRepositoryImpl(mockContext, gateway)

        assertTrue(repo.discardSimulation(repo.state.value) is DiscardSimulationResult.Failure)
        assertTrue(repo.state.value is PaymentState.StorageError)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    @Test fun givenCorruptedStoredAttemptWhenDiscardedThenClearsError() {
        context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).edit()
            .putString("attempt", "invalid").commit()
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.state.value is PaymentState.StorageError)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        assertEquals(DiscardSimulationResult.Discarded, repo.discardSimulation(repo.state.value))
        assertEquals(PaymentState.Ready, PaymentRepositoryImpl(context, gateway).state.value)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    private fun encoded(value: String) = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
}
