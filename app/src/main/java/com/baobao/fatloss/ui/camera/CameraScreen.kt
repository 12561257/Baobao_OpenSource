package com.baobao.fatloss.ui.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.baobao.fatloss.navigation.Screen
import com.baobao.fatloss.ui.components.AiQuoteCard
import com.baobao.fatloss.R
import com.baobao.fatloss.ui.theme.*
import com.baobao.fatloss.viewmodel.CameraViewModel
import com.baobao.fatloss.viewmodel.CameraUiState
import java.io.ByteArrayOutputStream
import androidx.compose.ui.res.stringResource

@Composable
fun CameraScreen(navController: NavHostController, viewModel: CameraViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 确认后自动跳转
    LaunchedEffect(state.isConfirmed) {
        if (state.isConfirmed) {
            navController.navigate(Screen.MealLog.route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
            }
            viewModel.reset()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // 持久化拍照 URI（防止系统杀进程后丢失）
    var photoUriString by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri = photoUriString?.let { android.net.Uri.parse(it) }

    // 拍照 launcher — 使用 TakePicture (文件URI方式)
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            photoUri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val base64 = uriToBase64(context, uri)
                        viewModel.setImageBase64(base64)
                    } catch (e: Exception) {
                        android.util.Log.e("CameraScreen", "图片解码失败", e)
                    }
                }
            }
        }
    }

    // 准备一个 "即将拍照" 的辅助函数
    val prepareAndLaunchCamera: () -> Unit = {
        val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        photoUriString = uri.toString()
        takePictureLauncher.launch(uri)
    }

    // 运行时权限申请 launcher（授权成功后自动启动拍照）
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            prepareAndLaunchCamera()
        } else {
            android.widget.Toast.makeText(context, context.getString(R.string.camera_need_permission), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 相册 launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val base64 = uriToBase64(context, it)
                    viewModel.setImageBase64(base64)
                } catch (e: Exception) {
                    android.util.Log.e("CameraScreen", "图片解码失败", e)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FusionBackground)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { PageTitle() }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { MealTypeSelector(state) { viewModel.selectMealType(it) } }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 拍照 / 相册按钮
        item {
            ImageSourceButtons(
                onTakePhoto = {
                    // 先检查权限，没有就弹窗申请
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        prepareAndLaunchCamera()
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onPickImage = {
                    pickImageLauncher.launch("image/*")
                }
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 图片预览
        if (state.hasImage && state.imageBase64 != null) {
            item {
                ImagePreviewCard(
                    imageBase64 = state.imageBase64!!,
                    onClear = { viewModel.reset() }
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // 文字输入（备选）——仅在无图片时显示
        if (!state.hasImage) {
            item {
                Text(
                    text = stringResource(R.string.camera_or_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { FoodInputArea(state, viewModel) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (state.parsedFoods.isNotEmpty()) {
            item { FoodResultList(state) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { TotalSection(state) }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { MacroSummary(state) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            if (state.aiComment.isNotEmpty()) {
                item { AiQuoteCard(state.aiComment, label = stringResource(R.string.chat_ai_comment)) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
            item { ConfirmButton(state, viewModel) }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        val errorMsg = state.error
        if (errorMsg != null) {
            item {
                Text(text = stringResource(R.string.common_error_prefix, errorMsg), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PageTitle() {
    Text(text = "RECORD", style = MaterialTheme.typography.displayMedium, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MealTypeSelector(state: CameraUiState, onSelect: (String) -> Unit) {
    val types = listOf("breakfast" to stringResource(R.string.meal_type_breakfast), "lunch" to stringResource(R.string.meal_type_lunch), "dinner" to stringResource(R.string.meal_type_dinner), "snack" to stringResource(R.string.meal_type_snack))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (key, label) ->
            val selected = state.mealType == key
            TextButton(
                onClick = { onSelect(key) },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(
                        if (selected) Modifier.background(FusionBlack, RoundedCornerShape(Dimen.RadiusMd))
                        else Modifier.border(
                            width = 1.dp,
                            color = FusionBorder,
                            shape = RoundedCornerShape(Dimen.RadiusMd)
                        )
                    ),
                shape = RoundedCornerShape(Dimen.RadiusMd)
            ) {
                Text(
                    text = label,
                    color = if (selected) FusionWhite else TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ImageSourceButtons(onTakePhoto: () -> Unit, onPickImage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 拍照按钮
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(Dimen.RadiusMd),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.camera_take_photo), fontWeight = FontWeight.Medium)
        }

        // 相册按钮
        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(Dimen.RadiusMd),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.camera_pick_from_gallery), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ImagePreviewCard(imageBase64: String, onClear: () -> Unit) {
    val bitmap = remember(imageBase64) {
        try {
            val bytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimen.RadiusLg),
        colors = CardDefaults.cardColors(containerColor = FusionCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = stringResource(R.string.camera_food_photo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(Dimen.RadiusLg)),
                    contentScale = ContentScale.Crop
                )
            }

            // 清除按钮
            TextButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Text("✕", color = FusionWhite, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun FoodInputArea(state: CameraUiState, viewModel: CameraViewModel) {
    Column {
        OutlinedTextField(
            value = state.inputText,
            onValueChange = { viewModel.updateInputText(it) },
            label = { Text(stringResource(R.string.camera_input_food_desc)) },
            placeholder = { Text(stringResource(R.string.camera_input_placeholder)) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(Dimen.RadiusLg)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.analyzeFood() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = state.inputText.isNotBlank() && !state.isAnalyzing,
            shape = RoundedCornerShape(Dimen.RadiusMd),
            colors = ButtonDefaults.buttonColors(containerColor = FusionBlack, contentColor = FusionWhite)
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FusionWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.camera_analyzing))
            } else {
                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.camera_ai_analyze))
            }
        }
    }
}

@Composable
private fun FoodResultList(state: CameraUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimen.RadiusLg),
        colors = CardDefaults.cardColors(containerColor = FusionCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            state.parsedFoods.forEachIndexed { index, food ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${index + 1}. ${food.name} ${food.weight}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "${food.calories.toInt()} kcal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun TotalSection(state: CameraUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(R.string.camera_total), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Text(text = "${state.totalCalories.toInt()} kcal", fontFamily = SerifFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    }
}

@Composable
private fun MacroSummary(state: CameraUiState) {
    Text(
        text = stringResource(R.string.camera_macro_summary, state.totalCarbs.toInt(), state.totalProtein.toInt(), state.totalFat.toInt()),
        color = TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConfirmButton(state: CameraUiState, viewModel: CameraViewModel) {
    Button(
        onClick = { viewModel.confirmRecord() },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = FusionBlack, contentColor = FusionWhite)
    ) {
        Text(stringResource(R.string.camera_confirm_record), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val scaled = scaleBitmap(bitmap, 1024)
    val outputStream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
    val bytes = outputStream.toByteArray()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

private fun uriToBase64(context: android.content.Context, uri: Uri): String {
    val options = android.graphics.BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
    }

    val maxSize = 1024
    var inSampleSize = 1
    if (options.outHeight > maxSize || options.outWidth > maxSize) {
        val halfHeight = options.outHeight / 2
        val halfWidth = options.outWidth / 2
        while (halfHeight / inSampleSize >= maxSize && halfWidth / inSampleSize >= maxSize) {
            inSampleSize *= 2
        }
    }

    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
    }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
        android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
    } ?: throw Exception("无法解码图片")

    val scaled = scaleBitmap(bitmap, maxSize)

    val outputStream = java.io.ByteArrayOutputStream()
    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
    val bytes = outputStream.toByteArray()

    if (scaled != bitmap) {
        bitmap.recycle()
    }

    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxSize && height <= maxSize) return bitmap
    val scale = maxSize.toFloat() / maxOf(width, height)
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
