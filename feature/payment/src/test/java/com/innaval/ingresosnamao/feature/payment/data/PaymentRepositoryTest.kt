package com.innaval.ingresosnamao.feature.payment.data

import android.content.Context
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

    @Test fun persistsBeforeLaunchAndBlocksAcrossRestartAndUsers() {
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

    @Test fun onlyMatchingCompletePaymentApproves() {
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

    @Test fun cancellationAndMalformedReturnStayBlocked() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        repo.acceptCallback(encoded("""{"code":1,"reason":"cancelado"}"""))
        assertEquals(AttemptStatus.CANCELLED, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        repo.acceptCallback("not-base64")
        assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
    }

    @Test fun rejectsInvalidTotalAndMissingCredentials() {
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.start(order.copy(totalCents = 1), credentials) is StartResult.Failure)
        assertTrue(repo.start(order, credentials.copy(accessToken = "")) is StartResult.Failure)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    @Test fun launchFailureRemainsBlocked() {
        every { gateway.launch(any(), any()) } returns StartResult.Failure("indisponível")
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        assertEquals(AttemptStatus.UNKNOWN, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
    }

    @Test fun timedOutAttemptRemainsBlockedAfterRestart() {
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

    @Test fun requestContainsAllItemsAndEncodedCallback() {
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

    @Test fun releaseBuildCannotStartPayment() {
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    @Test fun paymentErrorDoesNotMeanDeniedOrPermitRetry() {
        val repo = PaymentRepositoryImpl(context, gateway)
        repo.start(order, credentials)
        repo.acceptCallback(encoded("""{"code":3,"reason":"erro no pagamento"}"""))
        assertEquals(AttemptStatus.ERROR, (repo.state.value as PaymentState.Active).attempt.status)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
    }

    @Test fun corruptedStoredAttemptFailsClosed() {
        context.getSharedPreferences("payment_attempt", Context.MODE_PRIVATE).edit()
            .putString("attempt", "invalid").commit()
        val repo = PaymentRepositoryImpl(context, gateway)
        assertTrue(repo.state.value is PaymentState.StorageError)
        assertTrue(repo.start(order, credentials) is StartResult.Failure)
        verify(exactly = 0) { gateway.launch(any(), any()) }
    }

    private fun encoded(value: String) = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
}
