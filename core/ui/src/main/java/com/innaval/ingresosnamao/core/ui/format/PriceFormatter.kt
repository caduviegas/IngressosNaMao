package com.innaval.ingresosnamao.core.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatPriceBrl(priceCents: Long): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
        currency = Currency.getInstance("BRL")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(priceCents, 2))
        .replace('\u00a0', ' ')
        .replace('\u202f', ' ')
