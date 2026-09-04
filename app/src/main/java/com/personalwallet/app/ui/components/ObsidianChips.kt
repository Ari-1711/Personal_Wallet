package com.personalwallet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ObsidianChipStyle {
    POSITIVE, PENDING, NEUTRAL
}

@Composable
fun ObsidianStatusChip(
    text: String,
    style: ObsidianChipStyle,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, borderColor) = when (style) {
        ObsidianChipStyle.POSITIVE -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
        ObsidianChipStyle.PENDING -> Triple(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
        )
        ObsidianChipStyle.NEUTRAL -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline
        )
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(9999.dp))
            .border(1.dp, borderColor, RoundedCornerShape(9999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
