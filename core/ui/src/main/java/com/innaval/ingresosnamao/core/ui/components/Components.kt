package com.innaval.ingresosnamao.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.innaval.ingresosnamao.core.ui.R
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme

enum class IngressosButtonStyle { Primary, Secondary, Outlined, Text }

@Composable
fun IngressosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IngressosButtonStyle = IngressosButtonStyle.Primary,
    loading: Boolean = false,
) {
    val loadingLabel = stringResource(R.string.ingressos_loading)
    val buttonModifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
        .then(if (loading) Modifier.semantics { stateDescription = loadingLabel } else Modifier)
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).clearAndSetSemantics {},
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(IngressosTheme.spacing.sm))
            }
            Text(text)
        }
    }
    when (style) {
        IngressosButtonStyle.Primary -> Button(onClick, buttonModifier, enabled = enabled && !loading) { content() }
        IngressosButtonStyle.Secondary -> FilledTonalButton(onClick, buttonModifier, enabled = enabled && !loading) { content() }
        IngressosButtonStyle.Outlined -> OutlinedButton(onClick, buttonModifier, enabled = enabled && !loading) { content() }
        IngressosButtonStyle.Text -> TextButton(onClick, buttonModifier, enabled = enabled && !loading) { content() }
    }
}

@Composable
fun IngressosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val description = errorMessage ?: supportingText
    val errorModifier = if (errorMessage != null) {
        Modifier.semantics { error(errorMessage) }
    } else Modifier
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(errorModifier),
        enabled = enabled,
        readOnly = readOnly,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = description?.let { message ->
            {
                Text(
                    message,
                    modifier = if (errorMessage != null) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
                )
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
    )
}

@Composable
fun IngressosProgressIndicator(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.ingressos_loading),
) {
    CircularProgressIndicator(modifier = modifier.semantics {
        contentDescription = label
        liveRegion = LiveRegionMode.Polite
    })
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = stringResource(R.string.ingressos_loading)) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm)) {
            IngressosProgressIndicator(label = label)
            Text(label, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    retryLabel: String = stringResource(R.string.ingressos_retry),
) {
    Column(
        modifier.fillMaxSize().padding(IngressosTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (onRetry != null) IngressosButton(text = retryLabel, onClick = onRetry)
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    require((actionLabel == null) == (onAction == null)) { "Action label and callback must be supplied together." }
    Column(
        modifier.fillMaxSize().padding(IngressosTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (actionLabel != null && onAction != null) IngressosButton(actionLabel, onAction, style = IngressosButtonStyle.Outlined)
    }
}
