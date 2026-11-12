package com.innaval.ingresosnamao.feature.auth.presentation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.components.IngressosTextField
import com.innaval.ingresosnamao.core.ui.components.IngressosProgressIndicator
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.auth.domain.AuthError
import com.innaval.ingresosnamao.feature.auth.domain.AuthResult
import com.innaval.ingresosnamao.feature.auth.domain.Field
import kotlinx.coroutines.CancellationException
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(onLoggedIn: (userId: String) -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.stage) {
        if (state.stage == AuthStage.AUTHENTICATED) state.user?.id?.let(onLoggedIn)
    }
    AuthScreen(
        state = state,
        onName = viewModel::changeName,
        onEmail = viewModel::changeEmail,
        onAddress = viewModel::changeAddress,
        onPassword = viewModel::changePassword,
        onLogin = viewModel::submitLogin,
        onRegister = viewModel::submitRegistration,
        onGoogle = { viewModel.submitGoogle { googleToken(context) } },
        onCompleteProfile = viewModel::submitProfile,
        onRetry = viewModel::retryProfile,
        onLeave = viewModel::leaveAccount,
        showRegister = viewModel::showRegister,
        showLogin = viewModel::showLogin,
    )
}

private suspend fun googleToken(context: Context): AuthResult<String> {
    val resource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resource == 0) return AuthResult.Failure(AuthError.CONFIGURATION)
    val clientId = context.getString(resource)
    if (clientId.isBlank()) return AuthResult.Failure(AuthError.CONFIGURATION)
    return try {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            AuthResult.Failure(AuthError.GOOGLE_FAILED)
        } else {
            AuthResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: GetCredentialCancellationException) {
        AuthResult.Failure(AuthError.GOOGLE_CANCELLED)
    } catch (e: GetCredentialException) {
        AuthResult.Failure(AuthError.GOOGLE_FAILED)
    } catch (e: GoogleIdTokenParsingException) {
        AuthResult.Failure(AuthError.GOOGLE_FAILED)
    }
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onAddress: (String) -> Unit,
    onPassword: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onGoogle: () -> Unit,
    onCompleteProfile: () -> Unit,
    onRetry: () -> Unit,
    onLeave: () -> Unit,
    showRegister: () -> Unit,
    showLogin: () -> Unit,
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(IngressosTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.md),
    ) {
        val enabled = !state.loading
        Text(
            when (state.stage) {
                AuthStage.LOGIN -> "Entrar"
                AuthStage.REGISTER -> "Criar conta"
                AuthStage.CHECK_PROFILE -> "Verificando perfil"
                AuthStage.COMPLETE_PROFILE -> "Completar cadastro"
                AuthStage.AUTHENTICATED -> "Autenticado"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        if (state.stage == AuthStage.REGISTER || state.stage == AuthStage.COMPLETE_PROFILE) {
            AuthField("Nome", state.name, onName, Field.NAME in state.invalidFields, enabled)
        }
        if (state.stage == AuthStage.LOGIN || state.stage == AuthStage.REGISTER || state.stage == AuthStage.COMPLETE_PROFILE) {
            AuthField(
                "E-mail", state.email, onEmail, Field.EMAIL in state.invalidFields,
                enabled && state.stage != AuthStage.COMPLETE_PROFILE, KeyboardType.Email
            )
        }
        if (state.stage == AuthStage.REGISTER || state.stage == AuthStage.COMPLETE_PROFILE) {
            AuthField("Endereço", state.address, onAddress, Field.ADDRESS in state.invalidFields, enabled)
        }
        if (state.stage == AuthStage.LOGIN || state.stage == AuthStage.REGISTER) {
            IngressosTextField(
                value = state.password,
                onValueChange = onPassword,
                label = "Senha",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                errorMessage = if (Field.PASSWORD in state.invalidFields)
                    if (state.stage == AuthStage.REGISTER) "Use de 6 a 128 caracteres." else "Informe a senha."
                else null,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
        if (state.loading) IngressosProgressIndicator()
        when (state.stage) {
            AuthStage.LOGIN -> {
                IngressosButton("Entrar", onLogin, enabled = enabled)
                IngressosButton("Entrar com Google", onGoogle, enabled = enabled)
                TextButton(onClick = showRegister, enabled = enabled) { Text("Criar conta") }
            }
            AuthStage.REGISTER -> {
                IngressosButton("Cadastrar", onRegister, enabled = enabled)
                TextButton(onClick = showLogin, enabled = enabled) { Text("Já tenho conta") }
            }
            AuthStage.CHECK_PROFILE -> {
                if (!state.loading) IngressosButton("Tentar novamente", onRetry)
                TextButton(onClick = onLeave, enabled = enabled) { Text("Sair da conta") }
            }
            AuthStage.COMPLETE_PROFILE -> {
                IngressosButton("Salvar cadastro", onCompleteProfile, enabled = enabled)
                TextButton(onClick = onLeave, enabled = enabled) { Text("Sair da conta") }
            }
            AuthStage.AUTHENTICATED -> Unit
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    invalid: Boolean,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    IngressosTextField(
        value = value, onValueChange = onChange, label = label,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        errorMessage = if (invalid) {
            when (label) {
                "Nome" -> "Informe um nome com até 100 caracteres."
                "Endereço" -> "Informe um endereço com até 300 caracteres."
                else -> "Informe um e-mail válido com até 254 caracteres."
            }
        } else null,
        enabled = enabled, modifier = Modifier.fillMaxWidth(),
    )
}

private fun errorMessage(error: AuthError): String = logoutErrorMessage(error) ?: when (error) {
    AuthError.INVALID_CREDENTIALS -> "E-mail ou senha inválidos."
    AuthError.EMAIL_IN_USE -> "Este e-mail já está cadastrado. Entre com sua conta."
    AuthError.WEAK_PASSWORD -> "A senha não atende à política de segurança."
    AuthError.USER_DISABLED -> "Esta conta está desativada."
    AuthError.TOO_MANY_REQUESTS -> "Muitas tentativas. Tente novamente mais tarde."
    AuthError.NETWORK -> "Sem conexão. Verifique sua rede e tente novamente."
    AuthError.GOOGLE_CANCELLED -> "Seleção de conta Google cancelada."
    AuthError.GOOGLE_FAILED -> "Não foi possível entrar com Google. Tente novamente."
    AuthError.CONFIGURATION -> "Firebase ou Google não configurado. Contate o suporte."
    AuthError.PROFILE_READ -> "Não foi possível verificar seu cadastro. Tente novamente."
    AuthError.PROFILE_WRITE -> "Não foi possível salvar seu cadastro. Tente novamente; se sair, entre para completá-lo."
    else -> "Não foi possível autenticar. Tente novamente."
}

private fun logoutErrorMessage(error: AuthError): String? = when (error) {
    AuthError.LOGOUT_CART -> "Não foi possível apagar o carrinho. Você continua na conta; tente novamente."
    AuthError.LOGOUT_SESSION_CHANGED -> "A sessão mudou. A operação foi interrompida."
    AuthError.LOGOUT_SESSION_CHANGED_AFTER_CLEAR ->
        "O carrinho foi apagado, mas a sessão mudou; a outra conta foi mantida."
    AuthError.LOGOUT_AFTER_CLEAR -> "O carrinho foi apagado, mas não foi possível sair. Tente novamente."
    else -> null
}
