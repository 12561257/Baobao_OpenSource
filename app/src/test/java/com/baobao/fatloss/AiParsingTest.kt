package com.baobao.fatloss

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

/**
 * 模拟测试 AiRepository 内部使用的正则表达式和 JSON 解析逻辑。
 * 确保能够正确从 AI 的文本回复中提取结构化指令。
 */
class AiParsingTest {

    private val foodLogRegex = Regex("""\[FOOD_LOG\](.*?)\[/FOOD_LOG\]""", RegexOption.DOT_MATCHES_ALL)
    private val deleteRegex = Regex("""\[DELETE_FOOD\](.*?)\[/DELETE_FOOD\]""", RegexOption.DOT_MATCHES_ALL)
    private val exerciseRegex = Regex("""\[EXERCISE\](.*?)\[/EXERCISE\]""", RegexOption.DOT_MATCHES_ALL)

    @Test
    fun testParseFoodLog() {
        val aiReply = "好的，我帮你记录了：[FOOD_LOG]{\"meal\":\"lunch\",\"foods\":[{\"name\":\"煎饼果子\",\"calories\":450,\"carbs\":60,\"protein\":15,\"fat\":18}]}[/FOOD_LOG]"
        
        val match = foodLogRegex.find(aiReply)
        assertNotNull("应该匹配到 FOOD_LOG 标签", match)
        
        val jsonStr = match!!.groupValues[1]
        val json = JSONObject(jsonStr)
        
        assertEquals("lunch", json.getString("meal"))
        val foods = json.getJSONArray("foods")
        assertEquals(1, foods.length())
        assertEquals("煎饼果子", foods.getJSONObject(0).getString("name"))
        assertEquals(450.0, foods.getJSONObject(0).getDouble("calories"), 0.1)
    }

    @Test
    fun testParseExercise() {
        val aiReply = "太棒了！[EXERCISE]{\"name\":\"慢跑\",\"calories_burned\":300}[/EXERCISE] 这里的热量我记下了。"
        
        val match = exerciseRegex.find(aiReply)
        assertNotNull("应该匹配到 EXERCISE 标签", match)
        
        val json = JSONObject(match!!.groupValues[1])
        assertEquals("慢跑", json.getString("name"))
        assertEquals(300.0, json.getDouble("calories_burned"), 0.1)
    }

    @Test
    fun testParseMultipleActions() {
        val aiReply = """
            首先记录午餐：
            [FOOD_LOG]{"meal":"lunch","foods":[{"name":"沙拉","calories":200}]}[/FOOD_LOG]
            然后记录运动：
            [EXERCISE]{"name":"深蹲","calories_burned":50}[/EXERCISE]
        """.trimIndent()
        
        val foodMatches = foodLogRegex.findAll(aiReply).toList()
        assertEquals(1, foodMatches.size)
        
        val exMatches = exerciseRegex.findAll(aiReply).toList()
        assertEquals(1, exMatches.size)
    }

    @Test
    fun testMarkdownClean() {
        // 模拟 AI 有时会把 JSON 包裹在 Markdown 代码块里的情况
        val rawJson = "```json\n{\"meal\":\"snack\",\"foods\":[]}\n```"
        val cleanJson = rawJson.replace(Regex("```json|```", RegexOption.IGNORE_CASE), "").trim()
        
        val json = JSONObject(cleanJson)
        assertEquals("snack", json.getString("meal"))
    }
}
