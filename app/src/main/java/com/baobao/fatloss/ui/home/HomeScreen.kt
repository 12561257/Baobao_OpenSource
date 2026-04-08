package com.baobao.fatloss.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.baobao.fatloss.navigation.Screen
import com.baobao.fatloss.ui.components.AiQuoteCard
import com.baobao.fatloss.ui.components.CardLabel
import com.baobao.fatloss.ui.components.SerifNumber
import com.baobao.fatloss.ui.components.ThinProgressBar
import com.baobao.fatloss.ui.components.WaterDots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.baobao.fatloss.ui.onboarding.OnboardingScreen
import com.baobao.fatloss.viewmodel.HomeViewModel
import com.baobao.fatloss.viewmodel.HomeUiState
import androidx.compose.ui.res.stringResource
import com.baobao.fatloss.R
import com.baobao.fatloss.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    // 首次引导
    if (!state.hasProfile && !state.isLoading) {
        OnboardingScreen(onSave = { name, heightCm, weight, targetWeight, age, gender ->
            viewModel.saveInitialProfile(name, heightCm, weight, targetWeight, age, gender = gender)
        })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(FusionBackground, Color(0xFFF0F2F8))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ---- 1. 顶部问候区（入场动画） ----
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(600))
            ) {
                GreetingHeader(state.userName)
            }

            // ---- 2. 可滚动内容 ----
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = Dimen.ScreenPadding,
                    vertical = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimen.SectionGap)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 错位入场动画函数
                fun staggeredEnter(delay: Int) = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = delay)) +
                        slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(durationMillis = 600, delayMillis = delay))

                // a. 热量大卡片
                item {
                    AnimatedVisibility(visible = visible, enter = staggeredEnter(100)) {
                        CaloriesCard(state)
                    }
                }

                // b. 三大营养素并排
                item {
                    AnimatedVisibility(visible = visible, enter = staggeredEnter(200)) {
                        MacroRow(state)
                    }
                }

                // c. 步数 + 饮水并排
                item {
                    AnimatedVisibility(visible = visible, enter = staggeredEnter(300)) {
                        StepsWaterRow(state, viewModel)
                    }
                }

                // d. 本周进度横幅
                item {
                    AnimatedVisibility(visible = visible, enter = staggeredEnter(400)) {
                        WeeklyDigestBanner(state)
                    }
                }

                // e. AI 洞察卡片
                item {
                    AnimatedVisibility(visible = visible, enter = staggeredEnter(500)) {
                        AiQuoteCard(
                            quote = state.aiInsight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // f. 快捷操作
                item {
                    AnimatedVisibility(visible = visible, enter = staggeredEnter(600)) {
                        QuickActionsRow(navController = navController)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}


// ============================================================
// 1. 顶部问候区
// ============================================================

@Composable
private fun GreetingHeader(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionBackground)
            .padding(horizontal = Dimen.ScreenPadding, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 超大衬线标题
        Text(
            text = stringResource(R.string.home_greeting, userName),
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimary
        )
        // 日期标签
        Text(
            text = currentDateLabel(),
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.8.sp,
            color = TextTertiary
        )
    }
}

/**
 * 生成当前日期标签，格式 "THU / APR 2, 2026"
 */
private fun currentDateLabel(): String {
    val sdf = SimpleDateFormat("EEE / MMM d, yyyy", Locale.US)
    return sdf.format(Date()).uppercase(Locale.US)
}

// ============================================================
// a. 热量大卡片
// ============================================================

@Composable
private fun CaloriesCard(state: HomeUiState) {
    FusionCardFrame(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimen.CardPadding)) {
            CardLabel(text = stringResource(R.string.home_today_calories))

            Spacer(modifier = Modifier.height(12.dp))

            // 大数字 + "大卡可用"
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                val remaining = state.remainingCalories.toInt()
                SerifNumber(
                    number = "$remaining",
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_calories_available),
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 摄入 / 目标
            Text(
                text = "${state.consumedCalories.toInt().let { String.format("%,d", it) }} / ${state.dailyBudget.toInt().let { String.format("%,d", it) }} kcal",
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            ThinProgressBar(
                progress = state.progressPct
            )
        }
    }
}

// ============================================================
// b. 三大营养素并排
// ============================================================

@Composable
private fun MacroRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimen.GridGap)
    ) {
        MacroItem(
            label = stringResource(R.string.home_carbs),
            consumed = state.carbsConsumed.toInt(),
            goal = state.carbsTarget.toInt(),
            unit = "g",
            progress = if (state.carbsTarget > 0) (state.carbsConsumed / state.carbsTarget).toFloat() else 0f,
            modifier = Modifier.weight(1f)
        )
        MacroItem(
            label = stringResource(R.string.home_protein),
            consumed = state.proteinConsumed.toInt(),
            goal = state.proteinTarget.toInt(),
            unit = "g",
            progress = if (state.proteinTarget > 0) (state.proteinConsumed / state.proteinTarget).toFloat() else 0f,
            modifier = Modifier.weight(1f)
        )
        MacroItem(
            label = stringResource(R.string.home_fat),
            consumed = state.fatConsumed.toInt(),
            goal = state.fatTarget.toInt(),
            unit = "g",
            progress = if (state.fatTarget > 0) (state.fatConsumed / state.fatTarget).toFloat() else 0f,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroItem(
    label: String,
    consumed: Int,
    goal: Int,
    unit: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    FusionCardFrame(modifier = modifier) {
        Column(modifier = Modifier.padding(Dimen.CardPadding)) {
            CardLabel(text = label)

            Spacer(modifier = Modifier.height(8.dp))

            // 数值: 120 / 200g
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = SerifFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)) {
                        append("$consumed")
                    }
                    withStyle(SpanStyle(fontFamily = SansFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TextTertiary)) {
                        append(" / $goal$unit")
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ThinProgressBar(progress = progress)
        }
    }
}

// ============================================================
// c. 步数 + 饮水并排
// ============================================================

@Composable
private fun StepsWaterRow(state: HomeUiState, viewModel: HomeViewModel) {
    // 饮水卡片 — 全宽
    FusionCardFrame(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimen.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardLabel(text = stringResource(R.string.home_water))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.subtractWater() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.home_water_remove),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.addWater() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.home_water_add),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${state.waterMl}ml / ${state.waterTarget}ml",
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            WaterDots(
                filled = if (state.waterTarget > 0) (state.waterMl * 10 / state.waterTarget).coerceAtMost(10) else 0,
                total = 10
            )
        }
    }
}

// ============================================================
// d. 本周进度横幅
// ============================================================

@Composable
private fun WeeklyDigestBanner(state: HomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimen.RadiusLg))
            .background(FusionWeeklyBg)
    ) {
        Column(modifier = Modifier.padding(Dimen.CardPadding)) {
            CardLabel(
                text = stringResource(R.string.home_weekly_overview),
                color = FusionWhite
            )

            Spacer(modifier = Modifier.height(12.dp))

            SerifNumber(
                number = "${state.weeklyWeightChange} kg",
                fontSize = 36.sp,
                color = FusionWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_weekly_summary, String.format("%,d", state.weeklyAvgCalories.toInt()), state.weeklyAdherenceRate),
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFCCCCCC)
            )
        }
    }
}

// ============================================================
// f. 快捷操作
// ============================================================

@Composable
private fun QuickActionsRow(navController: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimen.GridGap)
    ) {
        QuickActionButton(
            text = stringResource(R.string.home_ask_ai),
            icon = Icons.AutoMirrored.Filled.Chat,
            onClick = {
                navController.navigate(Screen.AiChat.route) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Dimen.RadiusMd))
            .background(FusionBlack)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = FusionWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = FusionWhite
            )
        }
    }
}

// ============================================================
// 通用卡片容器
// ============================================================

/**
 * 白底圆角边框卡片容器。
 * 背景白色，圆角 Dimen.RadiusLg(16dp)，边框 1dp FusionBorder。
 */
@Composable
private fun FusionCardFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimen.RadiusLg))
            .background(FusionCard)
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusLg)
            )
    ) {
        content()
    }
}


