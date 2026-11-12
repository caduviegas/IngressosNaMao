package com.innaval.ingresosnamao.feature.profile.data

import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRepositoryTest {
    private val auth = mockk<ProfileAuthSource>()
    private val documents = mockk<ProfileDocumentSource>()
    private val repo = FirebaseProfileRepository(auth, documents)
    private val identity = ProfileIdentity("uid", "Nome Firebase", "user@example.com")

    @Test fun loadsProfileAndMarksCachedData() = runTest {
        every { auth.currentUser() } returns identity
        coEvery { documents.get("uid") } returns ProfileDocument(
            mapOf("name" to " Ana ", "email" to "USER@example.com", "address" to " Rua 1 "), true
        )
        assertEquals(
            ProfileResult.Success(UserProfile("Ana", "user@example.com", "Rua 1", true)),
            repo.load(identity),
        )
    }

    @Test fun missingDocumentAndFieldsUseAvailableIdentityWithoutCrashing() = runTest {
        every { auth.currentUser() } returns identity
        coEvery { documents.get("uid") } returns ProfileDocument(null, false)
        assertEquals(
            ProfileResult.Success(UserProfile("Nome Firebase", "user@example.com", null, false)),
            repo.load(identity),
        )
        val noData = ProfileIdentity("uid", null, null)
        every { auth.currentUser() } returns noData
        coEvery { documents.get("uid") } returns ProfileDocument(mapOf("name" to "", "address" to " "), false)
        assertEquals(ProfileResult.Success(UserProfile(null, null, null, false)), repo.load(noData))
    }

    @Test fun invalidFieldsAndConflictingEmailAreErrors() = runTest {
        every { auth.currentUser() } returns identity
        coEvery { documents.get("uid") } returns ProfileDocument(mapOf("address" to 12), false)
        assertEquals(ProfileResult.Failure(ProfileError.INVALID_DATA), repo.load(identity))
        coEvery { documents.get("uid") } returns ProfileDocument(mapOf("email" to "other@example.com"), false)
        assertEquals(ProfileResult.Failure(ProfileError.INVALID_DATA), repo.load(identity))
    }

    @Test fun readErrorsAndSessionChangesAreExplicit() = runTest {
        every { auth.currentUser() } returns identity
        coEvery { documents.get("uid") } throws IllegalStateException("firestore")
        assertEquals(ProfileResult.Failure(ProfileError.READ), repo.load(identity))
        coEvery { documents.get("uid") } returns ProfileDocument(null, false)
        every { auth.currentUser() } returnsMany listOf(identity, identity.copy(uid = "other"))
        assertEquals(ProfileResult.Failure(ProfileError.SESSION_CHANGED), repo.load(identity))
    }

    @Test fun signOutDoesNotDisconnectAnotherSession() = runTest {
        every { auth.currentUser() } returns identity.copy(uid = "other")
        assertEquals(ProfileResult.Failure(ProfileError.SESSION_CHANGED), repo.signOut("uid"))
        verify(exactly = 0) { auth.signOut() }
        every { auth.currentUser() } returns identity
        every { auth.signOut() } returns Unit
        assertEquals(ProfileResult.Success(Unit), repo.signOut("uid"))
        verify(exactly = 1) { auth.signOut() }
    }

    @Test fun currentUserErrorsAndCancellationAreNotSuccess() = runTest {
        every { auth.currentUser() } throws IllegalStateException("auth")
        assertEquals(ProfileResult.Failure(ProfileError.AUTH), repo.currentUser())
        every { auth.currentUser() } returns identity
        coEvery { documents.get("uid") } throws CancellationException("cancelled")
        try {
            repo.load(identity)
            throw AssertionError("Cancellation expected")
        } catch (_: CancellationException) {
            coVerify(exactly = 1) { documents.get("uid") }
        }
    }
}
