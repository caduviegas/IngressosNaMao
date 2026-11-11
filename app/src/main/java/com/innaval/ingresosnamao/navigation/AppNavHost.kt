package com.innaval.ingresosnamao.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innaval.ingresosnamao.feature.auth.presentation.LoginRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartCleanupRoute
import com.innaval.ingresosnamao.feature.cart.presentation.cartErrorMessage
import com.innaval.ingresosnamao.feature.cart.domain.AddToCartUseCase
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
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
            val scope = rememberCoroutineScope()
            var adding by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
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
            PaymentRoute(
                amountCents = requireNotNull(entry.arguments).getLong("amount"),
                onPaid = { nav.navigate(Routes.cleanup(userId, fingerprint)) { popUpTo(Routes.PAYMENT) { inclusive = true } } },
            )
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
            BackHandler {}
            CartCleanupRoute(userId, fingerprint, onComplete = {
                nav.navigate(Routes.TICKETS) { popUpTo(Routes.EVENTS) }
            })
        }
        composable(Routes.TICKETS) { TicketsRoute() }
    }
}
