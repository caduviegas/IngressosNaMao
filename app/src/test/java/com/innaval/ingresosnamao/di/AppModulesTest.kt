package com.innaval.ingresosnamao.di

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.database.AppDatabase
import com.innaval.ingresosnamao.core.database.cart.CartDao
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketDao
import com.innaval.ingresosnamao.core.database.ticket.TicketDao
import com.innaval.ingresosnamao.core.network.di.NetworkConfig
import com.innaval.ingresosnamao.feature.auth.domain.*
import com.innaval.ingresosnamao.feature.auth.presentation.AuthViewModel
import com.innaval.ingresosnamao.feature.cart.domain.*
import com.innaval.ingresosnamao.feature.cart.presentation.CartCleanupState
import com.innaval.ingresosnamao.feature.cart.presentation.CartCleanupViewModel
import com.innaval.ingresosnamao.feature.cart.presentation.CartViewModel
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import com.innaval.ingresosnamao.feature.events.presentation.EventsViewModel
import com.innaval.ingresosnamao.feature.payment.domain.*
import com.innaval.ingresosnamao.feature.payment.presentation.PaymentViewModel
import com.innaval.ingresosnamao.feature.profile.domain.ProfileCartCleaner
import com.innaval.ingresosnamao.feature.profile.domain.ProfileRepository
import com.innaval.ingresosnamao.feature.profile.domain.LogoutUseCase
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.ticket.domain.*
import com.innaval.ingresosnamao.feature.ticket.presentation.ReceiptViewModel
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketDetailViewModel
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class AppModulesTest {
    private val application: Application get() = ApplicationProvider.getApplicationContext()
    private lateinit var scheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun inMemoryDatabase() = module {
        single<AppDatabase> {
            Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).build()
        } onClose { it?.close() }
    }

    @Test
    fun givenProductionModulesWhenComposedThenRejectsDuplicateDefinitions() {
        val context = koinApplication {
            androidContext(application)
            allowOverride(false)
            modules(appModules())
        }
        try {
            assertNotNull(context.koin.get<DispatcherProvider>())
            assertNotNull(context.koin.get<NetworkConfig>())
        } finally {
            context.close()
        }
    }

    @Test
    fun givenProductionGraphWhenResolvedThenProvidesEntriesAndPreservesLifetimes() {
        val context = testKoin(application, inMemoryDatabase())
        try {
            val koin = context.koin
            assertSame(koin.get<DispatcherProvider>(), koin.get<DispatcherProvider>())
            assertSame(koin.get<AppDatabase>(), koin.get<AppDatabase>())
            assertSame(koin.get<CartDao>(), koin.get<CartDao>())
            assertSame(koin.get<TicketDao>(), koin.get<TicketDao>())
            assertSame(koin.get<IssuedTicketDao>(), koin.get<IssuedTicketDao>())
            assertSame(koin.get<Retrofit>(), koin.get<Retrofit>())
            assertSame(koin.get<AuthRepository>(), koin.get<AuthRepository>())
            assertSame(koin.get<EventRepository>(), koin.get<EventRepository>())
            assertSame(koin.get<CartRepository>(), koin.get<CartRepository>())
            assertSame(koin.get<PaymentGateway>(), koin.get<PaymentGateway>())
            assertSame(koin.get<PaymentRepository>(), koin.get<PaymentRepository>())
            assertSame(koin.get<ProfileRepository>(), koin.get<ProfileRepository>())
            assertSame(koin.get<ProfileCartCleaner>(), koin.get<ProfileCartCleaner>())
            assertSame(koin.get<TicketRepository>(), koin.get<TicketRepository>())
            assertSame(koin.get<QrCodeGenerator>(), koin.get<QrCodeGenerator>())
            assertNotSame(koin.get<GetEventsUseCase>(), koin.get<GetEventsUseCase>())

            koin.get<LoginUseCase>()
            koin.get<GoogleLoginUseCase>()
            koin.get<RegisterUseCase>()
            koin.get<GetCurrentUserUseCase>()
            koin.get<GetProfileUseCase>()
            koin.get<SaveProfileUseCase>()
            koin.get<SignOutUseCase>()
            koin.get<ObserveCartUseCase>()
            koin.get<GetCartUseCase>()
            koin.get<AddToCartUseCase>()
            koin.get<SetCartQuantityUseCase>()
            koin.get<RemoveFromCartUseCase>()
            koin.get<ClearCartUseCase>()
            koin.get<ClearCartForLogoutUseCase>()
            koin.get<LogoutUseCase>()
            koin.get<StartPaymentUseCase>()
            koin.get<IssueTicketsUseCase>()
            koin.get<ObservePurchasesUseCase>()
            koin.get<ObserveReceiptUseCase>()
            koin.get<ObserveTicketUseCase>()

            koin.get<AuthViewModel>()
            koin.get<EventsViewModel>()
            koin.get<CartViewModel> { parametersOf("user") }
            koin.get<CartCleanupViewModel> { parametersOf("user", "fingerprint") }
            koin.get<PaymentViewModel>()
            koin.get<TicketsViewModel> { parametersOf("user") }
            koin.get<ReceiptViewModel> { parametersOf("user", "reference") }
            koin.get<TicketDetailViewModel> { parametersOf("user", "ticket") }
        } finally {
            context.close()
        }
    }

    @Test
    fun givenIndependentGraphsWhenClosedThenDoNotShareOrCloseOtherSingletons() {
        val first = testKoin(application, inMemoryDatabase())
        val second = testKoin(application, inMemoryDatabase())
        var firstClosed = false
        try {
            val firstDispatchers = first.koin.get<DispatcherProvider>()
            val secondDispatchers = second.koin.get<DispatcherProvider>()
            val secondDatabase = second.koin.get<AppDatabase>()
            assertNotSame(firstDispatchers, secondDispatchers)
            assertNotSame(first.koin.get<AppDatabase>(), secondDatabase)
            assertNotSame(first.koin.get<PaymentRepository>(), second.koin.get<PaymentRepository>())
            first.close()
            firstClosed = true
            assertSame(secondDispatchers, second.koin.get<DispatcherProvider>())
            assertSame(secondDatabase, second.koin.get<AppDatabase>())
        } finally {
            if (!firstClosed) first.close()
            second.close()
        }
    }

    @Test
    fun givenOverridesWhenResolvingGraphThenReachesUseCasesAndViewModels() {
        val cart = mockk<CartRepository>()
        val tickets = mockk<TicketRepository>()
        val gateway = mockk<PaymentGateway>()
        val payment = mockk<PaymentRepository>()
        val fingerprint = "fingerprint"
        every { cart.observe("alice") } returns flowOf(CartResult.Success(CartSnapshot(emptyList(), 0)))
        coEvery { cart.clear("alice", fingerprint) } returns CartResult.Success(Unit)
        every { tickets.observePurchases("alice") } returns flowOf(TicketResult.Success(emptyList()))
        every { tickets.observeReceipt("alice", "purchase") } returns flowOf(TicketResult.Success(null))
        every { tickets.observeTicket("alice", "ticket") } returns flowOf(TicketResult.Success(null))
        every { payment.state } returns MutableStateFlow(PaymentState.Ready)
        every { payment.expire() } returns Unit
        val order = PaymentOrder("alice", fingerprint, listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val credentials = TestCredentials("test", "test")
        every { payment.start(order, credentials) } returns StartResult.Started

        val overrides = module {
            single<CartRepository> { cart }
            single<TicketRepository> { tickets }
            single<PaymentGateway> { gateway }
            single<PaymentRepository> { payment }
        }
        val context = testKoin(application, overrides)
        val separate = testKoin(application, inMemoryDatabase())
        try {
            val koin = context.koin
            assertSame(cart, koin.get<CartRepository>())
            assertSame(gateway, koin.get<PaymentGateway>())
            assertNotSame(cart, separate.koin.get<CartRepository>())
            assertNotSame(gateway, separate.koin.get<PaymentGateway>())
            koin.get<CartViewModel> { parametersOf("alice") }
            val cleanup = koin.get<CartCleanupViewModel> { parametersOf("alice", fingerprint) }
            koin.get<PaymentViewModel>().submit(order, credentials)
            koin.get<TicketsViewModel> { parametersOf("alice") }
            koin.get<ReceiptViewModel> { parametersOf("alice", "purchase") }
            koin.get<TicketDetailViewModel> { parametersOf("alice", "ticket") }
            scheduler.runCurrent()

            assertEquals(CartCleanupState.Complete, cleanup.state.value)
            verify(exactly = 1) { cart.observe("alice") }
            coVerify(exactly = 1) { cart.clear("alice", fingerprint) }
            verify(exactly = 1) { tickets.observePurchases("alice") }
            verify(exactly = 1) { tickets.observeReceipt("alice", "purchase") }
            verify(exactly = 1) { tickets.observeTicket("alice", "ticket") }
            verify(exactly = 1) { payment.start(order, credentials) }
        } finally {
            context.close()
            separate.close()
        }
    }

    @Test
    fun givenAuthRecoveryWhenLoggingOutThenUsesSharedCartCleanup() = runTest {
        val cart = mockk<CartRepository>()
        val profile = mockk<ProfileRepository>()
        coEvery { profile.currentUser() } returns ProfileResult.Success(ProfileIdentity("alice", null, null))
        coEvery { cart.clearForLogout("alice") } returns CartResult.Success(Unit)
        coEvery { profile.signOut("alice") } returns ProfileResult.Success(Unit)
        val overrides = module {
            single<CartRepository> { cart }
            single<ProfileRepository> { profile }
        }
        val context = testKoin(application, overrides)
        try {
            assertEquals(AuthResult.Success(Unit), context.koin.get<SignOutUseCase>()("alice"))
            coVerifyOrder { cart.clearForLogout("alice"); profile.signOut("alice") }
        } finally {
            context.close()
        }
    }
}
