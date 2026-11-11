package com.innaval.ingresosnamao.feature.payment.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.innaval.ingresosnamao.feature.payment.data.CALLBACK_SCHEME
import com.innaval.ingresosnamao.feature.payment.data.PaymentForegroundService
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import org.koin.android.ext.android.inject

class PaymentCallbackActivity : Activity() {
    private val repository: PaymentRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && uri?.scheme == CALLBACK_SCHEME && uri.host == "response") {
            repository.acceptCallback(uri.getQueryParameter("response"))
        }
        stopService(Intent(this, PaymentForegroundService::class.java))
        finish()
    }
}
