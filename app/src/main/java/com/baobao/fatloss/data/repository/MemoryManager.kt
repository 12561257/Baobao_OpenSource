package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.ChatMessageDao
import com.baobao.fatloss.data.local.dao.DailyNoteDao
import com.baobao.fatloss.data.local.dao.UserMemoryDao
import com.baobao.fatloss.data.local.dao.UserProfileDao
import com.baobao.fatloss.data.local.entity.UserMemoryEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * AI 记忆管理器：实现 TechSpec 描述的三层记忆架构 (OpenClaw 架构)。
 * Layer 1: 静态属性 (UserProfile)
 * Layer 2: 短期工作区 (Recent Chat Context)
 * Layer 3: 长期经验记忆 (Semantic retrieval from UserMemory)
 */
class MemoryManager(
    private val profileDao: UserProfileDao,
    private val chatDao: ChatMessageDao,
    private val memoryDao: UserMemoryDao,
    private val dailyNoteDao: DailyNoteDao
) {

    companion object {
        /** 全量注入模式下最多注入的记忆条数，防止 token 超限 */
        private const val MAX_FALLBACK_MEMORIES = 20
        /** RAG 语义检索的相似度阈值 */
        private const val SIMILARITY_THRESHOLD = 0.60
        /** RAG 检索返回的最大条数 */
        private const val MAX_RAG_RESULTS = 10
    }

    /**
     * 获取增强上下文，用于喂给 LLM 的 System Prompt。
     *
     * 记忆检索策略（三层兜底）：
     * 1. 优先使用 RAG 语义检索（需要 embedding 可用）
     * 2. 如果 RAG 检索结果为空，回退到全量注入（按更新时间倒序取最近条目）
     * 3. 如果 embedding 不可用，直接全量注入
     */
    suspend fun getEnhancedContext(userQuery: String, queryEmbedding: FloatArray? = null): String {
        val profile = profileDao.getProfileOnce()
        val recentMessages = chatDao.getRecentMessagesOnce(15).reversed()
        val allMemories = memoryDao.getAllMemoriesList()
        val recentNotes = dailyNoteDao.getRecentNotesOnce(7)

        // 检索相关长期记忆 (Layer 3) —— 带兜底策略
        val displayMemories = resolveMemories(allMemories, queryEmbedding)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentTimeStr = sdf.format(Date())

        return buildString {
            append("【当前系统时间】\n- $currentTimeStr\n\n")

            // Layer 1: 静态画像
            append("【用户基础画像】\n")
            if (profile != null) {
                append("- 姓名：${profile.name}\n")
                append("- 身体数据：${profile.heightCm}cm, ${profile.currentWeight}kg (目标: ${profile.targetWeight}kg)\n")
                append("- 剩余热量预算：${profile.dailyBudget} 大卡\n")
                if (profile.aiPersona.isNotBlank()) {
                    append("- 你的助理设定：${profile.aiPersona}\n")
                }
            } else {
                append("- 暂无详细资料，请用通用语气引导用户补充。\n")
            }

            // Layer 3: 长期经验记忆
            if (displayMemories.isNotEmpty()) {
                append("\n【核心记忆档案】\n")
                append("以下是关于用户的重要记忆，你必须参考这些信息来回答问题：\n")
                displayMemories.forEach {
                    append("- [ID: ${it.id}] ${it.content}\n")
                }
            }

            // 每日笔记：最近 7 天的饮食摘要
            if (recentNotes.isNotEmpty()) {
                append("\n【每日饮食笔记（近7天）】\n")
                append("当用户问到某天的饮食、摄入量、热量时，必须参考以下记录回答：\n")
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                recentNotes.forEach { note ->
                    val dateStr = dateFormatter.format(Date(note.date * 86400000L))
                    append("- $dateStr：${note.summary}（${note.calorieSummary}）AI点评：${note.aiComment}\n")
                }
            }

            // Layer 2: 当前短期对话摘要 (仅列出最近几轮，防止 Token 爆炸)
            if (recentMessages.isNotEmpty()) {
                append("\n【近期对话上下文摘要】\n")
                recentMessages.forEach { msg ->
                    val role = if (msg.role == "user") "用户" else "助手"
                    val timeStr = sdf.format(Date(msg.timestamp))
                    append("- [$timeStr] $role: ${msg.content.take(60)}${if(msg.content.length > 60) "..." else ""}\n")
                }
            }
        }
    }

    /**
     * 三层兜底记忆检索：
     * 1. embedding 可用 → RAG 语义检索
     * 2. RAG 结果为空或 embedding 不可用 → 全量注入最近记忆
     */
    private suspend fun resolveMemories(
        allMemories: List<UserMemoryEntity>,
        queryEmbedding: FloatArray?
    ): List<UserMemoryEntity> {
        if (allMemories.isEmpty()) return emptyList()

        // 尝试 RAG 语义检索
        if (queryEmbedding != null) {
            val ragResults = retrieveRelevantMemories(queryEmbedding, allMemories)
            if (ragResults.isNotEmpty()) return ragResults
        }

        // 兜底：全量注入最近记忆
        return allMemories.sortedByDescending { it.updatedAt }.take(MAX_FALLBACK_MEMORIES)
    }

    /**
     * 在本地计算余弦相似度进行语义检索 (模拟 Vector DB)
     */
    private fun retrieveRelevantMemories(queryEmbedding: FloatArray, allMemories: List<UserMemoryEntity>): List<UserMemoryEntity> {
        return allMemories.filter { it.embedding != null }
            .map { it to cosineSimilarity(queryEmbedding, decodeEmbedding(it.embedding!!)) }
            .filter { it.second > SIMILARITY_THRESHOLD }
            .sortedByDescending { it.second }
            .take(MAX_RAG_RESULTS)
            .map { it.first }
    }

    private fun decodeEmbedding(json: String): FloatArray {
        return try {
            val arr = JSONArray(json)
            FloatArray(arr.length()) { i -> arr.getDouble(i).toFloat() }
        } catch (_: Exception) {
            FloatArray(0)
        }
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size || vec1.isEmpty()) return 0.0
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vec1.indices) {
            dotProduct += (vec1[i] * vec2[i]).toDouble()
            normA += (vec1[i] * vec1[i]).toDouble()
            normB += (vec2[i] * vec2[i]).toDouble()
        }
        return if (normA > 0 && normB > 0) dotProduct / (sqrt(normA) * sqrt(normB)) else 0.0
    }
}
