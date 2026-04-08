package com.baobao.fatloss.ui.aichat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.baobao.fatloss.R
import com.baobao.fatloss.ui.theme.*
import com.baobao.fatloss.viewmodel.AiChatUiState
import com.baobao.fatloss.viewmodel.AiChatViewModel
import com.baobao.fatloss.viewmodel.ActionFeedbackData
import java.io.File
import java.io.FileOutputStream

@Composable
fun AiChatScreen(navController: NavHostController, viewModel: AiChatViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── 诊断日志：读取当前 WindowInsets 数值 ──
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeInsets = androidx.compose.foundation.layout.WindowInsets.ime
    val navBarInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars
    val imeBottom = imeInsets.getBottom(density)
    val navBarBottom = navBarInsets.getBottom(density)
    android.util.Log.d("CHAT_IME_DEBUG",
        "IME bottom=${imeBottom}px | NavBar bottom=${navBarBottom}px | modifier=$modifier"
    )

    // 图片选取器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = File(context.cacheDir, "chat_upload_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            viewModel.updateSelectedImage(file.absolutePath)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()   // 正确位置：键盘弹出时整个 Box 收缩，ChatList(weight=1f)自动让位，输入框自然贴键盘
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(FusionBackground, Color(0xFFF8F9FF))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(navController)

            ChatMessagesList(state)

            // 反馈横幅
            AnimatedVisibility(
                visible = state.actionFeedbackItems.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                ActionFeedbackBanner(state.actionFeedbackItems)
            }

            // 自动清除反馈
            LaunchedEffect(state.actionFeedbackItems) {
                if (state.actionFeedbackItems.isNotEmpty()) {
                    kotlinx.coroutines.delay(4000)
                    viewModel.clearActionFeedback()
                }
            }

            InputArea(
                viewModel = viewModel,
                state = state,
                onPickImage = { imagePickerLauncher.launch("image/*") }
            )
        }
    }
}

@Composable
private fun ActionFeedbackBanner(items: List<ActionFeedbackData>) {
    val mealLabelMap = mapOf(
        "breakfast" to stringResource(R.string.meal_type_breakfast),
        "lunch" to stringResource(R.string.meal_type_lunch),
        "dinner" to stringResource(R.string.meal_type_dinner),
        "snack" to stringResource(R.string.meal_type_snack),
    )

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            val text = when (item) {
                is ActionFeedbackData.FoodRecorded -> {
                    val mealLabel = mealLabelMap[item.mealType] ?: item.mealType
                    stringResource(R.string.chat_food_recorded, mealLabel, item.foodNames.joinToString(", "), item.totalCalories)
                }
                is ActionFeedbackData.ExerciseRecorded -> {
                    stringResource(R.string.chat_exercise_recorded, item.exerciseName, item.caloriesBurned)
                }
                is ActionFeedbackData.FoodDeleted -> {
                    stringResource(R.string.chat_food_deleted, item.foodNames.joinToString(", "), item.totalCalories)
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE8F5E9).copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✨", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(navController: NavHostController) {
    // 玻璃拟态 TopBar
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FusionWhite.copy(alpha = 0.7f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }

            Text(
                text = "AI ASSISTANT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            // 占位符保持居中
            Box(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ColumnScope.ChatMessagesList(state: AiChatUiState) {
    val listState = rememberLazyListState()

    // 自动滚动到最新消息 (reverseLayout 下 index 0 为底部)
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (state.isLoading && state.messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = FusionBlack)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            reverseLayout = true
        ) {
            items(state.messages.reversed(), key = { it.id }) { message ->
                // 每条消息进入时的微妙动画
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally { if (message.role == "assistant") -20 else 20 } + fadeIn()
                ) {
                    if (message.role == "assistant") {
                        AiMessageBubble(message.content, formatTimestamp(message.timestamp))
                    } else {
                        UserMessageBubble(message.content, message.imagePath)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(content: String, timestamp: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 40.dp)
            .animateContentSize()
    ) {
        Text(
            text = "${stringResource(R.string.chat_ai_label)} · $timestamp",
            style = MaterialTheme.typography.labelSmall,
            color = FusionAccent,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            color = FusionWhite.copy(alpha = 0.8f),
            shape = RoundedCornerShape(topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
        ) {
            SelectionContainer {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = SerifFontFamily,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            }
        }
    }
}

@Composable
private fun UserMessageBubble(content: String, imagePath: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .animateContentSize(),
            horizontalAlignment = Alignment.End
        ) {
            // 如果有图片，展示图片
            if (imagePath != null) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = "Sent Image",
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, FusionBorder, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (content.isNotBlank()) {
                Surface(
                    color = FusionBlack,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = content,
                            color = FusionWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputArea(viewModel: AiChatViewModel, state: AiChatUiState, onPickImage: () -> Unit) {
    // 玻璃拟态输入框区 — imePadding 已在外层 Box 处理，此处不重复
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionWhite.copy(alpha = 0.9f))
            .border(1.dp, FusionBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 图片预览区域
        AnimatedVisibility(
            visible = state.selectedImagePath != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            state.selectedImagePath?.let { path ->
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    AsyncImage(
                        model = path,
                        contentDescription = "Selected image preview",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, FusionAccent, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { viewModel.updateSelectedImage(null) },
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .offset(8.dp, (-8).dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_delete), tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = onPickImage,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF0F2F8), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.chat_upload_image),
                    tint = FusionBlack
                )
            }

            OutlinedTextField(
                value = state.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                placeholder = { Text(stringResource(R.string.chat_input_hint), color = TextTertiary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FusionAccent,
                    unfocusedBorderColor = FusionBorder,
                    cursorColor = FusionBlack
                ),
                maxLines = 4
            )

            val canSend = (state.inputText.isNotBlank() || state.selectedImagePath != null) && !state.isLoading
            
            // 发送按钮带微动效
            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (canSend) FusionBlack else Color(0xFFE0E0E0), CircleShape)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FusionWhite, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send), tint = if (canSend) FusionWhite else Color.Gray)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
