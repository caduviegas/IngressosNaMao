package com.innaval.ingresosnamao.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.di.appModules
import com.innaval.ingresosnamao.feature.auth.domain.AuthRepository
import com.innaval.ingresosnamao.feature.auth.domain.AuthResult
import com.innaval.ingresosnamao.feature.auth.domain.AuthUser
import com.innaval.ingresosnamao.feature.auth.domain.UserProfile
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.CartRepository
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.cart.domain.CartSnapshot
import com.innaval.ingresosnamao.feature.events.domain.EventList
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import com.innaval.ingresosnamao.feature.payment.domain.PaymentState
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileRepository
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile as ProfileData
import com.innaval.ingresosnamao.feature.ticket.domain.PurchaseSummary
import com.innaval.ingresosnamao.feature.ticket.domain.TicketRepository
import com.innaval.ingresosnamao.feature.ticket.domain.TicketResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
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
class MainNavigationRobotTest {
    @get:Rule val compose = createComposeRule()
    private val auth = mockk<AuthRepository>()
    private val events = mockk<EventRepository>()
    private val cart = mockk<CartRepository>()
    private val payments = mockk<PaymentRepository>()
    private val profiles = mockk<ProfileRepository>()
    private val tickets = mockk<TicketRepository>()
    private val snapshot = CartSnapshot(listOf(CartItem("event", "Show", 100, 1)), 100)
    private lateinit var nav: NavHostController
    private var signedIn = true

    private fun start(catalog: List<Event> = emptyList()) {
        val user = AuthUser("alice", "alice@example.com", "Alice")
        val identity = ProfileIdentity("alice", "Alice", user.email)
        coEvery { auth.currentUser() } answers { AuthResult.Success(if (signedIn) user else null) }
        coEvery { auth.getProfile(user) } returns AuthResult.Success(UserProfile("Alice", user.email, "Rua"))
        coEvery { events.getEvents() } returns EventsResult.Success(EventList(catalog, false))
        every { payments.state } returns MutableStateFlow(PaymentState.Ready)
        every { cart.observe("alice") } returns flowOf(CartResult.Success(snapshot))
        coEvery { profiles.currentUser() } returns ProfileResult.Success(identity)
        coEvery { profiles.load(identity) } returns
            ProfileResult.Success(ProfileData("Alice", user.email, "Rua", false))
        val purchase = PurchaseSummary("reference", "order", 100, 1)
        every { tickets.observePurchases("alice") } returns flowOf(TicketResult.Success(listOf(purchase)))
        every { tickets.observeReceipt("alice", "reference") } returns flowOf(TicketResult.Success(null))
        val overrides = module {
            single<AuthRepository> { auth }
            single<EventRepository> { events }
            single<CartRepository> { cart }
            single<PaymentRepository> { payments }
            single<ProfileRepository> { profiles }
            single<TicketRepository> { tickets }
        }
        val application = ApplicationProvider.getApplicationContext<Application>()
        compose.setContent {
            KoinApplication(application = {
                androidContext(application)
                allowOverride(true)
                modules(appModules() + overrides)
            }) {
                IngressosTheme {
                    nav = rememberNavController()
                    AppNavHost(nav)
                }
            }
        }
    }

    @Test fun givenAuthenticatedUserWhenSwitchingTabsThenShowsTheRequestedScreen() {
        start()
        val robot = MainNavigationRobot(compose)
        robot.result.selected("Home")
        robot.action.select("Meus pedidos")
        robot.result.selected("Meus pedidos")
        robot.action.openOrder()
        robot.result.shown("Resumo do pedido")
        robot.result.selected("Meus pedidos")
        robot.action.back()
        robot.result.selected("Meus pedidos")
        robot.action.select("Carrinho")
        robot.result.selected("Carrinho")
        robot.result.shown("Finalizar compra")
        robot.action.select("Meu perfil")
        robot.result.selected("Meu perfil")
        robot.result.shown("Alice")
        robot.action.select("Home")
        robot.result.selected("Home")
        compose.runOnIdle {
            assertEquals("events/{userId}", nav.currentDestination?.route)
            assertEquals(null, nav.previousBackStackEntry)
        }
    }

    @Test fun givenCheckoutInProgressWhenChangingTabsThenNavigationIsDisabled() {
        val pending = CompletableDeferred<CartResult<CartSnapshot>>()
        coEvery { cart.get("alice") } coAnswers { pending.await() }
        start()
        val robot = MainNavigationRobot(compose)
        robot.action.select("Carrinho")
        robot.action.checkout()
        robot.result.disabled("Home")
        robot.result.disabled("Meu perfil")
        robot.result.backDisabled()
        compose.runOnIdle { pending.complete(CartResult.Failure(CartError.STORAGE)) }
        robot.result.shown("Não foi possível acessar o carrinho. Tente novamente.")
        robot.action.select("Home")
        robot.result.selected("Home")
    }

    @Test fun givenAddingEventWhenChangingTabsThenNavigationWaitsForCart() {
        val pending = CompletableDeferred<CartResult<Unit>>()
        coEvery { cart.add("alice", any()) } coAnswers { pending.await() }
        start(listOf(Event("event", "file:///missing-image", "Show", 100, 5)))
        val robot = MainNavigationRobot(compose)
        robot.action.selectEvent()
        robot.result.disabled("Meus pedidos")
        robot.result.disabled("Meu perfil")
        compose.runOnIdle { pending.complete(CartResult.Success(Unit)) }
        robot.result.selected("Carrinho")
    }

    @Test fun givenPaymentCartErrorWhenPressingBackThenReturnsToCart() {
        coEvery { cart.get("alice") } returnsMany listOf(
            CartResult.Success(snapshot),
            CartResult.Failure(CartError.STORAGE),
        )
        coEvery { events.getFreshEvents() } returns EventsResult.Success(
            EventList(listOf(Event("event", "file:///missing-image", "Show", 100, 5)), false)
        )
        start()
        val robot = MainNavigationRobot(compose)
        robot.action.select("Carrinho")
        robot.action.checkout()
        robot.result.shown("Pagamento")
        robot.result.shown("Não foi possível acessar o carrinho. Tente novamente.")
        robot.result.hidden("Voltar")
        robot.action.back()
        robot.result.selected("Carrinho")
    }

    @Test fun givenNoSessionWhenLoginIsDisplayedThenNavigationIsHidden() {
        signedIn = false
        start()
        val robot = MainNavigationRobot(compose)
        robot.result.shown("Entrar com Google")
        robot.result.hidden("Home")
        robot.result.hidden("Meus pedidos")
    }

    @Test fun givenLogoutInProgressWhenChangingTabsThenNavigationIsDisabledUntilLogin() {
        val pending = CompletableDeferred<ProfileResult<Unit>>()
        coEvery { cart.clearForLogout("alice") } returns CartResult.Success(Unit)
        coEvery { profiles.signOut("alice") } coAnswers {
            val result = pending.await()
            signedIn = false
            result
        }
        start()
        val robot = MainNavigationRobot(compose)
        robot.action.select("Meu perfil")
        robot.action.requestLogout()
        robot.action.confirmLogout()
        robot.result.disabled("Home")
        robot.result.disabled("Carrinho")
        robot.result.backDisabled()
        compose.runOnIdle { pending.complete(ProfileResult.Success(Unit)) }
        robot.result.shown("Entrar com Google")
        robot.result.hidden("Home")
    }
}
