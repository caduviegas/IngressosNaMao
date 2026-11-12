package com.innaval.ingresosnamao.feature.payment.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import com.innaval.ingresosnamao.feature.payment.domain.*
import kotlinx.serialization.json.*

internal const val CALLBACK_SCHEME = "ingressosnamao-payment"
internal const val EMULATOR_PACKAGE = "br.com.cielosmart.orderservice"

internal class CieloGateway(private val context: Context) : PaymentGateway {
    override fun launch(attempt: PaymentAttempt, credentials: TestCredentials): StartResult {
        val uri = paymentUri(attempt, credentials)
        val service = Intent(context, PaymentForegroundService::class.java)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service)
            else context.startService(service)
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .setPackage(EMULATOR_PACKAGE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            StartResult.Started
        } catch (_: ActivityNotFoundException) {
            context.stopService(service)
            StartResult.Failure("Aplicativo Cielo não encontrado. A tentativa permanece bloqueada.")
        } catch (_: SecurityException) {
            context.stopService(service)
            StartResult.Failure("Não foi possível iniciar a integração Cielo; tentativa bloqueada.")
        } catch (_: IllegalStateException) {
            context.stopService(service)
            StartResult.Failure("Não foi possível iniciar o serviço Cielo; tentativa bloqueada.")
        }
    }
}

internal fun paymentUri(attempt: PaymentAttempt, credentials: TestCredentials): Uri {
    val request = buildJsonObject {
        put("accessToken", credentials.accessToken)
        put("clientID", credentials.clientId)
        put("reference", attempt.reference)
        put("value", attempt.order.totalCents.toString())
        putJsonArray("items") {
            attempt.order.items.forEach { item ->
                addJsonObject {
                    put("sku", item.sku)
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unitOfMeasure", "unidade")
                    put("unitPrice", item.unitPriceCents)
                }
            }
        }
    }.toString()
    val encoded = Base64.encodeToString(request.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    return Uri.Builder().scheme("lio").authority("payment")
        .appendQueryParameter("request", encoded)
        .appendQueryParameter("urlCallback", "$CALLBACK_SCHEME://response")
        .build()
}

class PaymentForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("payment_test", "Pagamento de teste", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "payment_test")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("Pagamento de teste")
            .setContentText("Aguardando retorno do emulador Cielo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(781, notification)
        handler.postDelayed(timeout, 120_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        super.onDestroy()
    }
}
