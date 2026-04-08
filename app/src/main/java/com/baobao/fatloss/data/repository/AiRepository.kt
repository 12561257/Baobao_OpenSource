package com.baobao.fatloss.data.repository

import android.util.Log
import android.content.Context
import com.baobao.fatloss.R
import com.baobao.fatloss.data.local.ApiKeyStore
import com.baobao.fatloss.data.local.dao.*
import com.baobao.fatloss.data.local.entity.ChatMessageEntity
import com.baobao.fatloss.data.local.entity.DailyNoteEntity
import com.baobao.fatloss.data.local.entity.FoodLogEntity
import com.baobao.fatloss.data.local.entity.UserMemoryEntity
import com.baobao.fatloss.data.remote.DoubaoApiService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** AI 解析出的动作结果 */
data class ParsedActions(
    val foodLogs: List<ParsedFoodItem> = emptyList(),
    val exercise: ParsedExercise? = null,
    val deleteFoods: List<ParsedDeleteFood> = emptyList()
)

data class ParsedFoodItem(
    val meal: String,         // breakfast / lunch / dinner / snack
    val name: String,
    val calories: Double,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0
)

data class ParsedExercise(
    val name: String,
    val caloriesBurned: Double
)

data class ParsedDeleteFood(
    val foodName: String,
    val meal: String  // breakfast / lunch / dinner / snack
)

class AiRepository(
    private val chatMessageDao: ChatMessageDao,
    private val doubaoApiService: DoubaoApiService,
    private val apiKeyStore: ApiKeyStore,
    private val userProfileDao: UserProfileDao,
    private val dailyLedgerDao: DailyLedgerDao,
    private val foodLogDao: FoodLogDao,
    private val userMemoryDao: UserMemoryDao,
    private val dailyNoteDao: DailyNoteDao,
    private val memoryManager: MemoryManager,
    private val context: Context
) {

    fun getAllMessages(): Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun clearAll() {
        chatMessageDao.clearHistory()
    }

    /**
     * 获取当前 API 配置，未配置时抛出明确异常。
     */
    private suspend fun requireApiConfig(): Triple<String, String, String> {
        val key = apiKeyStore.getApiKey()
        if (key.isNullOrBlank()) {
            throw IllegalStateException("API Key not configured")
        }
        val modelId = apiKeyStore.getModelId()
        val baseUrl = apiKeyStore.getBaseUrl()
        if (baseUrl.isBlank()) {
            throw IllegalStateException("API base URL not configured")
        }
        return Triple(key, modelId, baseUrl)
    }

    suspend fun sendMessage(userMessage: String, imagePath: String? = null): String {
        val (apiKey, modelId, baseUrl) = requireApiConfig()

        // 保存用户消息
        chatMessageDao.insertMessage(ChatMessageEntity(
            role = "user",
            content = userMessage,
            imagePath = imagePath
        ))

        // 获取三层记忆增强上下文 (embedding 降级：失败时不用 RAG)
        val embedding = try { doubaoApiService.getEmbedding(baseUrl, apiKey, modelId, userMessage) } catch (_: Exception) { null }
        val enhancedContext = memoryManager.getEnhancedContext(userMessage, embedding)

        val systemPrompt = buildSystemPrompt(enhancedContext)
        val history = getRecentHistory()

        val imageBase64 = imagePath?.let { fileToBase64(it) }

        val aiReply = try {
            doubaoApiService.chat(baseUrl, apiKey, modelId, systemPrompt, history, userMessage, imageBase64)
        } catch (e: Exception) {
            context.getString(R.string.error_ai_service, e.message ?: "")
        }

        processMemoryActions(aiReply, baseUrl, apiKey, modelId)

        // 保存助理回复
        val cleanReply = Regex("""\[MEMORY_ACTION\].*?\[/MEMORY_ACTION\]""", RegexOption.DOT_MATCHES_ALL).replace(aiReply, "").trim()
        chatMessageDao.insertMessage(ChatMessageEntity(role = "assistant", content = cleanReply))

        return cleanReply
    }

    suspend fun sendMessageAndParseActions(userMessage: String, imagePath: String? = null): Pair<String, ParsedActions> {
        val (apiKey, modelId, baseUrl) = requireApiConfig()

        chatMessageDao.insertMessage(ChatMessageEntity(
            role = "user",
            content = userMessage,
            imagePath = imagePath
        ))

        val embedding = try { doubaoApiService.getEmbedding(baseUrl, apiKey, modelId, userMessage) } catch (_: Exception) { null }
        val enhancedContext = memoryManager.getEnhancedContext(userMessage, embedding)

        val systemPrompt = buildSystemPrompt(enhancedContext)
        val history = getRecentHistory()

        val imageBase64 = imagePath?.let { fileToBase64(it) }

        val rawReply = try {
            doubaoApiService.chat(baseUrl, apiKey, modelId, systemPrompt, history, userMessage, imageBase64)
        } catch (e: Exception) {
            context.getString(R.string.error_ai_unable, e.message ?: "")
        }

        val (cleanReply, actions) = parseActionsFromReply(rawReply)
        processMemoryActions(rawReply, baseUrl, apiKey, modelId)

        chatMessageDao.insertMessage(ChatMessageEntity(role = "assistant", content = cleanReply))

        return Pair(cleanReply, actions)
    }

    suspend fun analyzeFood(foodDescription: String): String {
        val (apiKey, modelId, baseUrl) = requireApiConfig()

        val profile = userProfileDao.getProfileOnce()
        val gender = if (profile?.gender == 2) "女" else "男"
        val todayLedger = dailyLedgerDao.getLedgerOnce(LocalDate.now().toEpochDay())
        val remaining = todayLedger?.netRemaining ?: 0.0

        val prompt = """你是一个专业的精算营养师。请分析：$foodDescription
请结合用户资料（性别：$gender，今日余量：$remaining kcal）进行点评。
请务必返回 JSON 格式：{"foods":[{"name":"..","weight":"..","calories":..,"carbs":..,"protein":..,"fat":..}], "total_cal":.., "ai_comment":".."}"""

        return try {
            doubaoApiService.chat(baseUrl, apiKey, modelId, "你是营养分析师，只返回 JSON。", emptyList(), prompt)
        } catch (e: Exception) {
            """{"foods":[], "total_cal":0, "ai_comment":"${context.getString(R.string.error_analysis_failed)}"}"""
        }
    }

    suspend fun analyzeFoodImage(imageBase64: String): String {
        val (apiKey, modelId, baseUrl) = requireApiConfig()

        val profile = userProfileDao.getProfileOnce()
        val gender = if (profile?.gender == 2) "女" else "男"
        val todayLedger = dailyLedgerDao.getLedgerOnce(LocalDate.now().toEpochDay())
        val remaining = todayLedger?.netRemaining ?: 0.0

        val textPrompt = """分析图片中的食物。用户性别：$gender，今日预算余量：$remaining kcal。
请务必返回 JSON 格式：{"foods":[{"name":"..","calories":..,"carbs":..,"protein":..,"fat":..}], "total_cal":.., "ai_comment":".."}"""

        return try {
            doubaoApiService.analyzeImage(baseUrl, apiKey, modelId, "视觉营养分析师，只返回 JSON。", textPrompt, imageBase64)
        } catch (e: Exception) {
            """{"foods":[], "total_cal":0, "ai_comment":"${context.getString(R.string.error_analysis_error)}"}"""
        }
    }

    /**
     * 获取最近对话历史（OpenAI 格式：role 为 "user" 或 "assistant"）。
     */
    private suspend fun getRecentHistory(): List<Pair<String, String>> {
        return try {
            val messages = chatMessageDao.getRecentMessagesOnce(10)
            messages.reversed().map { msg ->
                Pair(msg.role, msg.content)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun processMemoryActions(reply: String, baseUrl: String, apiKey: String, modelId: String) {
        val actionRegex = Regex("""\[MEMORY_ACTION\](.*?)\[/MEMORY_ACTION\]""", RegexOption.DOT_MATCHES_ALL)
        actionRegex.findAll(reply).forEach { match ->
            try {
                val json = JSONObject(cleanJsonString(match.groupValues[1]))
                // 1. 处理新增 (Add)
                val adds = json.optJSONArray("add")
                if (adds != null) {
                    for (i in 0 until adds.length()) {
                        val addObj = adds.getJSONObject(i)
                        val content = addObj.optString("content", "")
                        if (content.isNotBlank()) {
                            val vector = try { doubaoApiService.getEmbedding(baseUrl, apiKey, modelId, content) } catch (_: Exception) { null }
                            val embeddingJson = vector?.let { JSONArray(it).toString() }
                            userMemoryDao.insert(UserMemoryEntity(
                                category = addObj.optString("category", "preference"),
                                content = content,
                                source = "ai_reflection",
                                embedding = embeddingJson
                            ))
                        }
                    }
                }

                // 2. 处理修改 (Update)
                val updates = json.optJSONArray("update")
                if (updates != null) {
                    for (i in 0 until updates.length()) {
                        val updObj = updates.getJSONObject(i)
                        val id = updObj.optLong("id", -1L)
                        val content = updObj.optString("content", "")
                        if (id != -1L && content.isNotBlank()) {
                            val vector = try { doubaoApiService.getEmbedding(baseUrl, apiKey, modelId, content) } catch (_: Exception) { null }
                            val embeddingJson = vector?.let { JSONArray(it).toString() }
                            // 获取旧条目保持 category 不变，或者简单更新特定 ID
                            userMemoryDao.updateById(id, content, embeddingJson, System.currentTimeMillis())
                        }
                    }
                }

                // 3. 处理删除 (Remove)
                val removes = json.optJSONArray("remove")
                if (removes != null) {
                    for (i in 0 until removes.length()) {
                        val id = removes.optLong(i, -1L)
                        if (id != -1L) {
                            userMemoryDao.deleteById(id)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildSystemPrompt(enhancedContext: String): String {
        return """你是用户的专属智能减脂助理。你需要基于以下【增强记忆上下文】来与用户交流。

$enhancedContext

【回复准则】
1. 始终保持你设定的教练人设。
2. 绝对尊重上下文中的【用户画像】，特别是性别和热量预算。
3. 你的目标是协助用户达成减脂目标。
4. 【重要】如果用户对食物的描述较为模糊（如“一碗米饭”、“一个苹果”、“一份炒青菜”），请根据标准份量或常见人份直接进行热量及营养估算，严禁追问用户具体的克数或细节。除非食材极其特殊且信息严重缺失才进行礼貌询问。
5. 【时间意识】请参考上下文中的【当前系统时间】判定用户的意图。所有的食物和运动记录默认均计入“今天”，除非用户明确提到了是针对过去某天的补记。

【动作指令】
1. 食物记录：[FOOD_LOG]{"meal":"breakfast|lunch|dinner|snack","foods":[{"name":"名","calories":数值,"carbs":数值,"protein":数值,"fat":数值}]}[/FOOD_LOG]
2. 运动记录：[EXERCISE]{"name":"描述","calories_burned":数值}[/EXERCISE]
3. 维护记忆：[MEMORY_ACTION]{"add":[{"category":"..","content":".."}], "update":[{"id":ID,"content":".."}], "remove":[ID1,ID2]}[/MEMORY_ACTION]
    - 重要：在记录前先比对【长期背景记录】中的 [ID: 内容]。如果仅需修正某个事实或内容重合，请使用 `update` 修改对应 ID，严禁通过 `add` 产生重复记录。如果记录完全错误请使用 `remove`。
4. 删除食物：[DELETE_FOOD]{"foodName":"准确名称","meal":"breakfast|lunch|dinner|snack"}[/DELETE_FOOD]

直接开始对话，不要输出多余的引导词。"""
    }

    suspend fun generateDailyNote() {
        val (apiKey, modelId, baseUrl) = requireApiConfig()

        val yesterday = LocalDate.now().minusDays(1).toEpochDay()
        if (dailyNoteDao.getByDate(yesterday) != null) return

        val foods = try { foodLogDao.getFoodsByDateOnce(yesterday) } catch (_: Exception) { emptyList() }
        if (foods.isEmpty()) return

        val ledger = dailyLedgerDao.getLedgerOnce(yesterday)
        val totalCal = foods.sumOf { it.estimatedCal }.toInt()
        val budget = ledger?.dailyBudget?.toInt() ?: 0

        val prompt = "昨日总结：摄入 $totalCal/$budget kcal。请简要点评并给出一条核心建议。返回 JSON: {\"summary\":\"..\",\"aiComment\":\"..\"}"
        try {
            val result = doubaoApiService.chat(baseUrl, apiKey, modelId, "你是总结专家，只返回JSON。", emptyList(), prompt)
            val json = JSONObject(cleanJsonString(result))
            dailyNoteDao.upsert(DailyNoteEntity(
                date = yesterday,
                summary = json.optString("summary", "昨日总结"),
                calorieSummary = "$totalCal/$budget kcal",
                aiComment = json.optString("aiComment", "")
            ))
        } catch (_: Exception) {}
    }

    internal fun parseActionsFromReply(reply: String): Pair<String, ParsedActions> {
        var cleanReply = reply
        val foodItems = mutableListOf<ParsedFoodItem>()
        val deleteItems = mutableListOf<ParsedDeleteFood>()
        var exercise: ParsedExercise? = null

        val foodLogRegex = Regex("""\[FOOD_LOG\](.*?)\[/FOOD_LOG\]""", RegexOption.DOT_MATCHES_ALL)
        foodLogRegex.findAll(reply).forEach { match ->
            try {
                val json = JSONObject(cleanJsonString(match.groupValues[1]))
                val meal = json.optString("meal", "snack")
                val foodsArray = json.optJSONArray("foods") ?: JSONArray()
                for (i in 0 until foodsArray.length()) {
                    val foodObj = foodsArray.getJSONObject(i)
                    foodItems.add(ParsedFoodItem(
                        meal = meal,
                        name = foodObj.optString("name", ""),
                        calories = foodObj.optDouble("calories", 0.0),
                        carbs = foodObj.optDouble("carbs", 0.0),
                        protein = foodObj.optDouble("protein", 0.0),
                        fat = foodObj.optDouble("fat", 0.0)
                    ))
                }
            } catch (_: Exception) {}
        }
        cleanReply = foodLogRegex.replace(cleanReply, "").trim()

        val deleteRegex = Regex("""\[DELETE_FOOD\](.*?)\[/DELETE_FOOD\]""", RegexOption.DOT_MATCHES_ALL)
        deleteRegex.findAll(reply).forEach { match ->
            try {
                val json = JSONObject(cleanJsonString(match.groupValues[1]))
                deleteItems.add(ParsedDeleteFood(
                    meal = json.optString("meal", ""),
                    foodName = json.optString("foodName", "")
                ))
            } catch (_: Exception) {}
        }
        cleanReply = deleteRegex.replace(cleanReply, "").trim()

        val exerciseRegex = Regex("""\[EXERCISE\](.*?)\[/EXERCISE\]""", RegexOption.DOT_MATCHES_ALL)
        exerciseRegex.findAll(reply).forEach { match ->
            try {
                val json = JSONObject(cleanJsonString(match.groupValues[1]))
                exercise = ParsedExercise(
                    name = json.optString("name", ""),
                    caloriesBurned = json.optDouble("calories_burned", 0.0)
                )
            } catch (_: Exception) {}
        }
        cleanReply = exerciseRegex.replace(cleanReply, "").trim()

        val actionRegex = Regex("""\[MEMORY_ACTION\].*?\[/MEMORY_ACTION\]""", RegexOption.DOT_MATCHES_ALL)
        cleanReply = actionRegex.replace(cleanReply, "").trim()

        return Pair(cleanReply, ParsedActions(foodItems, exercise, deleteItems))
    }

    private fun cleanJsonString(str: String): String = str.replace(Regex("```json|```", RegexOption.IGNORE_CASE), "").trim()

    private fun fileToBase64(filePath: String): String? {
        return try {
            val file = java.io.File(filePath)
            if (!file.exists()) return null
            
            // 1. 获取图片尺寸信息
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(filePath, options)
            
            // 2. 计算缩放比例 (目标最大 1280 像素)
            val maxSize = 1280
            var inSampleSize = 1
            if (options.outHeight > maxSize || options.outWidth > maxSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxSize && halfWidth / inSampleSize >= maxSize) {
                    inSampleSize *= 2
                }
            }
            
            // 3. 解码图片
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = android.graphics.BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null
            
            // 4. 精确缩放并压缩
            val width = bitmap.width
            val height = bitmap.height
            val scale = maxSize.toFloat() / maxOf(width, height)
            val finalBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                bitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            
            // 5. 释放资源
            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }
            bitmap.recycle()
            
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
