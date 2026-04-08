package com.baobao.fatloss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baobao.fatloss.ui.theme.FusionBorder
import com.baobao.fatloss.ui.theme.FusionCard
import com.baobao.fatloss.ui.theme.Dimen
import com.baobao.fatloss.ui.theme.SerifFontFamily
import com.baobao.fatloss.ui.theme.TextPrimary
import com.baobao.fatloss.ui.theme.TextTertiary

/**
 * 数据网格单元格。
 * label 用 labelSmall (TextTertiary)，
 * value 用 SerifFontFamily, 24sp, Bold, TextPrimary，
 * 白色背景圆角卡片，边框 FusionBorder。
 */
@Composable
fun DataCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimen.RadiusMd))
            .background(FusionCard)
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusMd)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )

        Text(
            text = value,
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
