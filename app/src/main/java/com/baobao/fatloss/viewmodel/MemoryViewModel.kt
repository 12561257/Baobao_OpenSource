package com.baobao.fatloss.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.data.local.dao.DailyNoteDao
import com.baobao.fatloss.data.local.dao.UserMemoryDao
import com.baobao.fatloss.data.local.entity.DailyNoteEntity
import com.baobao.fatloss.data.local.entity.UserMemoryEntity
import com.baobao.fatloss.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoryUiState(
    val coreMemories: List<UserMemoryEntity> = emptyList(),
    val dailyNotes: List<DailyNoteEntity> = emptyList(),
    val isEditing: Boolean = false,
    val editingMemory: UserMemoryEntity? = null,
    val showAddDialog: Boolean = false,
    val showClearConfirm: String? = null  // "memories" | "notes" | "chat"
)

class MemoryViewModel(
    private val userMemoryDao: UserMemoryDao,
    private val dailyNoteDao: DailyNoteDao,
    private val aiRepo: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userMemoryDao.getAllMemories().collect { memories ->
                _uiState.update { it.copy(coreMemories = memories) }
            }
        }
        viewModelScope.launch {
            dailyNoteDao.getRecentNotes(14).collect { notes ->
                _uiState.update { it.copy(dailyNotes = notes) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun addMemory(category: String, content: String) {
        viewModelScope.launch {
            userMemoryDao.insert(UserMemoryEntity(
                category = category,
                content = content,
                source = "user"
            ))
        }
    }

    fun startEditing(memory: UserMemoryEntity) {
        _uiState.update { it.copy(isEditing = true, editingMemory = memory) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, editingMemory = null) }
    }

    fun updateMemory(entity: UserMemoryEntity, newContent: String, newCategory: String) {
        viewModelScope.launch {
            userMemoryDao.update(entity.copy(
                content = newContent,
                category = newCategory,
                updatedAt = System.currentTimeMillis()
            ))
            _uiState.update { it.copy(isEditing = false, editingMemory = null) }
        }
    }

    fun deleteMemory(entity: UserMemoryEntity) {
        viewModelScope.launch { userMemoryDao.delete(entity) }
    }

    fun deleteDailyNote(entity: DailyNoteEntity) {
        viewModelScope.launch { dailyNoteDao.deleteByDate(entity.date) }
    }

    fun showClearConfirm(target: String) {
        _uiState.update { it.copy(showClearConfirm = target) }
    }

    fun hideClearConfirm() {
        _uiState.update { it.copy(showClearConfirm = null) }
    }

    fun confirmClear(target: String) {
        viewModelScope.launch {
            when (target) {
                "memories" -> userMemoryDao.clearAll()
                "notes" -> dailyNoteDao.clearAll()
                "chat" -> aiRepo.clearAll()
            }
            _uiState.update { it.copy(showClearConfirm = null) }
        }
    }

    class Factory(
        private val userMemoryDao: UserMemoryDao,
        private val dailyNoteDao: DailyNoteDao,
        private val aiRepo: AiRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MemoryViewModel(userMemoryDao, dailyNoteDao, aiRepo) as T
    }
}
