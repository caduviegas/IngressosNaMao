package com.innaval.ingresosnamao.feature.ticket.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.ticket.domain.*
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TicketRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenReceiptErrorWhenRetriedThenAllowsOpeningTicket() {
        val ticket = Ticket("ticket-id", "reference", "event", "Show", 100)
        val receipt = Receipt(PurchaseSummary("reference", "order", 100, 1), listOf(ticket))
        val repository = mockk<TicketRepository>()
        every { repository.observeReceipt("alice", "reference") } returnsMany listOf(
            flowOf<TicketResult<Receipt?>>(TicketResult.Failure(TicketError.STORAGE)),
            flowOf<TicketResult<Receipt?>>(TicketResult.Success(receipt)),
        )
        val viewModel = ReceiptViewModel("alice", "reference", ObserveReceiptUseCase(repository))
        var opened: String? = null
        compose.setContent {
            IngressosTheme { ReceiptRoute("alice", "reference", { opened = it }, {}, viewModel) }
        }
        val robot = TicketRobot(compose)
        robot.result.receiptErrorShown()
        robot.action.retry()
        robot.result.receiptShown()
        robot.action.openTicket()
        compose.runOnIdle { assertEquals("ticket-id", opened) }
    }

    @Test fun givenOwnedTicketAndQrFailureWhenRetriedThenShowsQr() {
        val ticket = Ticket("ticket-id", "reference", "event", "Show", 100)
        val repository = mockk<TicketRepository>()
        val generator = mockk<QrCodeGenerator>()
        every { repository.observeTicket("alice", "ticket-id") } returns flowOf(TicketResult.Success(ticket))
        coEvery { generator.generate("ticket-id") } returnsMany listOf(
            QrResult.Failure,
            QrResult.Success(QrPattern(1, intArrayOf(android.graphics.Color.BLACK))),
        )
        val viewModel = TicketDetailViewModel("alice", "ticket-id", ObserveTicketUseCase(repository), generator)
        compose.setContent {
            IngressosTheme { TicketDetailRoute("alice", "ticket-id", {}, viewModel) }
        }
        val robot = TicketRobot(compose)
        robot.result.invalidTicketAndQrErrorShown()
        robot.action.retry()
        robot.result.qrShown()
    }
}
