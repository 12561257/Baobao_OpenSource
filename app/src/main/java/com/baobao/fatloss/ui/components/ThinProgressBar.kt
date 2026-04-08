package com.baobao.fatloss.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.baobao.fatloss.ui.theme.FusionBlack
import com.baobao.fatloss.ui.theme.FusionProgressBarBg
import com.baobao.fatloss.ui.theme.Dimen

/**
 * 带有平滑动画的 3px 细线进度条。
 */
@Composable
fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = FusionBlack,
    backgroundColor: Color = FusionProgressBarBg
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    
    // 增加动效：平滑过渡到目标进度
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "ProgressBarAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimen.ProgressBarHeight)
            .clip(RoundedCornerShape(Dimen.RadiusSm))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(Dimen.ProgressBarHeight)
                .clip(RoundedCornerShape(Dimen.RadiusSm))
                .background(color)
        )
    }
}
