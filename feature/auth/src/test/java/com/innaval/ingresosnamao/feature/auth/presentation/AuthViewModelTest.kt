package com.innaval.ingresosnamao.feature.auth.presentation

import com.innaval.ingresosnamao.feature.auth.domain.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AuthRepository>()
    private val user = AuthUser("uid", "user@example.com", "Pessoa")
    private val profile = UserProfile("Pessoa", "user@example.com", "Rua 1")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.currentUser() } returns AuthResult.Success(null)
        every { repository.signOut() } returns Unit
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AuthViewModel(
        LoginUseCase(repository), GoogleLoginUseCase(repository), RegisterUseCase(repository),
        GetCurrentUserUseCase(repository), GetProfileUseCase(repository), SaveProfileUseCase(repository),
        SignOutUseCase(repository),
    )

    @Test fun invalidInputsDoNotCallRepository() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.submitLogin()
        assertEquals(setOf(Field.EMAIL, Field.PASSWORD), vm.state.value.invalidFields)
        vm.showRegister()
        vm.changeEmail("bad")
        vm.changePassword("12345")
        vm.submitRegistration()
        assertEquals(Field.entries.toSet(), vm.state.value.invalidFields)
        coVerify(exactly = 0) { repository.login(any(), any()) }
        coVerify(exactly = 0) { repository.register(any(), any()) }
    }

    @Test fun registrationValidationChecksBoundaries() {
        assertTrue(AuthValidation.registration(Registration("A", "a@b.co", "Rua", "123456")).isEmpty())
        assertEquals(
            setOf(Field.NAME, Field.ADDRESS, Field.PASSWORD),
            AuthValidation.registration(Registration("a".repeat(101), "a@b.co", "a".repeat(301), "12345"))
        )
        assertTrue(AuthValidation.login("a@b.co", "x".repeat(129)).isEmpty())
    }

    @Test fun loginWaitsForServerProfileBeforeAuthenticating() = runTest(dispatcher) {
        coEvery { repository.login("user@example.com", "secret") } returns AuthResult.Success(user)
        coEvery { repository.getProfile(user) } returns AuthResult.Success(profile)
        val vm = viewModel()
        advanceUntilIdle()
        vm.changeEmail(" user@example.com ")
        vm.changePassword("secret")
        vm.submitLogin()
        advanceUntilIdle()
        assertEquals(AuthStage.AUTHENTICATED, vm.state.value.stage)
        assertEquals("", vm.state.value.password)
        coVerify(exactly = 1) { repository.getProfile(user) }
    }

    @Test fun missingGoogleProfileRequiresCompletion() = runTest(dispatcher) {
        coEvery { repository.loginWithGoogle("token") } returns AuthResult.Success(user)
        coEvery { repository.getProfile(user) } returns AuthResult.Success(null)
        val vm = viewModel()
        advanceUntilIdle()
        vm.submitGoogle { AuthResult.Success("token") }
        advanceUntilIdle()
        assertEquals(AuthStage.COMPLETE_PROFILE, vm.state.value.stage)
        assertEquals(user.email, vm.state.value.email)
        assertEquals(user.name, vm.state.value.name)
    }

    @Test fun existingSessionWithReadFailureCanRetryWithoutAssumingMissingProfile() = runTest(dispatcher) {
        coEvery { repository.currentUser() } returns AuthResult.Success(user)
        coEvery { repository.getProfile(user) } returnsMany listOf(
            AuthResult.Failure(AuthError.PROFILE_READ), AuthResult.Success(profile)
        )
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(AuthStage.CHECK_PROFILE, vm.state.value.stage)
        assertEquals(AuthError.PROFILE_READ, vm.state.value.error)
        vm.retryProfile()
        advanceUntilIdle()
        assertEquals(AuthStage.AUTHENTICATED, vm.state.value.stage)
    }

    @Test fun registrationProfileFailureCanBeRetriedWithoutRecreatingAccount() = runTest(dispatcher) {
        coEvery { repository.register("user@example.com", "secret") } returns AuthResult.Success(user)
        coEvery { repository.saveProfile(user, profile) } returnsMany listOf(
            AuthResult.Failure(AuthError.PROFILE_WRITE), AuthResult.Success(Unit)
        )
        val vm = viewModel()
        advanceUntilIdle()
        vm.showRegister()
        vm.changeName(" Pessoa ")
        vm.changeEmail(" user@example.com ")
        vm.changeAddress(" Rua 1 ")
        vm.changePassword("secret")
        vm.submitRegistration()
        advanceUntilIdle()
        assertEquals(AuthStage.COMPLETE_PROFILE, vm.state.value.stage)
        assertEquals(AuthError.PROFILE_WRITE, vm.state.value.error)
        assertEquals("", vm.state.value.password)
        vm.submitProfile()
        advanceUntilIdle()
        assertEquals(AuthStage.AUTHENTICATED, vm.state.value.stage)
        coVerify(exactly = 1) { repository.register(any(), any()) }
        coVerify(exactly = 2) { repository.saveProfile(user, profile) }
    }

    @Test fun duplicateSubmitIsIgnoredAndErrorsAreVisible() = runTest(dispatcher) {
        val result = CompletableDeferred<AuthResult<AuthUser>>()
        coEvery { repository.login(any(), any()) } coAnswers { result.await() }
        val vm = viewModel()
        advanceUntilIdle()
        vm.changeEmail(user.email)
        vm.changePassword("secret")
        vm.submitLogin()
        vm.submitLogin()
        runCurrent()
        assertTrue(vm.state.value.loading)
        result.complete(AuthResult.Failure(AuthError.INVALID_CREDENTIALS))
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertEquals(AuthError.INVALID_CREDENTIALS, vm.state.value.error)
        coVerify(exactly = 1) { repository.login(any(), any()) }
    }

    @Test fun googleCancellationIsNotSuccess() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.submitGoogle { AuthResult.Failure(AuthError.GOOGLE_CANCELLED) }
        advanceUntilIdle()
        assertEquals(AuthStage.LOGIN, vm.state.value.stage)
        assertEquals(AuthError.GOOGLE_CANCELLED, vm.state.value.error)
        coVerify(exactly = 0) { repository.loginWithGoogle(any()) }
    }

    @Test fun useCasePropagatesCancellation() = runTest(dispatcher) {
        coEvery { repository.login(any(), any()) } throws CancellationException("cancelled")
        try {
            LoginUseCase(repository)("user@example.com", "secret")
            throw AssertionError("CancellationException expected")
        } catch (_: CancellationException) {
            // Expected: cancellation must never become an authentication error.
        }
    }
}
