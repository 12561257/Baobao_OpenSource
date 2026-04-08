package com.baobao.fatloss.ui.meallog

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import com.baobao.fatloss.ui.components.CardLabel
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.baobao.fatloss.R
import com.baobao.fatloss.data.local.entity.FoodLogEntity
import com.baobao.fatloss.ui.theme.*
import com.baobao.fatloss.viewmodel.MealLogViewModel
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun MealLogScreen(
    navController: NavHostController,
    viewModel: MealLogViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 临时拍照 URI（防止系统打开相机时杀掉本应用导致丢失）
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri = photoUriString?.let { Uri.parse(it) }
    val coroutineScope = rememberCoroutineScope()

    // 拍照 launcher — 使用 TakePicture（文件URI方式，避免闪退）
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            photoUri?.let { uri ->
                viewModel.setAnalysisMode("photo")
                coroutineScope.launch(Dispatchers.IO) {
                    val base64 = uriToBase64(context, uri)
                    viewModel.setDialogImageBase64(base64)
                }
            }
        }
    }

    // 准备拍照（创建临时文件并启动相机）
    val prepareAndLaunchCamera: () -> Unit = {
        val uri = createTempImageUri(context)
        photoUriString = uri.toString()
        takePictureLauncher.launch(uri)
    }

    // 记录权限回调来源：是弹窗拍照还是快捷入口拍照
    var pendingCameraSource by remember { mutableStateOf("quick") }

    // 运行时权限申请 launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            if (pendingCameraSource == "dialog") {
                viewModel.setAnalysisMode("photo")
            } else {
                if (!state.showAddDialog) viewModel.showAddMealDialog("lunch")
            }
            prepareAndLaunchCamera()
        } else {
            android.widget.Toast.makeText(context, context.getString(R.string.camera_need_permission), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 检查权限的统一入口
    val requestCameraWithPermission: (String) -> Unit = { source ->
        pendingCameraSource = source
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            if (source == "dialog") {
                viewModel.setAnalysisMode("photo")
            } else {
                if (!state.showAddDialog) viewModel.showAddMealDialog("lunch")
            }
            prepareAndLaunchCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // 相册 launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setAnalysisMode("photo")
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = uriToBase64(context, it)
                viewModel.setDialogImageBase64(base64)
            }
        }
    }

    if (state.showAddDialog) {
        AddFoodDialog(
            viewModel = viewModel,
            onTakePhoto = {
                requestCameraWithPermission("dialog")
            },
            onPickImage = {
                viewModel.setAnalysisMode("photo")
                pickImageLauncher.launch("image/*")
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FusionBackground)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { PageTitle() }
        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            DateSelectorRow(
                dateLabel = state.dateLabel,
                onPrevious = { viewModel.previousDay() },
                onNext = { viewModel.nextDay() }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 拍照 / 相册 快捷入口
        item {
            QuickRecordButtons(
                onTakePhoto = {
                    requestCameraWithPermission("quick")
                },
                onPickImage = {
                    if (!state.showAddDialog) viewModel.showAddMealDialog("lunch")
                    pickImageLauncher.launch("image/*")
                }
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            MealSection(
                title = stringResource(R.string.meal_type_breakfast),
                foods = state.breakfastFoods,
                mealType = "breakfast",
                onAddMeal = { viewModel.showAddMealDialog(it) },
                onDeleteFood = { viewModel.deleteFood(it) }
            )
        }

        item { Spacer(modifier = Modifier.height(Dimen.SectionGap)) }

        item {
            MealSection(
                title = stringResource(R.string.meal_type_lunch),
                foods = state.lunchFoods,
                mealType = "lunch",
                onAddMeal = { viewModel.showAddMealDialog(it) },
                onDeleteFood = { viewModel.deleteFood(it) }
            )
        }

        item { Spacer(modifier = Modifier.height(Dimen.SectionGap)) }

        item {
            MealSection(
                title = stringResource(R.string.meal_type_dinner),
                foods = state.dinnerFoods,
                mealType = "dinner",
                onAddMeal = { viewModel.showAddMealDialog(it) },
                onDeleteFood = { viewModel.deleteFood(it) }
            )
        }

        item { Spacer(modifier = Modifier.height(Dimen.SectionGap)) }

        item {
            MealSection(
                title = stringResource(R.string.meal_type_snack),
                foods = state.snackFoods,
                mealType = "snack",
                onAddMeal = { viewModel.showAddMealDialog(it) },
                onDeleteFood = { viewModel.deleteFood(it) }
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            DailySummary(
                totalConsumed = state.totalConsumed,
                dailyBudget = state.dailyBudget,
                netRemaining = state.netRemaining
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ============================================================
// 标题
// ============================================================

@Composable
private fun PageTitle() {
    Text(
        text = stringResource(R.string.meallog_today_records),
        style = MaterialTheme.typography.displayMedium
    )
}

// ============================================================
// 日期选择
// ============================================================

@Composable
private fun DateSelectorRow(
    dateLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.meallog_previous_day))
        }

        Text(
            text = dateLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontStyle = FontStyle.Normal
        )

        IconButton(onClick = onNext) {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.meallog_next_day))
        }
    }
}

// ============================================================
// 拍照/相册 快捷按钮
// ============================================================

@Composable
private fun QuickRecordButtons(onTakePhoto: () -> Unit, onPickImage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 拍照识别按钮
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(Dimen.RadiusMd),
            colors = ButtonDefaults.buttonColors(containerColor = FusionBlack, contentColor = FusionWhite)
        ) {
            Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.meallog_photo_recognize), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        // 相册按钮
        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(Dimen.RadiusMd),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.meallog_from_gallery), fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

// ============================================================
// 餐次区域
// ============================================================

@Composable
private fun MealSection(
    title: String,
    foods: List<FoodLogEntity>,
    mealType: String,
    onAddMeal: (String) -> Unit,
    onDeleteFood: (FoodLogEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MealHeader(title)

        Spacer(modifier = Modifier.height(12.dp))

        if (foods.isEmpty()) {
            EmptyMealSection(
                mealType = mealType,
                onAdd = { onAddMeal(mealType) }
            )
        } else {
            foods.forEach { food ->
                FoodRow(food = food, onDelete = onDeleteFood)
            }

            Spacer(modifier = Modifier.height(8.dp))

            MealSubtotal(foods.sumOf { it.estimatedCal }.toInt())
        }
    }
}

@Composable
private fun MealHeader(title: String) {
    CardLabel(title)
}

@Composable
private fun FoodRow(food: FoodLogEntity, onDelete: (FoodLogEntity) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = food.foodDescription,
            style = MaterialTheme.typography.bodyMedium
        )
        Row {
            Text(
                text = "${food.estimatedCal.toInt()} kcal",
                style = MaterialTheme.typography.headlineSmall,
                color = TextSecondary
            )
            IconButton(onClick = { onDelete(food) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_cancel),
                    modifier = Modifier.size(14.dp),
                    tint = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun EmptyMealSection(mealType: String, onAdd: () -> Unit) {
    val mealLabel = when (mealType) {
        "breakfast" -> stringResource(R.string.meal_type_breakfast)
        "lunch" -> stringResource(R.string.meal_type_lunch)
        "dinner" -> stringResource(R.string.meal_type_dinner)
        "snack" -> stringResource(R.string.meal_type_snack)
        else -> ""
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(FusionWhite.copy(alpha = 0.5f), RoundedCornerShape(Dimen.RadiusMd))
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(FusionBorder),
                shape = RoundedCornerShape(Dimen.RadiusMd)
            )
            .clickable { onAdd() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                contentDescription = null,
                tint = FusionAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.meallog_record_meal, mealLabel),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MealSubtotal(calories: Int) {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = FusionBorder,
        thickness = 1.dp
    )

    Text(
        text = "$calories kcal",
        style = MaterialTheme.typography.headlineSmall,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth()
    )
}

// ============================================================
// 每日汇总
// ============================================================

@Composable
private fun DailySummary(
    totalConsumed: Double,
    dailyBudget: Double,
    netRemaining: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionBlack, RoundedCornerShape(Dimen.RadiusLg))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(stringResource(R.string.meallog_remaining_budget), color = FusionWhite.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${netRemaining.toInt()} kcal",
                        style = MaterialTheme.typography.displayLarge,
                        color = FusionWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(if (netRemaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (netRemaining >= 0) stringResource(R.string.meallog_in_progress) else stringResource(R.string.meallog_over_budget),
                        color = FusionWhite,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider(color = FusionWhite.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem(stringResource(R.string.meallog_consumed), "${totalConsumed.toInt()}", Modifier.weight(1f))
                SummaryItem(stringResource(R.string.meallog_total_budget), "${dailyBudget.toInt()}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = FusionWhite.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = FusionWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isAccent: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (isAccent) FusionAccent else TextPrimary,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================================
// 添加食物对话框 (支持文字描述 + 拍照识别)
// ============================================================

@Composable
private fun AddFoodDialog(
    viewModel: MealLogViewModel,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mealLabel = when (state.addMealType) {
        "breakfast" -> stringResource(R.string.meal_type_breakfast)
        "lunch" -> stringResource(R.string.meal_type_lunch)
        "dinner" -> stringResource(R.string.meal_type_dinner)
        "snack" -> stringResource(R.string.meal_type_snack)
        else -> ""
    }

    AlertDialog(
        onDismissRequest = { viewModel.hideAddDialog() },
        title = {
            Text(
                stringResource(R.string.meallog_record_meal, mealLabel),
                fontFamily = SerifFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 模式切换：文字 / 拍照
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeTab("✍️ 文字描述", state.analysisMode == "text", Modifier.weight(1f)) {
                        viewModel.setAnalysisMode("text")
                    }
                    ModeTab("📸 拍照识别", state.analysisMode == "photo", Modifier.weight(1f)) {
                        viewModel.setAnalysisMode("photo")
                    }
                }

                if (state.analysisMode == "text") {
                    // 文字描述模式
                    FoodLogInput(
                        value = state.addFoodInput,
                        onValueChange = { viewModel.updateAddFoodInput(it) },
                        label = stringResource(R.string.meallog_what_did_you_eat),
                        placeholder = stringResource(R.string.meallog_food_example),
                        enabled = !state.isAnalyzing
                    )

                    androidx.compose.material3.Button(
                        onClick = { viewModel.analyzeFoodInput() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = state.addFoodInput.isNotBlank() && !state.isAnalyzing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FusionBlack, contentColor = FusionWhite)
                    ) {
                        if (state.isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = FusionWhite, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.meallog_analyzing), fontFamily = SansFontFamily)
                        } else {
                            Text(stringResource(R.string.meallog_ai_convert), fontFamily = SansFontFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // 拍照模式
                    if (state.dialogImageBase64 != null) {
                        // 图片预览
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
                        ) {
                            ImagePreviewThumbnail(state.dialogImageBase64!!)
                        }
                    } else {
                        // 拍照/相册按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTakePhoto,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
                            ) {
                                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.meallog_take_photo_now), color = TextPrimary)
                            }
                            OutlinedButton(
                                onClick = onPickImage,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
                            ) {
                                Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.meallog_local_gallery), color = TextPrimary)
                            }
                        }
                    }

                    if (state.isAnalyzing && state.analyzedFoods.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = FusionBlack)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.meallog_ai_recognizing), color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }

                // 分析结果 (两种模式共用)
                if (state.analyzedFoods.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = FusionBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.analyzedFoods.forEachIndexed { index, food ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 1}. ${food.name}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text("${food.calories.toInt()} kcal", fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            
                            HorizontalDivider(color = FusionDivider, thickness = 1.dp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.meallog_total), fontWeight = FontWeight.Bold)
                                Text("${state.analysisTotalCal.toInt()} kcal", fontWeight = FontWeight.Bold, color = FusionAccent)
                            }
                            
                            if (state.analysisComment.isNotEmpty()) {
                                Text(
                                    text = state.analysisComment,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                    
                    // 确认添加按钮 - 只有当有结果时才显示在底部
                    androidx.compose.material3.Button(
                        onClick = { viewModel.confirmAnalyzedFoods() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FusionAccent)
                    ) {
                        Text(stringResource(R.string.meallog_save_to, mealLabel), color = FusionWhite, fontWeight = FontWeight.Bold)
                    }
                }

                // 错误信息
                state.analysisError?.let {
                    Text(
                        text = "⚠️ $it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { viewModel.hideAddDialog() }) {
                Text(stringResource(R.string.meallog_cancel_record), color = TextTertiary)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = FusionWhite
    )
}

@Composable
private fun FoodLogInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FusionBlack,
                unfocusedBorderColor = FusionBorder,
                cursorColor = FusionBlack
            )
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (selected) FusionBlack else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                if (selected) 0.dp else 1.dp,
                if (selected) Color.Transparent else FusionBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) FusionWhite else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ImagePreviewThumbnail(imageBase64: String) {
    val bitmap = remember(imageBase64) {
        try {
            val bytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = stringResource(R.string.meallog_food_photo),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(Dimen.RadiusMd)),
            contentScale = ContentScale.Crop
        )
    }
}

// ============================================================
// 工具方法
// ============================================================

private fun bitmapToBase64(bitmap: Bitmap): String {
    // 压缩到最大 1024px 再编码
    val scaled = scaleBitmap(bitmap, 1024)
    val outputStream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
    val bytes = outputStream.toByteArray()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

private fun uriToBase64(context: android.content.Context, uri: Uri): String {
    // 1. 只获取图片的宽高，不把图片读入内存，避免 OOM
    val options = android.graphics.BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
    }

    // 2. 计算缩放比例 (inSampleSize)
    val maxSize = 1024
    var inSampleSize = 1
    if (options.outHeight > maxSize || options.outWidth > maxSize) {
        val halfHeight = options.outHeight / 2
        val halfWidth = options.outWidth / 2
        while (halfHeight / inSampleSize >= maxSize && halfWidth / inSampleSize >= maxSize) {
            inSampleSize *= 2
        }
    }

    // 3. 使用计算出的 inSampleSize 真正把图片加载到内存
    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
    }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
        android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
    } ?: throw Exception("无法解码图片")

    // 4. 精确缩放到我们的目标尺寸
    val scaled = scaleBitmap(bitmap, maxSize)

    val outputStream = java.io.ByteArrayOutputStream()
    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
    val bytes = outputStream.toByteArray()

    // 释放内存
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

private fun createTempImageUri(context: android.content.Context): Uri {
    val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
