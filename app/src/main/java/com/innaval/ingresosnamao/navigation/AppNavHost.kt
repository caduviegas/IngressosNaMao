package com.innaval.ingresosnamao.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.feature.auth.presentation.LoginRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartCleanupRoute
import com.innaval.ingresosnamao.feature.cart.presentation.cartErrorMessage
import com.innaval.ingresosnamao.feature.cart.domain.AddToCartUseCase
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.cart.domain.CartSnapshot
import com.innaval.ingresosnamao.feature.cart.domain.GetCartUseCase
import com.innaval.ingresosnamao.feature.cart.domain.cartFingerprint
import com.innaval.ingresosnamao.feature.payment.domain.PaymentItem
import com.innaval.ingresosnamao.feature.payment.domain.PaymentOrder
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import com.innaval.ingresosnamao.feature.payment.domain.PaymentState
import com.innaval.ingresosnamao.feature.payment.domain.AttemptStatus
import com.innaval.ingresosnamao.core.ui.components.ErrorState
import com.innaval.ingresosnamao.core.ui.components.LoadingState
import com.innaval.ingresosnamao.BuildConfig
import com.innaval.ingresosnamao.feature.events.presentation.EventsRoute
import com.innaval.ingresosnamao.feature.payment.presentation.PaymentRoute
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketsRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private object Routes {
    const val LOGIN = "login"
    const val EVENTS = "events/{userId}"
    const val CART = "cart/{userId}"
    const val PAYMENT = "payment/{userId}/{amount}/{fingerprint}"
    const val CLEANUP = "payment-cleanup/{userId}/{fingerprint}"
    const val TICKETS = "tickets"
    fun events(userId: String) = "events/${Uri.encode(userId)}"
    fun cart(userId: String) = "cart/${Uri.encode(userId)}"
    fun payment(userId: String, amountCents: Long, fingerprint: String) = "payment/${Uri.encode(userId)}/$amountCents/$fingerprint"
    fun cleanup(userId: String, fingerprint: String) = "payment-cleanup/${Uri.encode(userId)}/$fingerprint"
}

/** Features never navigate to each other; every cross-feature transition is wired here. */
@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.LOGIN) {
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
            var adding by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            Column {
                val attempt = (paymentState as? PaymentState.Active)?.attempt
                if (BuildConfig.DEBUG && attempt?.status == AttemptStatus.APPROVED && attempt.order.userId == userId) {
                    TextButton(onClick = { nav.navigate(Routes.cleanup(userId, attempt.order.fingerprint)) }) {
                        Text("Concluir pagamento de teste pendente")
                    }
                }
                EventsRoute(
                    adding = adding,
                    cartError = error,
                    onCartClick = { nav.navigate(Routes.cart(userId)) },
                    onEventClick = { id, name, price ->
                        if (!adding) {
                            adding = true
                            error = null
                            scope.launch {
                                when (val result = addToCart(userId, CartItem(id, name, price, 1))) {
                                    is CartResult.Success -> {
                                        adding = false
                                        nav.navigate(Routes.cart(userId))
                                    }
                                    is CartResult.Failure -> {
                                        adding = false
                                        error = cartErrorMessage(result.error)
                                    }
                                }
                            }
                        }
                    },
                )
            }
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
            var cartResult by remember(userId, fingerprint) { mutableStateOf<CartResult<CartSnapshot>?>(null) }
            LaunchedEffect(userId, fingerprint) { cartResult = getCart(userId) }
            when (val result = cartResult) {
                null -> LoadingState()
                is CartResult.Failure -> ErrorState(cartErrorMessage(result.error), { nav.popBackStack() })
                is CartResult.Success -> {
                    val cart = result.value
                    if (cartFingerprint(cart) != fingerprint || cart.totalCents != amount) {
                        ErrorState("O carrinho foi alterado. Volte e tente novamente.", { nav.popBackStack() })
                    } else {
                        PaymentRoute(
                            order = PaymentOrder(userId, fingerprint, cart.items.map {
                                PaymentItem(it.eventId, it.eventName, it.quantity, it.unitPriceCents)
                            }, cart.totalCents),
                            enabled = BuildConfig.DEBUG,
                            onPaid = { nav.navigate(Routes.cleanup(userId, fingerprint)) { popUpTo(Routes.PAYMENT) { inclusive = true } } },
                            onBack = { nav.popBackStack() },
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
            val paymentState by payments.state.collectAsStateWithLifecycle()
            var acknowledgementError by remember { mutableStateOf(false) }
            BackHandler {}
            val attempt = (paymentState as? PaymentState.Active)?.attempt
            if (!BuildConfig.DEBUG || attempt?.status != AttemptStatus.APPROVED ||
                attempt.order.userId != userId || attempt.order.fingerprint != fingerprint
            ) {
                ErrorState("Não há aprovação de teste para este carrinho.", { nav.popBackStack() })
            } else if (acknowledgementError) {
                ErrorState("Não foi possível concluir o registro do pagamento de teste.", {
                    acknowledgementError = !payments.acknowledgeApproved(userId, fingerprint)
                    if (!acknowledgementError) nav.navigate(Routes.TICKETS) { popUpTo(Routes.EVENTS) }
                })
            } else {
                CartCleanupRoute(userId, fingerprint, onComplete = {
                    if (payments.acknowledgeApproved(userId, fingerprint)) {
                        nav.navigate(Routes.TICKETS) { popUpTo(Routes.EVENTS) }
                    } else acknowledgementError = true
                })
            }
        }
        composable(Routes.TICKETS) { TicketsRoute() }
    }
}
