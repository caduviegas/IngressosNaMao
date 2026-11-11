package com.innaval.ingresosnamao.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innaval.ingresosnamao.feature.auth.presentation.LoginRoute
import com.innaval.ingresosnamao.feature.cart.presentation.CartRoute
import com.innaval.ingresosnamao.feature.events.presentation.EventsRoute
import com.innaval.ingresosnamao.feature.payment.presentation.PaymentRoute
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketsRoute

private object Routes {
    const val LOGIN = "login"
    const val EVENTS = "events"
    const val CART = "cart"
    const val PAYMENT = "payment/{amount}"
    const val TICKETS = "tickets"
    fun payment(amountCents: Long) = "payment/$amountCents"
}

/** Features never navigate to each other; every cross-feature transition is wired here. */
@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginRoute(onLoggedIn = {
                nav.navigate(Routes.EVENTS) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.EVENTS) {
            EventsRoute(onEventClick = { nav.navigate(Routes.CART) })
        }
        composable(Routes.CART) {
            CartRoute(onCheckout = { total -> nav.navigate(Routes.payment(total)) })
        }
        composable(
            Routes.PAYMENT,
            arguments = listOf(navArgument("amount") { type = NavType.LongType }),
        ) { entry ->
            PaymentRoute(
                amountCents = entry.arguments?.getLong("amount") ?: 0L,
                onPaid = { nav.navigate(Routes.TICKETS) { popUpTo(Routes.EVENTS) } },
            )
        }
        composable(Routes.TICKETS) { TicketsRoute() }
    }
}
