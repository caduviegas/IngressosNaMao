package com.innaval.ingresosnamao.feature.profile.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.components.IngressosBackScreen
import com.innaval.ingresosnamao.core.ui.components.IngressosButtonStyle
import com.innaval.ingresosnamao.core.ui.components.IngressosProgressIndicator
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileRoute(
    onLoggedOut: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) { if (state.signedOut) onLoggedOut() }
    BackHandler(enabled = state.loggingOut) {}
    ProfileScreen(
        state, viewModel::load, viewModel::requestLogout,
        viewModel::cancelLogout, viewModel::confirmLogout, onBack,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    onBack: () -> Unit,
) {
    IngressosBackScreen("Meu perfil", onBack, backEnabled = state.canNavigateBack()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(IngressosTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.md),
        ) {
            if (state.loading || state.loggingOut) {
                IngressosProgressIndicator(label = if (state.loggingOut) "Encerrando sessão" else "Carregando perfil")
                Text(if (state.loggingOut) "Encerrando sessão…" else "Carregando perfil…")
            }
            state.profile?.let { profile ->
                if (profile.fromCache) Text("Dados salvos no dispositivo; podem estar desatualizados.")
                ProfileField("Nome", profile.name)
                ProfileField("E-mail", profile.email)
                ProfileField("Endereço", profile.address)
            }
            state.error?.let { error ->
                Text(profileErrorMessage(error), color = MaterialTheme.colorScheme.error)
                if (state.profile == null) IngressosButton("Tentar novamente", onRetry)
            }
            if (state.canNavigateBack()) {
                if (state.identity != null) {
                    IngressosButton("Sair da conta", onLogout, style = IngressosButtonStyle.Outlined)
                }
            }
        }
    }
    if (state.confirmingLogout) {
        AlertDialog(
            onDismissRequest = onCancelLogout,
            title = { Text("Sair da conta?") },
            text = { Text("Seu carrinho neste dispositivo será apagado. Seus ingressos serão mantidos.") },
            confirmButton = { TextButton(onClick = onConfirmLogout) { Text("Sair") } },
            dismissButton = { TextButton(onClick = onCancelLogout) { Text("Cancelar") } },
        )
    }
}

private fun ProfileUiState.canNavigateBack() = !loading && !loggingOut && !signedOut

@Composable
private fun ProfileField(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value ?: "Não informado", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun profileErrorMessage(error: ProfileError) = when (error) {
    ProfileError.AUTH -> "Não foi possível verificar sua sessão. Tente novamente."
    ProfileError.READ -> "Não foi possível carregar seu perfil. Tente novamente."
    ProfileError.INVALID_DATA -> "Os dados do perfil são inválidos. Tente novamente."
    ProfileError.CART -> "Não foi possível apagar o carrinho. Você continua na conta; tente novamente."
    ProfileError.SESSION_CHANGED -> "A sessão mudou. A operação foi interrompida; volte e entre novamente."
    ProfileError.SESSION_CHANGED_AFTER_CLEAR ->
        "O carrinho foi apagado, mas a sessão mudou. A outra conta não foi desconectada."
    ProfileError.SIGN_OUT_AFTER_CLEAR -> "O carrinho foi apagado, mas não foi possível sair. Tente novamente."
}
