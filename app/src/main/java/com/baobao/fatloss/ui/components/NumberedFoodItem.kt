package com.baobao.fatloss.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baobao.fatloss.ui.theme.SansFontFamily
import com.baobao.fatloss.ui.theme.SerifFontFamily
import com.baobao.fatloss.ui.theme.TextPrimary
import com.baobao.fatloss.ui.theme.TextSecondary
import com.baobao.fatloss.ui.theme.TextTertiary

/**
 * 编号式食物行。
 * 左侧编号用 SerifFontFamily, 14sp, TextTertiary（如 "01"），
 * 名称用 SansFontFamily, 14sp，
 * 右侧热量用 SerifFontFamily, 14sp, TextPrimary（如 "380 kcal"），
 * weight 用 TextSecondary。
 */
@Composable
fun NumberedFoodItem(
    index: Int,
    name: String,
    weight: String,
    calories: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 编号
        Text(
            text = String.format("%02d", index),
            fontFamily = SerifFontFamily,
            fontSize = 14.sp,
            color = TextTertiary,
            modifier = Modifier.width(28.dp)
        )

        // 名称和重量
        Text(
            text = name,
            fontFamily = SansFontFamily,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = weight,
            fontFamily = SansFontFamily,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(end = 12.dp)
        )

        // 热量
        Text(
            text = calories,
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}
