package com.baobao.fatloss.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baobao.fatloss.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.baobao.fatloss.data.local.entity.DailyNoteEntity
import com.baobao.fatloss.data.local.entity.UserMemoryEntity
import com.baobao.fatloss.ui.theme.Dimen
import com.baobao.fatloss.ui.theme.FusionBackground
import com.baobao.fatloss.ui.theme.FusionBlack
import com.baobao.fatloss.ui.theme.FusionBorder
import com.baobao.fatloss.ui.theme.FusionCard
import com.baobao.fatloss.ui.theme.TextPrimary
import com.baobao.fatloss.ui.theme.TextSecondary
import com.baobao.fatloss.ui.theme.TextTertiary
import com.baobao.fatloss.viewmodel.MemoryViewModel
import java.time.LocalDate

@Composable
fun MemoryScreen(
    navController: NavHostController,
    viewModel: MemoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.memory_tab_core),
        stringResource(R.string.memory_tab_daily),
        stringResource(R.string.memory_tab_cleanup)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FusionBackground)
    ) {
        // 顶部导航
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.common_back)
                )
            }
            Text(
                text = stringResource(R.string.memory_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> CoreMemoriesTab(
                memories = state.coreMemories,
                onAdd = { viewModel.showAddDialog() },
                onEdit = { viewModel.startEditing(it) },
                onDelete = { viewModel.deleteMemory(it) }
            )
            1 -> DailyNotesTab(
                notes = state.dailyNotes,
                onDelete = { viewModel.deleteDailyNote(it) }
            )
            2 -> DataCleanupTab(
                onClearMemories = { viewModel.showClearConfirm("memories") },
                onClearNotes = { viewModel.showClearConfirm("notes") },
                onClearChat = { viewModel.showClearConfirm("chat") }
            )
        }
    }

    // 添加记忆对话框
    if (state.showAddDialog) {
        AddMemoryDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { category, content ->
                viewModel.addMemory(category, content)
                viewModel.hideAddDialog()
            }
        )
    }

    // 编辑记忆对话框
    if (state.isEditing && state.editingMemory != null) {
        EditMemoryDialog(
            memory = state.editingMemory!!,
            onDismiss = { viewModel.cancelEditing() },
            onConfirm = { entity, content, category ->
                viewModel.updateMemory(entity, content, category)
            }
        )
    }

    // 清理确认对话框
    if (state.showClearConfirm != null) {
        val target = state.showClearConfirm!!
        val label = when (target) {
            "memories" -> stringResource(R.string.memory_all_core_memories)
            "notes" -> stringResource(R.string.memory_all_daily_notes)
            "chat" -> stringResource(R.string.memory_all_chat_history)
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { viewModel.hideClearConfirm() },
            title = { Text(stringResource(R.string.memory_confirm_clear)) },
            text = { Text(stringResource(R.string.memory_confirm_clear_msg, label)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClear(target) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.memory_confirm_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideClearConfirm() }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

// ============================================================
// 核心记忆 Tab
// ============================================================

@Composable
private fun CoreMemoriesTab(
    memories: List<UserMemoryEntity>,
    onAdd: () -> Unit,
    onEdit: (UserMemoryEntity) -> Unit,
    onDelete: (UserMemoryEntity) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (memories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.memory_no_core), color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.memory_ai_auto_record),
                                color = TextTertiary, style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            items(memories.size) { index ->
                MemoryCard(
                    memory = memories[index],
                    onEdit = { onEdit(memories[index]) },
                    onDelete = { onDelete(memories[index]) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // FAB 添加按钮
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = FusionBlack
        ) {
            Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MemoryCard(
    memory: UserMemoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryLabel = when (memory.category) {
        "preference" -> stringResource(R.string.memory_cat_preference)
        "observation" -> stringResource(R.string.memory_cat_observation)
        "habit" -> stringResource(R.string.memory_cat_habit)
        "goal" -> stringResource(R.string.memory_cat_goal)
        else -> stringResource(R.string.memory_cat_custom)
    }

    val sourceLabel = when (memory.source) {
        "ai" -> stringResource(R.string.memory_source_ai)
        "user" -> stringResource(R.string.memory_source_user)
        else -> ""
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
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChip(categoryLabel)
                    SmallChip(stringResource(R.string.memory_from_source, sourceLabel), alpha = 0.04f)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.common_edit),
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SmallChip(label: String, alpha: Float = 0.08f) {
    Box(
        modifier = Modifier
            .background(FusionBlack.copy(alpha = alpha), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (alpha > 0.05f) TextSecondary else TextTertiary,
            fontSize = 11.sp
        )
    }
}

// ============================================================
// 每日笔记 Tab
// ============================================================

@Composable
private fun DailyNotesTab(
    notes: List<DailyNoteEntity>,
    onDelete: (DailyNoteEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (notes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.memory_no_daily), color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.memory_notes_auto_gen),
                            color = TextTertiary, style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        items(notes.size) { index ->
            DailyNoteCard(
                note = notes[index],
                onDelete = { onDelete(notes[index]) }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun DailyNoteCard(
    note: DailyNoteEntity,
    onDelete: () -> Unit
) {
    val dateStr = try {
        LocalDate.ofEpochDay(note.date).toString()
    } catch (_: Exception) {
        stringResource(R.string.memory_unknown_date)
    }

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusLg)
            )
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${stringResource(R.string.memory_calories_prefix)}${note.calorieSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )

                if (note.aiComment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.memory_ai_comment_prefix)}${note.aiComment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

// ============================================================
// 数据清理 Tab
// ============================================================

@Composable
private fun DataCleanupTab(
    onClearMemories: () -> Unit,
    onClearNotes: () -> Unit,
    onClearChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.memory_bulk_cleanup),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.memory_cleanup_warning),
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )

        CleanupItem(
            title = stringResource(R.string.memory_clear_core_all),
            description = stringResource(R.string.memory_clear_core_desc),
            onClick = onClearMemories
        )

        CleanupItem(
            title = stringResource(R.string.memory_clear_daily_all),
            description = stringResource(R.string.memory_clear_daily_desc),
            onClick = onClearNotes
        )

        CleanupItem(
            title = stringResource(R.string.memory_clear_chat_all),
            description = stringResource(R.string.memory_clear_chat_desc),
            onClick = onClearChat
        )
    }
}

@Composable
private fun CleanupItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FusionCard, RoundedCornerShape(Dimen.RadiusLg))
            .border(
                width = 1.dp,
                color = FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusLg)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// ============================================================
// 添加记忆对话框
// ============================================================

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, content: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("custom") }

    val categories = listOf(
        "preference" to stringResource(R.string.memory_cat_preference),
        "observation" to stringResource(R.string.memory_cat_observation),
        "habit" to stringResource(R.string.memory_cat_habit),
        "goal" to stringResource(R.string.memory_cat_goal),
        "custom" to stringResource(R.string.memory_cat_custom)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.memory_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.memory_category), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (key, label) ->
                        CategorySelectChip(
                            label = label,
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.memory_content_label)) },
                    placeholder = { Text(stringResource(R.string.memory_content_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCategory, content) },
                enabled = content.isNotBlank()
            ) { Text(stringResource(R.string.memory_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun CategorySelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .background(
                color = if (selected) FusionBlack else Color.Transparent,
                shape = RoundedCornerShape(Dimen.RadiusMd)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else FusionBorder,
                shape = RoundedCornerShape(Dimen.RadiusMd)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// ============================================================
// 编辑记忆对话框
// ============================================================

@Composable
private fun EditMemoryDialog(
    memory: UserMemoryEntity,
    onDismiss: () -> Unit,
    onConfirm: (UserMemoryEntity, String, String) -> Unit
) {
    var content by remember { mutableStateOf(memory.content) }
    var selectedCategory by remember { mutableStateOf(memory.category) }

    val categories = listOf(
        "preference" to stringResource(R.string.memory_cat_preference),
        "observation" to stringResource(R.string.memory_cat_observation),
        "habit" to stringResource(R.string.memory_cat_habit),
        "goal" to stringResource(R.string.memory_cat_goal),
        "custom" to stringResource(R.string.memory_cat_custom)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.memory_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (key, label) ->
                        CategorySelectChip(
                            label = label,
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.memory_content_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(memory, content, selectedCategory) },
                enabled = content.isNotBlank()
            ) { Text(stringResource(R.string.memory_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
