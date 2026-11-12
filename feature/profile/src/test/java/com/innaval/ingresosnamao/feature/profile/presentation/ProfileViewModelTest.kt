package com.innaval.ingresosnamao.feature.profile.presentation

import com.innaval.ingresosnamao.feature.profile.domain.GetProfileUseCase
import com.innaval.ingresosnamao.feature.profile.domain.LogoutUseCase
import com.innaval.ingresosnamao.feature.profile.domain.ProfileCartCleaner
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileRepository
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<ProfileRepository>()
    private val cart = mockk<ProfileCartCleaner>()
    private val identity = ProfileIdentity("uid", "Nome", "user@example.com")
    private val profile = UserProfile("Nome", identity.email, null, false)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repo.currentUser() } returns ProfileResult.Success(identity)
        coEvery { repo.load(identity) } returns ProfileResult.Success(profile)
        coEvery { repo.signOut("uid") } returns ProfileResult.Success(Unit)
        coEvery { cart.clear("uid") } returns ProfileResult.Success(Unit)
    }

    @After fun cleanup() = Dispatchers.resetMain()

    private fun create() = ProfileViewModel(GetProfileUseCase(repo), LogoutUseCase(repo, cart))

    @Test fun loadsProfileAndRequiresConfirmationForLogout() = runTest(dispatcher) {
        val vm = create()
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()
        assertEquals(profile, vm.state.value.profile)
        vm.confirmLogout()
        coVerify(exactly = 0) { cart.clear(any()) }
        vm.requestLogout()
        assertTrue(vm.state.value.confirmingLogout)
        vm.cancelLogout()
        assertFalse(vm.state.value.confirmingLogout)
        vm.requestLogout()
        vm.confirmLogout()
        advanceUntilIdle()
        assertTrue(vm.state.value.signedOut)
        coVerify(exactly = 1) { cart.clear("uid") }
    }

    @Test fun readFailureCanRetryOrLogout() = runTest(dispatcher) {
        coEvery { repo.load(identity) } returnsMany listOf(
            ProfileResult.Failure(ProfileError.READ), ProfileResult.Success(profile)
        )
        val vm = create()
        advanceUntilIdle()
        assertEquals(ProfileError.READ, vm.state.value.error)
        assertEquals(identity, vm.state.value.identity)
        vm.load()
        advanceUntilIdle()
        assertEquals(profile, vm.state.value.profile)
        assertEquals(null, vm.state.value.error)
    }

    @Test fun logoutFailureKeepsSessionAndAllowsRetry() = runTest(dispatcher) {
        coEvery { cart.clear("uid") } returnsMany listOf(
            ProfileResult.Failure(ProfileError.CART), ProfileResult.Success(Unit)
        )
        val vm = create()
        advanceUntilIdle()
        vm.requestLogout()
        vm.confirmLogout()
        advanceUntilIdle()
        assertEquals(ProfileError.CART, vm.state.value.error)
        assertFalse(vm.state.value.signedOut)
        coVerify(exactly = 0) { repo.signOut(any()) }
        vm.requestLogout()
        vm.confirmLogout()
        advanceUntilIdle()
        assertTrue(vm.state.value.signedOut)
    }

    @Test fun duplicateActionsAreBlockedWhileLoadingAndLoggingOut() = runTest(dispatcher) {
        val result = CompletableDeferred<ProfileResult<Unit>>()
        coEvery { cart.clear("uid") } coAnswers { result.await() }
        val vm = create()
        vm.load()
        vm.requestLogout()
        runCurrent()
        coVerify(exactly = 1) { repo.load(identity) }
        vm.requestLogout()
        vm.confirmLogout()
        vm.confirmLogout()
        vm.load()
        runCurrent()
        assertTrue(vm.state.value.loggingOut)
        result.complete(ProfileResult.Success(Unit))
        advanceUntilIdle()
        coVerify(exactly = 1) { cart.clear("uid") }
    }

    @Test fun noSessionReturnsToLoginAndAuthFailureCanRetry() = runTest(dispatcher) {
        coEvery { repo.currentUser() } returnsMany listOf(
            ProfileResult.Failure(ProfileError.AUTH), ProfileResult.Success(null)
        )
        val vm = create()
        advanceUntilIdle()
        assertEquals(ProfileError.AUTH, vm.state.value.error)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value.signedOut)
    }
}
