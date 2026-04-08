package com.baobao.fatloss.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.baobao.fatloss.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baobao.fatloss.data.local.ApiKeyStore
import com.baobao.fatloss.ui.theme.SansFontFamily
import com.baobao.fatloss.ui.theme.SerifFontFamily
import com.baobao.fatloss.viewmodel.ApiKeySetupViewModel
import com.baobao.fatloss.FitAssistantApp
import com.baobao.fatloss.ui.components.LanguagePickerDialog
import android.app.Activity


private val DarkBg = Color(0xFF111111)
private val DarkCard = Color(0xFF1E1E1E)
private val DarkCardBorder = Color(0xFF333333)
private val DarkInputBg = Color(0xFF2A2A2A)
private val DarkInputBorder = Color(0xFF444444)
private val DarkTextPrimary = Color(0xFFF5F5F5)
private val DarkTextSecondary = Color(0xFF999999)
private val DarkAccent = Color(0xFFFFFFFF)
private val GreenSuccess = Color(0xFF4CAF50)
private val RedError = Color(0xFFEF5350)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiKeySetupScreen(
    viewModel: ApiKeySetupViewModel,
    onSaveSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaveSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val context = LocalContext.current
        val app = context.applicationContext as FitAssistantApp
        val languageStore = app.container.languageStore
        var showLanguageDialog by remember { mutableStateOf(false) }
        val currentLang = remember { languageStore.getLanguage() }

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

        // 语言切换按钮 (右上角)
        IconButton(
            onClick = { showLanguageDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Outlined.Language, contentDescription = "Switch Language", tint = DarkTextSecondary)
        }

        Column(

            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.setup_app_name), fontFamily = SerifFontFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp, color = DarkTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.setup_subtitle), fontFamily = SansFontFamily, fontSize = 15.sp, color = DarkTextSecondary)
            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(stringResource(R.string.setup_api_config), fontFamily = SerifFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = DarkTextPrimary)

                    // API Key
                    DarkInputField(
                        value = state.apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        label = "API Key",
                        placeholder = stringResource(R.string.setup_api_key_placeholder),
                        isPassword = true
                    )

                    // 模型 / 接入点 ID
                    DarkInputField(
                        value = state.selectedModelId,
                        onValueChange = { viewModel.updateModel(it) },
                        label = stringResource(R.string.setup_model_label),
                        placeholder = stringResource(R.string.setup_model_placeholder),
                        isPassword = false
                    )

                    // API 服务地址 (Base URL)
                    DarkInputField(
                        value = state.baseUrl,
                        onValueChange = { viewModel.updateBaseUrl(it) },
                        label = stringResource(R.string.setup_base_url_label),
                        placeholder = stringResource(R.string.setup_base_url_placeholder),
                        isPassword = false
                    )

                    // 预置模型快捷选择
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ApiKeyStore.PRESET_MODELS.forEach { preset ->
                            val isSelected = preset.defaultModelId == state.selectedModelId || (preset.id == "custom" && state.selectedModelId.isBlank() && state.baseUrl.isBlank())
                            val displayName = when (preset.id) {
                                "custom" -> stringResource(R.string.setup_preset_custom)
                                else -> preset.displayName
                            }
                            Surface(
                                modifier = Modifier
                                    .clickable { 
                                        if (preset.id == "custom") {
                                            viewModel.updateModel("")
                                            viewModel.updateBaseUrl("")
                                        } else {
                                            viewModel.updateModel(preset.defaultModelId)
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DarkAccent else DarkInputBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) DarkAccent else DarkInputBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(displayName, fontFamily = SansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) DarkBg else DarkTextPrimary)
                                    if (preset.id != "custom") {
                                        Text(preset.defaultModelId.take(15) + if(preset.defaultModelId.length > 15) "..." else "", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.sp, color = if (isSelected) Color(0xFF888888) else Color(0xFF555555))
                                    }
                                }
                            }
                        }
                    }

                    // 检测按钮
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.testConnection() },
                            enabled = !state.isTesting,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                        ) {
                            if (state.isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkTextPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.setup_testing), color = DarkTextPrimary, fontSize = 14.sp)
                            } else {
                                Text(stringResource(R.string.setup_test_connection), color = DarkTextPrimary, fontSize = 14.sp)
                            }
                        }
                    }

                    // 检测结果
                    AnimatedVisibility(visible = state.testMessage != null) {
                        val icon: ImageVector = if (state.testSuccess == true) Icons.Default.CheckCircle else Icons.Default.Warning
                        val color = if (state.testSuccess == true) GreenSuccess else RedError
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (state.testSuccess == true) Color(0xFF1A2E1A) else Color(0xFF2E1A1A)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                Text(
                                    text = state.testMessage ?: "",
                                    fontFamily = SansFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = color
                                )
                            }
                        }
                    }

                    // 帮助
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleHelp() },
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = if (state.showHelp) stringResource(R.string.setup_hide_help) else stringResource(R.string.setup_how_to_get),
                            fontFamily = SansFontFamily, fontSize = 13.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Medium
                        )
                    }
                    AnimatedVisibility(visible = state.showHelp) { HelpContent() }

                    // 表单错误
                    AnimatedVisibility(visible = state.error != null) {
                        Text(state.error ?: "", color = MaterialTheme.colorScheme.error, fontFamily = SansFontFamily, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 保存按钮
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkAccent)
                    .clickable(enabled = !state.isLoading) { viewModel.save() },
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkBg, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.setup_save_and_start), color = DarkBg, fontFamily = SansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DarkInputField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, isPassword: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontFamily = SansFontFamily, fontSize = 13.sp, color = DarkTextSecondary, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF666666), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = DarkTextSecondary)
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary,
                focusedBorderColor = DarkInputBorder, unfocusedBorderColor = DarkInputBorder,
                focusedContainerColor = DarkInputBg, unfocusedContainerColor = DarkInputBg, cursorColor = DarkAccent
            )
        )
    }
}

@Composable
private fun HelpContent() {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val tutorialUrl = "https://zcntd77twma2.feishu.cn/wiki/U9ABwZqsti54rQkjq7mcqVX7nOh?from=from_copylink"

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = DarkInputBg) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.setup_quick_steps_title), fontFamily = SansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
            Text(
                stringResource(R.string.setup_quick_steps_content),
                fontFamily = SansFontFamily, fontSize = 13.sp, lineHeight = 20.sp, color = DarkTextSecondary
            )

            HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.setup_tutorial_title), fontFamily = SansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCard)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setup_feishu_tutorial),
                        fontFamily = SansFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF4A90E2),
                        modifier = Modifier.weight(1f).clickable { uriHandler.openUri(tutorialUrl) }
                    )
                    
                    Text(
                        text = stringResource(R.string.setup_view),
                        fontSize = 11.sp,
                        color = DarkTextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333333))
                            .clickable { uriHandler.openUri(tutorialUrl) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.setup_copy_link),
                        fontSize = 11.sp,
                        color = DarkTextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333333))
                            .clickable { 
                                clipboardManager.setText(AnnotatedString(tutorialUrl))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
