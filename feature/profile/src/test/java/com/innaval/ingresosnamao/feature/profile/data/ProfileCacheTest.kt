package com.innaval.ingresosnamao.feature.profile.data

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileCacheTest {
    private val unavailable = FirebaseFirestoreException("offline", FirebaseFirestoreException.Code.UNAVAILABLE)

    @Test fun givenNetworkFailureWhenLoadingProfileThenUsesOnlyConfirmedCache() = runTest {
        assertEquals("server", readConfirmedProfile({ "server" }, { "cache" }, { true }))
        assertEquals("cache", readConfirmedProfile({ throw unavailable }, { "cache" }, { true }))
        try {
            readConfirmedProfile({ throw unavailable }, { "pending" }, { false })
            throw AssertionError("Expected network error")
        } catch (e: FirebaseFirestoreException) {
            assertSame(unavailable, e)
        }
    }

    @Test fun givenPermissionFailureWhenLoadingProfileThenDoesNotReadCache() = runTest {
        val denied = FirebaseFirestoreException("denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        var cacheRead = false
        try {
            readConfirmedProfile({ throw denied }, { cacheRead = true; "cache" }, { true })
            throw AssertionError("Expected permission error")
        } catch (e: FirebaseFirestoreException) {
            assertSame(denied, e)
            assertEquals(false, cacheRead)
        }
    }

    @Test fun givenCacheFailureWhenLoadingProfileThenPreservesFailureOrCancellation() = runTest {
        val cacheError = IllegalStateException("cache")
        try {
            readConfirmedProfile<String>({ throw unavailable }, { throw cacheError }, { true })
            throw AssertionError("Expected network error")
        } catch (e: FirebaseFirestoreException) {
            assertSame(unavailable, e)
            assertTrue(e.suppressed.contains(cacheError))
        }
        try {
            readConfirmedProfile<String>({ throw unavailable }, { throw CancellationException() }, { true })
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            // Cancellation must not become a Firestore error.
        }
    }
}
