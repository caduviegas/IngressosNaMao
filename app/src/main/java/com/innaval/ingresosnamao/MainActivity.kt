package com.innaval.ingresosnamao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.innaval.ingresosnamao.core.ui.components.IngressosBrandHeader
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IngressosTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { IngressosBrandHeader(Modifier.statusBarsPadding()) },
                ) { padding ->
                    Box(Modifier.padding(padding).consumeWindowInsets(padding)) { AppNavHost() }
                }
            }
        }
    }
}
