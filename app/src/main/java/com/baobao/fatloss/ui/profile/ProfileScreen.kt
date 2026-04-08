package com.baobao.fatloss.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.baobao.fatloss.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.baobao.fatloss.ui.components.CardLabel
import com.baobao.fatloss.ui.components.FusionInputField
import com.baobao.fatloss.ui.components.GenderChip
import com.baobao.fatloss.navigation.Screen
import com.baobao.fatloss.ui.theme.*
import com.baobao.fatloss.viewmodel.ProfileViewModel
import com.baobao.fatloss.viewmodel.ProfileUiState
import com.baobao.fatloss.FitAssistantApp
import com.baobao.fatloss.ui.components.LanguagePickerDialog
import android.app.Activity
import com.baobao.fatloss.data.local.LanguageStore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 编辑对话框
    if (state.isEditing) {
        EditProfileDialog(viewModel)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FusionBackground)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { PageTitle() }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item { ProfileHero(state) }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item { BodyDataSection(state, viewModel) }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item { AchievementsSection(state) }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item { PersonaSection(state, viewModel) }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item { SettingsSection(viewModel, navController, state) }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ============================================================
// 编辑对话框
// ============================================================

@Composable
private fun EditProfileDialog(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = { viewModel.cancelEditing() },
        title = {
            Text(
                stringResource(R.string.profile_update_health),
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = androidx.compose.foundation.rememberScrollState().let { Modifier },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 姓名
                ProfileInput(
                    value = state.editName,
                    onValueChange = { viewModel.updateEditName(it) },
                    label = stringResource(R.string.profile_nickname),
                    placeholder = stringResource(R.string.profile_your_name)
                )

                // 性别
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.profile_gender), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GenderChip(stringResource(R.string.profile_male), state.editGender == 1, Modifier.weight(1f)) { viewModel.updateEditGender(1) }
                        GenderChip(stringResource(R.string.profile_female), state.editGender == 2, Modifier.weight(1f)) { viewModel.updateEditGender(2) }
                    }
                }

                // 年龄 + 身高
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileInput(
                        value = state.editAge,
                        onValueChange = { viewModel.updateEditAge(it) },
                        label = stringResource(R.string.profile_age),
                        placeholder = stringResource(R.string.profile_age_placeholder),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileInput(
                        value = state.editHeightCm,
                        onValueChange = { viewModel.updateEditHeight(it) },
                        label = stringResource(R.string.profile_height),
                        placeholder = "cm",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 体重
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileInput(
                        value = state.editCurrentWeight,
                        onValueChange = { viewModel.updateEditWeight(it) },
                        label = stringResource(R.string.profile_current_weight),
                        placeholder = "kg",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileInput(
                        value = state.editTargetWeight,
                        onValueChange = { viewModel.updateEditTarget(it) },
                        label = stringResource(R.string.profile_target_weight),
                        placeholder = "kg",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { viewModel.saveProfile() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FusionBlack)
            ) {
                Text(stringResource(R.string.profile_save_update))
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelEditing() }) {
                Text(stringResource(R.string.profile_skip_edit), color = TextTertiary)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = FusionWhite
    )
}

@Composable
private fun ProfileInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    FusionInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        keyboardType = keyboardType,
        modifier = modifier
    )
}

@Composable
private fun PageTitle() {
    Text(
        text = stringResource(R.string.profile_title),
        style = MaterialTheme.typography.displayMedium
    )
}

@Composable
private fun ProfileHero(state: ProfileUiState) {
    val profile = state.profile
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(FusionBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile?.name?.take(1) ?: "?",
                color = FusionWhite,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile?.name ?: "",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.profile_goal_weight, profile?.targetWeight?.toInt() ?: 0),
            style = MaterialTheme.typography.titleSmall,
            color = FusionAccent,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun BodyDataSection(
    state: ProfileUiState,
    viewModel: ProfileViewModel
) {
    val profile = state.profile

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusLg)
            )
            .clickable { viewModel.startEditing() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            CardLabel(stringResource(R.string.profile_body_data))

            Spacer(modifier = Modifier.height(12.dp))

            DataLine(stringResource(R.string.profile_height), "${profile?.heightCm ?: 0} cm", showDivider = true)
            DataLine(stringResource(R.string.profile_gender), if (profile?.gender == 2) "女" else "男", showDivider = true)
            DataLine(stringResource(R.string.profile_current_weight), "${profile?.currentWeight ?: 0.0} kg", showDivider = true)
            DataLine(stringResource(R.string.profile_target_weight), "${profile?.targetWeight ?: 0.0} kg", showDivider = true)
            DataLine(stringResource(R.string.profile_bmi_label), String.format("%.1f", state.bmi), showDivider = true)
            DataLine(stringResource(R.string.profile_activity_label), activityLevelText(profile?.activityLevel ?: 2), showDivider = true)
            DataLine(stringResource(R.string.profile_daily_budget_label), "${String.format("%,d", state.dailyBudget.toInt())} kcal", showDivider = false)
        }
    }
}

@Composable
private fun activityLevelText(level: Int): String = when (level) {
    1 -> stringResource(R.string.profile_sedentary)
    2 -> stringResource(R.string.profile_light_activity)
    3 -> stringResource(R.string.profile_moderate_activity)
    4 -> stringResource(R.string.profile_heavy_activity)
    else -> stringResource(R.string.profile_light_activity)
}

@Composable
private fun DataLine(label: String, value: String, showDivider: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall
        )
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = FusionDivider,
            thickness = 1.dp
        )
    }
}

@Composable
private fun AchievementsSection(state: ProfileUiState) {
    CardLabel(stringResource(R.string.profile_achievements))

    Spacer(modifier = Modifier.height(12.dp))

    // 从 state 数据动态计算成就
    val achievements = buildAchievements(state)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        achievements.forEach { (emoji, title, subtitle) ->
            AchievementCard(emoji, title, subtitle)
        }
    }
}

@Composable
private fun buildAchievements(state: ProfileUiState): List<Triple<String, String, String>> {
    val list = mutableListOf<Triple<String, String, String>>()

    // 连续记录天数
    val streak = state.daysStreak
    list.add(Triple(
        "\uD83D\uDD25",
        stringResource(R.string.profile_streak_days, streak),
        stringResource(R.string.profile_consecutive_record)
    ))

    // 累计减重
    val lost = state.totalWeightLost
    list.add(Triple(
        "\uD83C\uDFAF",
        String.format("%.1f", lost) + "kg",
        stringResource(R.string.profile_total_weight_lost)
    ))

    // BMI 正常
    val bmi = state.bmi
    list.add(Triple(
        "\uD83D\uDCAA",
        if (bmi in 18.5..24.9) "正常" else String.format("%.1f", bmi),
        stringResource(R.string.profile_bmi_label)
    ))

    return list
}

@Composable
private fun RowScope.AchievementCard(emoji: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusLg)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// ============================================================
// 人设（灵魂）区域
// ============================================================

@Composable
private fun rememberPersonaPresets(): List<Pair<String, String>> {
    return listOf(
        Pair(stringResource(R.string.persona_warm), "温柔贴心、像朋友一样包容的私人减脂教练。"),
        Pair(stringResource(R.string.persona_sarcastic), "毒舌且极其严格的魔鬼减脂教练，遇到违规绝不留情，一针见血地批评。"),
        Pair(stringResource(R.string.persona_professional), "绝对理性的临床营养师，不闲聊，语言极简，只给出绝对客观的数据分析和硬核建议。"),
        Pair(stringResource(R.string.persona_custom), "")
    )
}

@Composable
private fun PersonaSection(state: ProfileUiState, viewModel: ProfileViewModel) {
    var showDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val personaPresets = rememberPersonaPresets()

    // 获取当前真实文本，如果数据库为空（旧用户的默认状态），取学姐的预设词
    val rawPersona = state.profile?.aiPersona ?: ""
    val currentPersonaStr = if (rawPersona.isBlank()) personaPresets[0].second else rawPersona

    // 根据文本反推当前标题
    val displayPersonaTitle = personaPresets.find { it.second == currentPersonaStr }?.first ?: stringResource(R.string.profile_custom_setting)

    if (showDialog) {
        var customText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentPersonaStr) }
        
        // 根据当前的 customText 反推匹配的预设标题
        val matchedTitle = personaPresets.find { it.second == customText }?.first
            ?: if (customText.isBlank()) stringResource(R.string.persona_custom) else stringResource(R.string.profile_custom_setting)

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    stringResource(R.string.profile_reshape_persona),
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.profile_recommend_templates), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        personaPresets.forEach { (title, prompt) ->
                            val customTitle = stringResource(R.string.persona_custom)
                            val isSelected = (matchedTitle == title) || (matchedTitle == stringResource(R.string.profile_custom_setting) && title == customTitle)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customText = prompt },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) FusionBlack else Color.Transparent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (isSelected) FusionWhite else TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) FusionWhite else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Text(stringResource(R.string.profile_advanced_custom), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        placeholder = { Text(stringResource(R.string.profile_assistant_hint), color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FusionBlack,
                            unfocusedBorderColor = FusionBorder
                        )
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.updateAiPersona(customText)
                        showDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FusionBlack)
                ) { Text(stringResource(R.string.profile_confirm_modify)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.common_cancel), color = TextTertiary) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = FusionWhite
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusLg)
            )
            .clickable { showDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardLabel(stringResource(R.string.profile_ai_assistant_setting))
                Box(
                    modifier = Modifier
                        .background(FusionBlack, RoundedCornerShape(Dimen.RadiusSm))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(displayPersonaTitle, color = FusionWhite, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentPersonaStr,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun SettingsSection(viewModel: ProfileViewModel, navController: NavHostController, state: ProfileUiState) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(1.dp, FusionBorder, RoundedCornerShape(Dimen.RadiusLg))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            CardLabel(stringResource(R.string.profile_settings))

            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(Icons.Filled.TrackChanges, stringResource(R.string.profile_goal_setting), showDivider = true, context = context) {
                viewModel.startEditing()
            }
            SettingsItem(Icons.Filled.VpnKey, stringResource(R.string.profile_api_config), subtitle = state.maskedApiKey.ifBlank { "未配置" }, showDivider = true, context = context) {
                navController.navigate(Screen.ApiKeySetup.route)
            }
            SettingsItem(Icons.Filled.Spa, stringResource(R.string.profile_dietary_prefs), showDivider = true, context = context)
            SettingsItem(Icons.Filled.Notifications, stringResource(R.string.profile_notification), showDivider = true, context = context)
            SettingsItem(Icons.Filled.Download, stringResource(R.string.profile_data_export), showDivider = true, context = context)
            SettingsItem(Icons.Filled.Settings, stringResource(R.string.profile_memory_manage), showDivider = true, context = context) {
                navController.navigate(Screen.MemoryManagement.route)
            }
            SettingsItem(Icons.Filled.Info, stringResource(R.string.profile_about), showDivider = true, context = context) {
                Toast.makeText(context, context.getString(R.string.profile_app_version), Toast.LENGTH_SHORT).show()
            }

            // 语言设置
            val app = context.applicationContext as FitAssistantApp
            val languageStore = app.container.languageStore
            var showLanguageDialog by remember { mutableStateOf(false) }
            val currentLang = remember { languageStore.getLanguage() }
            val currentLangDisplay = when (currentLang) {
                LanguageStore.LANG_ZH -> stringResource(R.string.settings_language_zh)
                LanguageStore.LANG_EN -> stringResource(R.string.settings_language_en)
                else -> stringResource(R.string.settings_language_system)
            }

            if (showLanguageDialog) {
                LanguagePickerDialog(
                    currentLanguage = currentLang,
                    onDismiss = { showLanguageDialog = false },
                    onLanguageSelected = { lang ->
                        languageStore.saveLanguage(lang)
                        showLanguageDialog = false
                        (context as? Activity)?.recreate()
                    }
                )
            }

            SettingsItem(
                icon = Icons.Filled.Language,
                label = stringResource(R.string.settings_language),
                subtitle = currentLangDisplay,
                showDivider = true,
                context = context
            ) {
                showLanguageDialog = true
            }


            // 重置账户区域
            var showResetDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text(stringResource(R.string.profile_reset_title), color = Color(0xFFD32F2F)) },
                    text = { Text(stringResource(R.string.profile_reset_desc)) },
                    confirmButton = {
                        TextButton(onClick = { 
                            viewModel.resetProfile()
                            showResetDialog = false
                        }) {
                            Text(stringResource(R.string.profile_reset_confirm), color = Color(0xFFD32F2F))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
            SettingsItem(
                icon = Icons.Filled.DeleteForever,
                label = stringResource(R.string.profile_reset_all),
                showDivider = false,
                context = context,
                tint = Color(0xFFD32F2F)
            ) {
                showResetDialog = true
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String = "",
    showDivider: Boolean,
    context: android.content.Context,
    tint: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onClick != null) {
                    onClick()
                } else {
                    Toast.makeText(context, context.getString(R.string.profile_feature_wip, label), Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "→", tint = TextTertiary)
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = FusionDivider,
            thickness = 1.dp
        )
    }
}


