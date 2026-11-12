package com.innaval.ingresosnamao.core.common.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class AppResultTest {
    @Test fun mapTransformsSuccessAndPreservesFailure() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })
        val failure = AppResult.Failure(AppError.Network)
        assertSame(failure, failure.map { error("must not be called") })
    }

    @Test fun catchesFailuresButPropagatesCancellation() = runTest {
        assertEquals(AppResult.Success("ok"), runCatchingApp { "ok" })
        val cause = IllegalStateException("failure")
        val failure = runCatchingApp { throw cause } as AppResult.Failure
        assertSame(cause, (failure.error as AppError.Unknown).cause)
        val cancelled = CancellationException("cancelled")
        try {
            runCatchingApp { throw cancelled }
        } catch (exception: CancellationException) {
            assertSame(cancelled, exception)
            return@runTest
        }
        fail("CancellationException must be propagated")
    }
}
