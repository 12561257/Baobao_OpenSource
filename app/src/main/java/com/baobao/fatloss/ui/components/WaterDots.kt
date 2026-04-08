package com.baobao.fatloss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.baobao.fatloss.ui.theme.FusionProgressBarBg

/**
 * 饮水圆点指示器。
 * 一排小圆点（6dp），filled 用蓝色，empty 用灰色。
 */
@Composable
fun WaterDots(
    filled: Int,
    total: Int = 10,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        repeat(total) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) WaterDotFilled else WaterDotEmpty
                    )
            )
            if (index < total - 1) {
                Box(modifier = Modifier.padding(horizontal = 2.dp))
            }
        }
    }
}

// 饮水圆点颜色
private val WaterDotFilled = Color(0xFF4A9BFF)
private val WaterDotEmpty = FusionProgressBarBg
