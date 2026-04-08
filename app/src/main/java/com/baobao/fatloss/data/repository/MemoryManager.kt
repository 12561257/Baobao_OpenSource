package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.ChatMessageDao
import com.baobao.fatloss.data.local.dao.UserMemoryDao
import com.baobao.fatloss.data.local.dao.UserProfileDao
import com.baobao.fatloss.data.local.entity.UserMemoryEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
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
    private val memoryDao: UserMemoryDao
) {

    /**
     * 获取增强上下文，用于喂给 LLM 的 System Prompt。
     */
    suspend fun getEnhancedContext(userQuery: String, queryEmbedding: FloatArray? = null): String {
        val profile = profileDao.getProfileOnce()
        val recentMessages = chatDao.getRecentMessagesOnce(15).reversed()
        
        // 检索相关长期记忆 (Layer 3)
        val relevantMemories = if (queryEmbedding != null) {
            retrieveRelevantMemories(queryEmbedding)
        } else {
            emptyList()
        }

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

            // Layer 3: 长期经验 (按相似度排序的 Top 5)
            if (relevantMemories.isNotEmpty()) {
                append("\n【长期背景记录】\n")
                relevantMemories.take(5).forEach { 
                    append("- [ID: ${it.id}] ${it.content}\n")
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
     * 在本地计算余弦相似度进行语义检索 (模拟 Vector DB)
     */
    private suspend fun retrieveRelevantMemories(queryEmbedding: FloatArray): List<UserMemoryEntity> {
        val allMemories = memoryDao.getAllMemoriesList()
        return allMemories.filter { it.embedding != null }
            .map { it to cosineSimilarity(queryEmbedding, decodeEmbedding(it.embedding!!)) }
            .filter { it.second > 0.70 } // 相似度阈值
            .sortedByDescending { it.second }
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
