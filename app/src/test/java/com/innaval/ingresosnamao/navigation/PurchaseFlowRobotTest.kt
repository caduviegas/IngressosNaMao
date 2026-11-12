package com.innaval.ingresosnamao.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.BuildConfig
import com.innaval.ingresosnamao.di.appModules
import com.innaval.ingresosnamao.feature.auth.domain.*
import com.innaval.ingresosnamao.feature.cart.domain.*
import com.innaval.ingresosnamao.feature.events.domain.*
import com.innaval.ingresosnamao.feature.payment.domain.*
import com.innaval.ingresosnamao.feature.ticket.domain.*
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PurchaseFlowRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenApprovedPaymentWhenContinuingThenIssuesTicketBeforeClearingCart() {
        val auth = mockk<AuthRepository>()
        val events = mockk<EventRepository>()
        val cart = mockk<CartRepository>()
        val payments = mockk<PaymentRepository>()
        val tickets = mockk<TicketRepository>()
        val qr = mockk<QrCodeGenerator>()
        val user = AuthUser("alice", "alice@example.com", "Alice")
        val item = CartItem("event", "Show", 100, 1)
        val snapshot = CartSnapshot(listOf(item), 100)
        val cartState = MutableStateFlow<CartResult<CartSnapshot>>(CartResult.Success(CartSnapshot(emptyList(), 0)))
        val paymentState = MutableStateFlow<PaymentState>(PaymentState.Ready)
        val ticket = Ticket("ticket-id", "reference", "event", "Show", 100)
        val receipt = Receipt(PurchaseSummary("reference", "order", 100, 1), listOf(ticket))

        coEvery { auth.currentUser() } returns AuthResult.Success(user)
        coEvery { auth.getProfile(user) } returns AuthResult.Success(UserProfile("Alice", "alice@example.com", "Rua"))
        coEvery { events.getEvents() } returns EventsResult.Success(
            EventList(listOf(Event("event", "file:///missing-image", "Show", 100, 10)), false)
        )
        coEvery { events.getFreshEvents() } returns EventsResult.Success(
            EventList(listOf(Event("event", "file:///missing-image", "Show", 100, 10)), false)
        )
        every { cart.observe("alice") } returns cartState
        coEvery { cart.add("alice", item) } coAnswers {
            cartState.value = CartResult.Success(snapshot)
            CartResult.Success(Unit)
        }
        coEvery { cart.get("alice") } returns CartResult.Success(snapshot)
        coEvery { cart.clear("alice", any()) } returns CartResult.Success(Unit)
        every { payments.state } returns paymentState
        every { payments.expire() } returns Unit
        every { payments.start(any(), any()) } answers {
            val order = firstArg<PaymentOrder>()
            paymentState.value = PaymentState.Active(
                PaymentAttempt("reference", order, 1, AttemptStatus.APPROVED, "order")
            )
            StartResult.Started
        }
        every { payments.acknowledgeApproved("alice", any()) } answers {
            paymentState.value = PaymentState.Ready
            true
        }
        coEvery { tickets.issue(any()) } returns TicketResult.Success(receipt)
        every { tickets.observeReceipt("alice", "reference") } returns flowOf(TicketResult.Success(receipt))
        every { tickets.observeTicket("alice", "ticket-id") } returns flowOf(TicketResult.Success(ticket))
        coEvery { qr.generate("ticket-id") } returns QrResult.Failure

        val overrides = module {
            single<AuthRepository> { auth }
            single<EventRepository> { events }
            single<CartRepository> { cart }
            single<PaymentRepository> { payments }
            single<TicketRepository> { tickets }
            single<QrCodeGenerator> { qr }
        }
        val application = ApplicationProvider.getApplicationContext<Application>()
        compose.setContent {
            KoinApplication(application = {
                androidContext(application)
                allowOverride(true)
                modules(appModules() + overrides)
            }) {
                IngressosTheme { AppNavHost() }
            }
        }
        val robot = PurchaseFlowRobot(compose)
        robot.action.selectEvent()
        robot.action.checkout()
        robot.result.navigationHidden()
        robot.action.continueApproved()
        robot.result.receiptShown()
        robot.action.openTicket()
        robot.result.ticketShown()
        verify(exactly = 1) {
            payments.start(any(), TestCredentials(BuildConfig.CIELO_CLIENT_ID, BuildConfig.CIELO_ACCESS_TOKEN))
        }
        coVerifyOrder {
            tickets.issue(any())
            cart.clear("alice", any())
        }
    }
}
