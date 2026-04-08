package com.baobao.fatloss.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.baobao.fatloss.ui.components.AiQuoteCard
import com.baobao.fatloss.ui.components.CardLabel
import com.baobao.fatloss.ui.components.DataCell
import com.baobao.fatloss.ui.components.SerifNumber
import androidx.compose.ui.res.stringResource
import com.baobao.fatloss.R
import com.baobao.fatloss.ui.theme.*
import com.baobao.fatloss.viewmodel.ProgressViewModel

@Composable
fun ProgressScreen(
    navController: NavHostController,
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.showWeightDialog) {
        WeightDialog(
            onRecord = { viewModel.recordWeight(it) },
            onDismiss = { viewModel.hideWeightDialog() }
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

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            HeroStat(
                currentWeight = state.currentWeight,
                weightLost = state.weightLost,
                onRecordWeight = { viewModel.showWeightDialog() }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item { WeightTrendChart(weightTrend = state.weightTrend) }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            DataGrid(
                weightToGo = state.weightToGo,
                bmi = state.bmi,
                avgDailyCalories = state.avgDailyCalories
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            WeeklyReport(
                weeklyOnTrackDays = state.weeklyOnTrackDays,
                weeklyAvgCalories = state.weeklyAvgCalories,
                weeklyBurned = state.weeklyBurned,
                weeklyWeightChange = state.weeklyWeightChange
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            AiQuoteCard(
                stringResource(R.string.progress_ai_advice_content),
                label = stringResource(R.string.progress_ai_advice)
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun PageTitle() {
    Text(
        text = stringResource(R.string.progress_title),
        style = MaterialTheme.typography.displayMedium
    )
}

@Composable
private fun HeroStat(currentWeight: Double, weightLost: Double, onRecordWeight: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SerifNumber(
                number = String.format("%.1f", currentWeight),
                fontSize = 64.sp
            )
            Text(
                text = stringResource(R.string.common_kg),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.progress_weight_lost, String.format("%.1f", weightLost)),
            style = MaterialTheme.typography.titleSmall,
            color = FusionAccent,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRecordWeight,
            shape = RoundedCornerShape(Dimen.RadiusMd),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = FusionAccent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.progress_record_weight), color = FusionAccent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun WeightTrendChart(weightTrend: List<com.baobao.fatloss.data.local.entity.WeightRecordEntity>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(1.dp, FusionBorder, RoundedCornerShape(Dimen.RadiusLg)),
        contentAlignment = Alignment.Center
    ) {
        if (weightTrend.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_no_weight_data),
                color = TextTertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val minWeight = weightTrend.minOfOrNull { it.weight }?.let { (it - 1f).toFloat() } ?: 76f
                val maxWeight = weightTrend.maxOfOrNull { it.weight }?.let { (it + 1f).toFloat() } ?: 79f
                val yRange = maxWeight - minWeight

                if (width > 0f && height > 0f && yRange > 0f && weightTrend.size >= 2) {
                    val xStep = width / (weightTrend.size - 1)
                    val yScale = height / yRange

                    // 绘制面积填充
                    val fillPath = Path().apply {
                        moveTo(0f, height)
                        weightTrend.forEachIndexed { index, record ->
                            val x = index * xStep
                            val y = height - ((record.weight.toFloat() - minWeight) * yScale)
                            lineTo(x, y)
                        }
                        lineTo((weightTrend.size - 1) * xStep, height)
                    }
                    drawPath(
                        path = fillPath,
                        color = Color(0x1A1A1A1A),
                        alpha = 0.1f
                    )

                    // 绘制折线
                    val linePath = Path().apply {
                        weightTrend.forEachIndexed { index, record ->
                            val x = index * xStep
                            val y = height - ((record.weight.toFloat() - minWeight) * yScale)
                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = FusionBlack,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 绘制关键节点
                    weightTrend.forEachIndexed { index, record ->
                        val x = index * xStep
                        val y = height - ((record.weight.toFloat() - minWeight) * yScale)
                        drawCircle(
                            color = FusionBlack,
                            center = Offset(x, y),
                            radius = 3.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataGrid(weightToGo: Double, bmi: Double, avgDailyCalories: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimen.GridGap)
        ) {
            DataCell(stringResource(R.string.progress_to_goal), String.format("%.1f kg", weightToGo), modifier = Modifier.weight(1f))
            DataCell(stringResource(R.string.progress_bmi), String.format("%.1f", bmi), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(Dimen.GridGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimen.GridGap)
        ) {
            DataCell(stringResource(R.string.progress_avg_daily_intake), String.format("%.0f", avgDailyCalories), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeeklyReport(
    weeklyOnTrackDays: Int,
    weeklyAvgCalories: Double,
    weeklyBurned: Double,
    weeklyWeightChange: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FusionBorder, RoundedCornerShape(Dimen.RadiusLg)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            CardLabel(stringResource(R.string.progress_weekly_report))

            Spacer(modifier = Modifier.height(12.dp))

            ReportRow(stringResource(R.string.progress_on_track_days), "$weeklyOnTrackDays / 7")
            ReportRow(stringResource(R.string.progress_avg_intake), String.format("%.0f kcal", weeklyAvgCalories))
            ReportRow(stringResource(R.string.progress_exercise_burned), String.format("%.0f kcal", weeklyBurned))
            ReportRow(stringResource(R.string.progress_weekly_weight_change), String.format("%.1f kg", weeklyWeightChange))
            ReportRow(stringResource(R.string.progress_assessment), stringResource(R.string.progress_assessment_normal), isAccent = true)
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, isAccent: Boolean = false) {
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
            style = MaterialTheme.typography.titleSmall,
            color = if (isAccent) FusionAccent else TextPrimary,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun WeightDialog(
    onRecord: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.progress_record_today_weight)) },
        text = {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text(stringResource(R.string.progress_weight_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                weight.toDoubleOrNull()?.let(onRecord)
            }) { Text(stringResource(R.string.common_record)) }
        }
    )
}
