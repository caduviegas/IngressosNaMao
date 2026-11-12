package com.innaval.ingresosnamao.feature.profile.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutUseCaseTest {
    private val repo = mockk<ProfileRepository>()
    private val cart = mockk<ProfileCartCleaner>()
    private val logout = LogoutUseCase(repo, cart)
    private val identity = ProfileIdentity("alice", null, null)

    @Test fun clearsBeforeSigningOut() = runTest {
        coEvery { repo.currentUser() } returns ProfileResult.Success(identity)
        coEvery { cart.clear("alice") } returns ProfileResult.Success(Unit)
        coEvery { repo.signOut("alice") } returns ProfileResult.Success(Unit)
        assertEquals(ProfileResult.Success(Unit), logout("alice"))
        coVerifyOrder { repo.currentUser(); cart.clear("alice"); repo.signOut("alice") }
    }

    @Test fun sessionChangeOrCartFailureNeverSignsOut() = runTest {
        coEvery { repo.currentUser() } returns ProfileResult.Success(identity.copy(uid = "bob"))
        assertEquals(ProfileResult.Failure(ProfileError.SESSION_CHANGED), logout("alice"))
        coVerify(exactly = 0) { cart.clear(any()) }
        coEvery { repo.currentUser() } returns ProfileResult.Success(identity)
        coEvery { cart.clear("alice") } returns ProfileResult.Failure(ProfileError.CART)
        assertEquals(ProfileResult.Failure(ProfileError.CART), logout("alice"))
        coVerify(exactly = 0) { repo.signOut(any()) }
    }

    @Test fun errorsAfterClearAndCancellationRemainExplicit() = runTest {
        coEvery { repo.currentUser() } returns ProfileResult.Success(identity)
        coEvery { cart.clear("alice") } returns ProfileResult.Success(Unit)
        coEvery { repo.signOut("alice") } returns ProfileResult.Failure(ProfileError.AUTH)
        assertEquals(ProfileResult.Failure(ProfileError.SIGN_OUT_AFTER_CLEAR), logout("alice"))
        coEvery { repo.signOut("alice") } returns ProfileResult.Failure(ProfileError.SESSION_CHANGED)
        assertEquals(ProfileResult.Failure(ProfileError.SESSION_CHANGED_AFTER_CLEAR), logout("alice"))
        coEvery { cart.clear("alice") } throws CancellationException("cancelled")
        try {
            logout("alice")
            throw AssertionError("Cancellation expected")
        } catch (_: CancellationException) {
            coVerify(exactly = 2) { repo.signOut("alice") }
        }
    }

    @Test fun authenticationFailureStopsBeforeCart() = runTest {
        coEvery { repo.currentUser() } returns ProfileResult.Failure(ProfileError.AUTH)
        assertEquals(ProfileResult.Failure(ProfileError.AUTH), logout("alice"))
        coVerify(exactly = 0) { cart.clear(any()) }
    }
}
