package com.innaval.ingresosnamao.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.feature.auth.presentation.LoginRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartCleanupRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartViewModel
import com.innaval.ingresosnamao.feature.cart.presentation.CartUiState
import com.innaval.ingresosnamao.feature.cart.presentation.cartErrorMessage
import com.innaval.ingresosnamao.feature.cart.domain.AddToCartUseCase
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.cart.domain.CartSnapshot
import com.innaval.ingresosnamao.feature.cart.domain.GetCartUseCase
import com.innaval.ingresosnamao.feature.cart.domain.ValidateCartAvailabilityUseCase
import com.innaval.ingresosnamao.feature.cart.domain.cartFingerprint
import com.innaval.ingresosnamao.feature.payment.domain.PaymentItem
import com.innaval.ingresosnamao.feature.payment.domain.PaymentOrder
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import com.innaval.ingresosnamao.feature.payment.domain.PaymentState
import com.innaval.ingresosnamao.feature.payment.domain.TestCredentials
import com.innaval.ingresosnamao.feature.payment.domain.AttemptStatus
import com.innaval.ingresosnamao.core.ui.components.ErrorState
import com.innaval.ingresosnamao.core.ui.components.IngressosBackScreen
import com.innaval.ingresosnamao.core.ui.components.LoadingState
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.BuildConfig
import com.innaval.ingresosnamao.feature.events.presentation.EventsRoute
import com.innaval.ingresosnamao.feature.payment.presentation.PaymentRoute
import com.innaval.ingresosnamao.feature.payment.presentation.SimulationRecoveryAction
import com.innaval.ingresosnamao.feature.profile.presentation.ProfileRoute
import com.innaval.ingresosnamao.feature.profile.presentation.ProfileViewModel
import com.innaval.ingresosnamao.feature.profile.presentation.ProfileUiState
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketsRoute
import com.innaval.ingresosnamao.feature.ticket.presentation.ReceiptRoute
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketDetailRoute
import com.innaval.ingresosnamao.feature.ticket.domain.IssueTicketsUseCase
import com.innaval.ingresosnamao.feature.ticket.domain.PurchaseItem
import com.innaval.ingresosnamao.feature.ticket.domain.TicketError
import com.innaval.ingresosnamao.feature.ticket.domain.TicketResult
import com.innaval.ingresosnamao.feature.ticket.domain.validPurchaseItems
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private object Routes {
    const val LOGIN = "login"
    const val PROFILE = "profile/{userId}"
    const val EVENTS = "events/{userId}"
    const val CART = "cart/{userId}"
    const val PAYMENT = "payment/{userId}/{amount}/{fingerprint}"
    const val CLEANUP = "payment-cleanup/{userId}/{fingerprint}"
    const val TICKETS = "tickets/{userId}"
    const val RECEIPT = "receipt/{userId}/{reference}"
    const val TICKET = "ticket/{userId}/{ticketId}"
    fun events(userId: String) = "events/${Uri.encode(userId)}"
    fun profile(userId: String) = "profile/${Uri.encode(userId)}"
    fun cart(userId: String) = "cart/${Uri.encode(userId)}"
    fun payment(userId: String, amountCents: Long, fingerprint: String) = "payment/${Uri.encode(userId)}/$amountCents/$fingerprint"
    fun cleanup(userId: String, fingerprint: String) = "payment-cleanup/${Uri.encode(userId)}/$fingerprint"
    fun tickets(userId: String) = "tickets/${Uri.encode(userId)}"
    fun receipt(userId: String, reference: String) = "receipt/${Uri.encode(userId)}/${Uri.encode(reference)}"
    fun ticket(userId: String, ticketId: String) = "ticket/${Uri.encode(userId)}/${Uri.encode(ticketId)}"
}
private fun tabForRoute(route: String?) = when (route) {
    Routes.EVENTS -> MainTab.HOME
    Routes.TICKETS, Routes.RECEIPT, Routes.TICKET -> MainTab.ORDERS
    Routes.CART -> MainTab.CART
    Routes.PROFILE -> MainTab.PROFILE
    else -> null
}
private fun tabRoute(tab: MainTab, userId: String) = when (tab) {
    MainTab.HOME -> Routes.events(userId)
    MainTab.ORDERS -> Routes.tickets(userId)
    MainTab.CART -> Routes.cart(userId)
    MainTab.PROFILE -> Routes.profile(userId)
}

private fun navigationBlocked(adding: Boolean, cart: CartUiState?, profile: ProfileUiState?): Boolean =
    adding || cart?.busy == true || profile?.loggingOut == true || profile?.signedOut == true

@Composable
private fun MainNavigationScaffold(
    nav: NavHostController,
    addingState: MutableState<Boolean>,
    content: @Composable (Modifier) -> Unit,
) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val entry = backStackEntry
    val route = entry?.destination?.route
    val userId = entry?.arguments?.getString("userId")
    val selected = tabForRoute(route)
    val adding by addingState
    val cartViewModel = if (route == Routes.CART && userId != null) {
        koinViewModel<CartViewModel>(viewModelStoreOwner = entry, key = userId, parameters = { parametersOf(userId) })
    } else null
    val profileViewModel = if (route == Routes.PROFILE) {
        koinViewModel<ProfileViewModel>(viewModelStoreOwner = entry)
    } else null
    val cartState = cartViewModel?.state?.collectAsStateWithLifecycle()
    val profileState = profileViewModel?.state?.collectAsStateWithLifecycle()
    val blocked = navigationBlocked(adding, cartState?.value, profileState?.value)
    BackHandler(enabled = blocked) {}
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selected != null && userId != null) {
                BottomNavigation(selected, !blocked) { tab ->
                    val rootRoutes = setOf(Routes.EVENTS, Routes.TICKETS, Routes.CART, Routes.PROFILE)
                    val alreadySelected = tab == selected && route in rootRoutes
                    val navigationBusy = navigationBlocked(
                        addingState.value, cartViewModel?.state?.value, profileViewModel?.state?.value,
                    )
                    if (!alreadySelected && !navigationBusy) {
                        nav.navigate(tabRoute(tab, userId)) {
                            popUpTo(Routes.EVENTS)
                            launchSingleTop = true
                        }
                    }
                }
            }
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

/** Features never navigate to each other; every cross-feature transition is wired here. */
@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    val addingState = remember { mutableStateOf(false) }
    MainNavigationScaffold(nav, addingState) { modifier ->
    NavHost(nav, startDestination = Routes.LOGIN, modifier = modifier) {
        composable(Routes.LOGIN) {
            LoginRoute(onLoggedIn = { userId ->
                nav.navigate(Routes.events(userId)) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.EVENTS, arguments = listOf(navArgument("userId") { type = NavType.StringType })) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            val addToCart: AddToCartUseCase = koinInject()
            val payments: PaymentRepository = koinInject()
            val paymentState by payments.state.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            var error by remember { mutableStateOf<String?>(null) }
            Column(verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm)) {
                val attempt = (paymentState as? PaymentState.Active)?.attempt
                if (BuildConfig.DEBUG && attempt?.status == AttemptStatus.APPROVED && attempt.order.userId == userId) {
                    TextButton(
                        onClick = { nav.navigate(Routes.cleanup(userId, attempt.order.fingerprint)) },
                        enabled = !addingState.value,
                    ) {
                        Text("Concluir pagamento pendente")
                    }
                }
                if (BuildConfig.DEBUG && paymentState != PaymentState.Ready && !addingState.value) {
                    SimulationRecoveryAction(paymentState)
                }
                EventsRoute(
                    adding = addingState.value,
                    cartError = error,
                    onEventClick = { id, name, price ->
                        if (!addingState.value) {
                            addingState.value = true
                            error = null
                            scope.launch {
                                when (val result = addToCart(userId, CartItem(id, name, price, 1))) {
                                    is CartResult.Success -> {
                                        addingState.value = false
                                        nav.navigate(Routes.cart(userId))
                                    }
                                    is CartResult.Failure -> {
                                        addingState.value = false
                                        error = cartErrorMessage(result.error)
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
        composable(Routes.PROFILE, arguments = listOf(navArgument("userId") { type = NavType.StringType })) {
            ProfileRoute(
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(nav.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.CART, arguments = listOf(navArgument("userId") { type = NavType.StringType })) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            CartRoute(
                userId = userId,
                onCheckout = { total, fingerprint -> nav.navigate(Routes.payment(userId, total, fingerprint)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.PAYMENT,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.LongType },
                navArgument("fingerprint") { type = NavType.StringType },
            ),
        ) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            val fingerprint = requireNotNull(entry.arguments?.getString("fingerprint"))
            val amount = requireNotNull(entry.arguments).getLong("amount")
            val getCart: GetCartUseCase = koinInject()
            val validateCart: ValidateCartAvailabilityUseCase = koinInject()
            var cartResult by remember(userId, fingerprint) { mutableStateOf<CartResult<CartSnapshot>?>(null) }
            LaunchedEffect(userId, fingerprint) { cartResult = getCart(userId) }
            when (val result = cartResult) {
                null -> LoadingState()
                is CartResult.Failure -> PaymentBackError(cartErrorMessage(result.error)) { nav.popBackStack() }
                is CartResult.Success -> {
                    val cart = result.value
                    if (cartFingerprint(cart) != fingerprint || cart.totalCents != amount) {
                        PaymentBackError("O carrinho foi alterado. Volte e tente novamente.") { nav.popBackStack() }
                    } else if (!validPurchaseItems(cart.items.map {
                            PurchaseItem(it.eventId, it.eventName, it.unitPriceCents, it.quantity)
                        }, cart.totalCents)
                    ) {
                        PaymentBackError("A compra deve conter até 100 ingressos e um total válido.") {
                            nav.popBackStack()
                        }
                    } else {
                        PaymentRoute(
                            order = PaymentOrder(userId, fingerprint, cart.items.map {
                                PaymentItem(it.eventId, it.eventName, it.quantity, it.unitPriceCents)
                            }, cart.totalCents),
                            enabled = BuildConfig.DEBUG,
                            credentials = TestCredentials(BuildConfig.CIELO_CLIENT_ID, BuildConfig.CIELO_ACCESS_TOKEN),
                            onPaid = { nav.navigate(Routes.cleanup(userId, fingerprint)) { popUpTo(Routes.PAYMENT) { inclusive = true } } },
                            onBack = { nav.popBackStack() },
                            validate = {
                                when (val current = getCart(userId)) {
                                    is CartResult.Failure -> cartErrorMessage(current.error)
                                    is CartResult.Success -> {
                                        val updated = current.value
                                        if (cartFingerprint(updated) != fingerprint || updated.totalCents != amount) {
                                            cartErrorMessage(CartError.CART_CHANGED)
                                        } else when (val check = validateCart(updated)) {
                                            is CartResult.Failure -> cartErrorMessage(check.error)
                                            is CartResult.Success -> null
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        composable(
            Routes.CLEANUP,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("fingerprint") { type = NavType.StringType },
            ),
        ) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            val fingerprint = requireNotNull(entry.arguments?.getString("fingerprint"))
            val payments: PaymentRepository = koinInject()
            val issueTickets: IssueTicketsUseCase = koinInject()
            val paymentState by payments.state.collectAsStateWithLifecycle()
            var acknowledgementError by remember { mutableStateOf(false) }
            var issueResult by remember(userId, fingerprint) { mutableStateOf<TicketResult<Unit>?>(null) }
            var issueRetry by remember(userId, fingerprint) { mutableStateOf(0) }
            BackHandler {}
            val attempt = (paymentState as? PaymentState.Active)?.attempt
            val purchase = approvedSimulation(BuildConfig.DEBUG, userId, fingerprint, attempt)
            if (paymentState is PaymentState.StorageError) {
                Column(
                    modifier = Modifier.padding(IngressosTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm),
                ) {
                    Text(
                        "Falha ao salvar o estado do pagamento. Os ingressos já emitidos podem ser consultados; " +
                            "não pague novamente."
                    )
                    TextButton(onClick = { nav.navigate(Routes.tickets(userId)) { popUpTo(Routes.EVENTS) } }) {
                        Text("Ver ingressos")
                    }
                }
            } else if (purchase == null) {
                PaymentBackError("Não há aprovação para este carrinho.") { nav.popBackStack() }
            } else {
                val reference = purchase.reference
                LaunchedEffect(reference, issueRetry) {
                    issueResult = null
                    issueResult = when (val result = issueTickets(purchase)) {
                        is TicketResult.Success -> TicketResult.Success(Unit)
                        is TicketResult.Failure -> result
                    }

                }
                if (acknowledgementError) {
                    ErrorState("Não foi possível concluir o registro do pagamento.", {
                        acknowledgementError = !payments.acknowledgeApproved(userId, fingerprint)
                        if (!acknowledgementError) nav.navigate(Routes.receipt(userId, reference)) { popUpTo(Routes.EVENTS) }
                    })
                } else when (val result = issueResult) {
                    null -> LoadingState()
                    is TicketResult.Failure -> ErrorState(
                        when (result.error) {
                            TicketError.INVALID_PURCHASE -> "Dados da compra inválidos ou limite de 100 ingressos excedido; não pague novamente."
                            TicketError.CONFLICT -> "Os dados da compra conflitam com uma emissão existente; não pague novamente."
                            TicketError.STORAGE -> "Falha ao salvar os ingressos. Tente novamente sem pagar outra vez."
                        },
                        { issueRetry++ },
                    )
                    is TicketResult.Success -> CartCleanupRoute(userId, fingerprint, onComplete = {
                        if (payments.acknowledgeApproved(userId, fingerprint)) {
                            nav.navigate(Routes.receipt(userId, reference)) { popUpTo(Routes.EVENTS) }
                        } else acknowledgementError = true
                    })
                }

            }
        }
        composable(Routes.TICKETS, arguments = listOf(navArgument("userId") { type = NavType.StringType })) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            TicketsRoute(userId, onReceipt = { nav.navigate(Routes.receipt(userId, it)) }, onBack = { nav.popBackStack() })
        }
        composable(Routes.RECEIPT, arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("reference") { type = NavType.StringType },
        )) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            val reference = requireNotNull(entry.arguments?.getString("reference"))
            ReceiptRoute(userId, reference, onTicket = { nav.navigate(Routes.ticket(userId, it)) }, onBack = { nav.popBackStack() })
        }
        composable(Routes.TICKET, arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("ticketId") { type = NavType.StringType },
        )) { entry ->
            val userId = requireNotNull(entry.arguments?.getString("userId"))
            val ticketId = requireNotNull(entry.arguments?.getString("ticketId"))
            TicketDetailRoute(userId, ticketId, onBack = { nav.popBackStack() })
        }
    }
    }
}

@Composable
private fun PaymentBackError(message: String, onBack: () -> Unit) {
    IngressosBackScreen("Pagamento", onBack) { ErrorState(message) }
}
