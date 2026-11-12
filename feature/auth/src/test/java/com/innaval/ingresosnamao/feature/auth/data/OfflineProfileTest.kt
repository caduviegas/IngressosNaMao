package com.innaval.ingresosnamao.feature.auth.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class OfflineProfileTest {
    private class Offline : Exception()
    private class Denied : Exception()

    @Test fun givenOfflineProfileWhenLoadedThenUsesOnlyConfirmedCache() = runTest {
        val unavailable = Offline()
        assertEquals("profile", readWithConfirmedCache(
            server = { throw unavailable }, cache = { "profile" }, confirmed = { true }, canUseCache = { it is Offline }
        ))
        try {
            readWithConfirmedCache(
                server = { throw unavailable }, cache = { "pending" }, confirmed = { false }, canUseCache = { it is Offline }
            )
            fail("Unconfirmed profile must not be used")
        } catch (e: Offline) {
            assertEquals(unavailable, e)
        }
    }

    @Test fun givenPermissionFailureWhenLoadingProfileThenDoesNotUseCache() = runTest {
        val denied = Denied()
        try {
            readWithConfirmedCache(
                server = { throw denied }, cache = { fail("Unexpected cache read"); "profile" },
                confirmed = { true }, canUseCache = { it is Offline }
            )
            fail("Permission error expected")
        } catch (e: Denied) {
            assertEquals(denied, e)
        }
    }

    @Test fun givenCancelledCacheWhenLoadingProfileThenPropagatesCancellation() = runTest {
        try {
            readWithConfirmedCache(
                server = { throw Offline() }, cache = { throw CancellationException() },
                confirmed = { _: String -> true }, canUseCache = { it is Offline }
            )
            fail("Cancellation expected")
        } catch (_: CancellationException) {
            // Offline fallback must not swallow cancellation.
        }
    }
}
