package com.baobao.fatloss.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.baobao.fatloss.ui.theme.TextTertiary

/**
 * 大写标签组件，用于 "TODAY'S CALORIES"、"BREAKFAST"、"WEEKLY DIGEST" 等。
 * 使用 labelSmall (10sp, SemiBold, 1.8sp letter-spacing)，自动转大写。
 */
@Composable
fun CardLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextTertiary
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}
