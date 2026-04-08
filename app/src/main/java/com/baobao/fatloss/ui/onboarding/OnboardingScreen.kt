package com.baobao.fatloss.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baobao.fatloss.R
import com.baobao.fatloss.ui.components.FusionInputField
import com.baobao.fatloss.ui.components.GenderChip
import com.baobao.fatloss.ui.theme.*
import androidx.compose.ui.res.stringResource

@Composable
fun OnboardingScreen(
    onSave: (name: String, heightCm: Int, weight: Double, targetWeight: Double, age: Int, gender: Int) -> Unit
) {
    OnboardingForm(onSave = onSave)
}

@Composable
private fun OnboardingForm(
    onSave: (name: String, heightCm: Int, weight: Double, targetWeight: Double, age: Int, gender: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(1) }
    var validationError by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val defaultName = stringResource(R.string.onboarding_default_name)

    fun validate(): Boolean {
        val h = heightCm.toIntOrNull()
        val w = weight.toDoubleOrNull()
        val t = targetWeight.toDoubleOrNull()
        val a = age.toIntOrNull()
        return when {
            h == null || h < 100 || h > 250 -> { validationError = R.string.onboarding_validation_height; false }
            w == null || w < 30 || w > 300   -> { validationError = R.string.onboarding_validation_weight; false }
            t == null || t < 30 || t > 300   -> { validationError = R.string.onboarding_validation_target; false }
            a == null || a < 10 || a > 120   -> { validationError = R.string.onboarding_validation_age; false }
            else -> { validationError = null; true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8F9FF), FusionBackground)))
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── 顶部标题区 ──
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_hello),
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_intro),
                fontFamily = SansFontFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 表单卡片 ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = FusionCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 姓名
                    FusionInputField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(R.string.onboarding_name_label),
                        placeholder = stringResource(R.string.onboarding_name_placeholder)
                    )

                    // 性别
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.onboarding_gender), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GenderChip(stringResource(R.string.onboarding_gender_male), gender == 1, Modifier.weight(1f)) { gender = 1 }
                            GenderChip(stringResource(R.string.onboarding_gender_female), gender == 2, Modifier.weight(1f)) { gender = 2 }
                        }
                    }

                    // 年龄 + 身高
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FusionInputField(
                            value = age,
                            onValueChange = { age = it },
                            label = stringResource(R.string.onboarding_age),
                            placeholder = stringResource(R.string.onboarding_age_placeholder),
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        FusionInputField(
                            value = heightCm,
                            onValueChange = { heightCm = it },
                            label = stringResource(R.string.onboarding_height),
                            placeholder = "cm",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 当前体重 + 目标体重
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FusionInputField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = stringResource(R.string.onboarding_current_weight),
                            placeholder = "kg",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FusionInputField(
                            value = targetWeight,
                            onValueChange = { targetWeight = it },
                            label = stringResource(R.string.onboarding_target_weight),
                            placeholder = "kg",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 错误提示
                    AnimatedVisibility(visible = validationError != null) {
                        Text(
                            text = "⚠️ ${validationError?.let { stringResource(it) } ?: ""}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 开始按钮 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FusionBlack)
                    .clickable {
                        if (validate()) {
                            onSave(
                                name.ifEmpty { defaultName },
                                heightCm.toInt(),
                                weight.toDouble(),
                                targetWeight.toDouble(),
                                age.toInt(),
                                gender
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.onboarding_start),
                    color = FusionWhite,
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
