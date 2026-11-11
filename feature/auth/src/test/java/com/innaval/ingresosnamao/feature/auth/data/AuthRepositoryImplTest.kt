package com.innaval.ingresosnamao.feature.auth.data

import com.google.firebase.FirebaseNetworkException
import com.innaval.ingresosnamao.feature.auth.domain.AuthError
import com.innaval.ingresosnamao.feature.auth.domain.AuthResult
import com.innaval.ingresosnamao.feature.auth.domain.AuthUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AuthRepositoryImplTest {
    private val auth = mockk<FirebaseAuthDataSource>()
    private val profiles = mockk<FirestoreProfileDataSource>()
    private val repository = AuthRepositoryImpl(auth, profiles)
    private val user = AuthUser("uid", "user@example.com")

    @Test fun missingFirebaseConfigurationIsExplicit() = runTest {
        every { auth.currentUser() } throws IllegalStateException("not configured")
        assertEquals(AuthResult.Failure(AuthError.CONFIGURATION), repository.currentUser())
    }

    @Test fun networkFailureIsExplicit() = runTest {
        coEvery { auth.login(any(), any()) } throws mockk<FirebaseNetworkException>()
        assertEquals(AuthResult.Failure(AuthError.NETWORK), repository.login(user.email, "secret"))
    }

    @Test fun profileFailureIsNotReportedAsMissing() = runTest {
        coEvery { profiles.get(user) } throws RuntimeException("read failed")
        assertEquals(AuthResult.Failure(AuthError.PROFILE_READ), repository.getProfile(user))
    }

    @Test fun dataSourceCancellationPropagates() = runTest {
        coEvery { profiles.get(user) } throws CancellationException("cancelled")
        try {
            repository.getProfile(user)
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            // Cancellation is not a profile error.
        }
    }
}
