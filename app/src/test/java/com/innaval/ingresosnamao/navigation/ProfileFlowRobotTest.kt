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
import com.innaval.ingresosnamao.feature.auth.domain.UserProfile as AuthProfile
import com.innaval.ingresosnamao.feature.cart.domain.CartRepository
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import com.innaval.ingresosnamao.feature.events.domain.EventList
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import com.innaval.ingresosnamao.feature.payment.domain.PaymentState
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileRepository
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class ProfileFlowRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenAuthenticatedUserWhenLoggingOutThenClearsCartAndNavigationHistory() {
        val auth = mockk<AuthRepository>()
        val events = mockk<EventRepository>()
        val cart = mockk<CartRepository>()
        val payment = mockk<PaymentRepository>()
        val profiles = mockk<ProfileRepository>()
        val authUser = AuthUser("alice", "alice@example.com", "Alice")
        val identity = ProfileIdentity("alice", "Alice", "alice@example.com")
        var signedIn = true
        coEvery { auth.currentUser() } answers { AuthResult.Success(if (signedIn) authUser else null) }
        coEvery { auth.getProfile(authUser) } returns AuthResult.Success(AuthProfile("Alice", authUser.email, "Rua"))
        coEvery { events.getEvents() } returns EventsResult.Success(EventList(emptyList(), false))
        every { payment.state } returns MutableStateFlow(PaymentState.Ready)
        coEvery { profiles.currentUser() } returns ProfileResult.Success(identity)
        coEvery { profiles.load(identity) } returns ProfileResult.Success(
            UserProfile("Alice", authUser.email, "Rua", false)
        )
        coEvery { cart.clearForLogout("alice") } returns CartResult.Success(Unit)
        coEvery { profiles.signOut("alice") } coAnswers {
            signedIn = false
            ProfileResult.Success(Unit)
        }
        val overrides = module {
            single<AuthRepository> { auth }
            single<EventRepository> { events }
            single<CartRepository> { cart }
            single<PaymentRepository> { payment }
            single<ProfileRepository> { profiles }
        }
        val application = ApplicationProvider.getApplicationContext<Application>()
        lateinit var nav: NavHostController
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
        val robot = ProfileFlowRobot(compose)
        robot.action.openProfile()
        robot.result.profileShown()
        robot.action.requestLogout()
        robot.action.confirmLogout()
        robot.result.loginShown()
        compose.runOnIdle {
            assertEquals("login", nav.currentDestination?.route)
            assertNull(nav.previousBackStackEntry)
        }
        coVerifyOrder { cart.clearForLogout("alice"); profiles.signOut("alice") }
    }
}
