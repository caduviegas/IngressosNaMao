package com.innaval.ingresosnamao.feature.ticket.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.feature.ticket.domain.QrResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeTest {
    private val dispatchers = mockk<DispatcherProvider> { every { default } returns Dispatchers.Default }

    @Test fun givenTicketIdWhenGeneratingQrThenEncodesOnlyId() = runBlocking {
        val id = "550e8400-e29b-41d4-a716-446655440000"
        val pattern = (ZxingQrCodeGenerator(dispatchers).generate(id) as QrResult.Success).pattern
        val source = RGBLuminanceSource(pattern.size, pattern.size, pattern.pixels)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
        assertEquals(id, decoded.text)
    }

    @Test fun givenBlankTicketIdWhenGeneratingQrThenReturnsFailure() = runBlocking {
        assertEquals(QrResult.Failure, ZxingQrCodeGenerator(dispatchers).generate(""))
    }
}
