package com.baobao.fatloss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baobao.fatloss.ui.theme.FusionAccent
import com.baobao.fatloss.ui.theme.FusionCard
import com.baobao.fatloss.ui.theme.FusionBlack
import com.baobao.fatloss.ui.theme.Dimen
import com.baobao.fatloss.ui.theme.SerifFontFamily
import com.baobao.fatloss.ui.theme.TextPrimary

/**
 * AI 引用卡片，左侧黑色竖线。
 * 左侧 3dp 黑色竖线，label 用 CardLabel 样式（FusionAccent 颜色），
 * quote 用 SerifFontFamily italic, 14sp，背景白色，圆角 Dimen.RadiusLg。
 */
@Composable
fun AiQuoteCard(
    quote: String,
    modifier: Modifier = Modifier,
    label: String = "AI INSIGHT"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimen.RadiusLg))
            .background(FusionCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧黑色竖线
            Box(
                modifier = Modifier
                    .width(Dimen.QuoteLineWidth)
                    .height(60.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(FusionBlack)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                // 标签
                CardLabel(
                    text = label,
                    color = FusionAccent
                )

                // 引用文字
                Text(
                    text = quote,
                    fontFamily = SerifFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
