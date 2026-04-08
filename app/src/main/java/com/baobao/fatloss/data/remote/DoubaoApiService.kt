package com.baobao.fatloss.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI 兼容 API 服务，支持任意提供商（火山方舟 / DeepSeek / OpenAI / 自定义）。
 * baseUrl 由调用方传入，不再硬编码。
 */
class DoubaoApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept-Encoding", "identity")
                .build()
            chain.proceed(request)
        }
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 发送聊天请求（OpenAI Chat Completions 兼容格式）。
     */
    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userMessage: String,
        imageBase64: String? = null
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildChatRequestBody(modelId, systemPrompt, history, userMessage, imageBase64)
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            Log.e(TAG, "API error ${response.code}: $responseBody")
            throw Exception("API error ${response.code}: $responseBody")
        }

        parseChatResponse(responseBody)
    }

    /**
     * 多模态图片分析（复用 chat 端点，图片放在 content 数组中）。
     */
    suspend fun analyzeImage(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        textPrompt: String,
        imageBase64: String
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildImageRequestBody(modelId, systemPrompt, textPrompt, imageBase64)
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            Log.e(TAG, "Image analysis error ${response.code}: $responseBody")
            throw Exception("API error ${response.code}: $responseBody")
        }

        parseChatResponse(responseBody)
    }

    /**
     * 获取文本的嵌入向量（用于 RAG 语义检索）。
     * 失败时返回 null（降级为不使用 RAG）。
     */
    suspend fun getEmbedding(baseUrl: String, apiKey: String, modelId: String, text: String): FloatArray? = withContext(Dispatchers.IO) {
        val json = buildJsonObject {
            put("model", modelId)
            put("input", buildJsonArray {
                add(text)
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/embeddings")
            .header("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                Log.w(TAG, "Embedding API returned ${response.code}, skipping RAG")
                return@withContext null
            }

            val root = Json.parseToJsonElement(responseBody).jsonObject
            val data = root["data"]?.jsonArray
            val embedding = data?.firstOrNull()?.jsonObject?.get("embedding")?.jsonArray

            embedding?.map { it.jsonPrimitive.float }?.toFloatArray()
        } catch (e: Exception) {
            Log.w(TAG, "Embedding failed, RAG disabled: ${e.message}")
            null
        }
    }

    // ── 请求体构建 ──

    private fun buildChatRequestBody(
        modelId: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userMessage: String,
        imageBase64: String?
    ): String {
        val json = buildJsonObject {
            put("model", modelId)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                history.forEach { (role, content) ->
                    add(buildJsonObject {
                        put("role", role)
                        put("content", content)
                    })
                }
                if (imageBase64 != null) {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject {
                                    put("url", "data:image/jpeg;base64,$imageBase64")
                                })
                            })
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", userMessage)
                            })
                        })
                    })
                } else {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }
            })
        }
        return json.toString()
    }

    private fun buildImageRequestBody(
        modelId: String,
        systemPrompt: String,
        textPrompt: String,
        imageBase64: String
    ): String {
        val json = buildJsonObject {
            put("model", modelId)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:image/jpeg;base64,$imageBase64")
                            })
                        })
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", textPrompt)
                        })
                    })
                })
            })
        }
        return json.toString()
    }

    private fun parseChatResponse(responseBody: String): String {
        val json = Json.parseToJsonElement(responseBody).jsonObject
        val choices = json["choices"]?.jsonArray
        val firstChoice = choices?.firstOrNull()?.jsonObject
        val message = firstChoice?.get("message")?.jsonObject
        val content = message?.get("content")?.jsonPrimitive?.content
        return content ?: throw Exception("Could not parse AI response")
    }

    companion object {
        private const val TAG = "DoubaoApiService"
    }
}
