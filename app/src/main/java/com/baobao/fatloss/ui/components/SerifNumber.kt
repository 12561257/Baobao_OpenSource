package com.baobao.fatloss.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.baobao.fatloss.ui.theme.SerifFontFamily
import com.baobao.fatloss.ui.theme.TextPrimary

/**
 * Playfair Display 风格的大数字组件。
 * 使用 SerifFontFamily, FontWeight.Bold，可自定义字号。
 */
@Composable
fun SerifNumber(
    number: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 72.sp,
    color: Color = TextPrimary
) {
    Text(
        text = number,
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}
