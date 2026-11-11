package com.innaval.ingresosnamao.feature.ticket.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.feature.ticket.domain.QrCodeGenerator
import com.innaval.ingresosnamao.feature.ticket.domain.QrPattern
import com.innaval.ingresosnamao.feature.ticket.domain.QrResult
import kotlinx.coroutines.withContext

internal class ZxingQrCodeGenerator(private val dispatchers: DispatcherProvider) : QrCodeGenerator {
    override suspend fun generate(ticketId: String): QrResult = withContext(dispatchers.default) {
        if (ticketId.isBlank()) return@withContext QrResult.Failure
        try {
            val matrix = MultiFormatWriter().encode(
                ticketId, BarcodeFormat.QR_CODE, 512, 512,
                mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.CHARACTER_SET to "UTF-8"),
            )
            val pixels = IntArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) 0xff000000.toInt() else 0xffffffff.toInt()
            }
            QrResult.Success(QrPattern(matrix.width, pixels))
        } catch (_: WriterException) {
            QrResult.Failure
        } catch (_: IllegalArgumentException) {
            QrResult.Failure
        }
    }
}
