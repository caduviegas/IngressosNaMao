package com.innaval.ingresosnamao.feature.ticket.domain

data class QrPattern(val size: Int, val pixels: IntArray)

sealed interface QrResult {
    data class Success(val pattern: QrPattern) : QrResult
    data object Failure : QrResult
}

interface QrCodeGenerator {
    suspend fun generate(ticketId: String): QrResult
}
