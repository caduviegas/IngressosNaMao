package com.innaval.ingresosnamao.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.innaval.ingresosnamao.core.ui.R
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme

@Composable
fun IngressosBrandHeader(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(horizontal = IngressosTheme.spacing.md, vertical = IngressosTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.ingressos_logo),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
            Text(stringResource(R.string.ingressos_brand_name), style = MaterialTheme.typography.titleMedium)
        }
    }
}
