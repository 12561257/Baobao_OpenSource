package com.baobao.fatloss.util

object MockData {
    // 用户资料
    const val userName = "小明"
    const val userHeightCm = 175
    const val currentWeight = 76.8
    const val targetWeight = 70.0
    const val initialWeight = 79.0
    const val dailyBudget = 1850.0

    // 今日热量
    const val consumedCalories = 1200.0
    const val burnedCalories = 200.0
    val remainingCalories = dailyBudget - consumedCalories + burnedCalories  // 850

    // 营养素
    data class Macro(val consumed: Double, val target: Double, val name: String)
    val carbs = Macro(120.0, 200.0, "碳水")
    val protein = Macro(68.0, 90.0, "蛋白质")
    val fat = Macro(35.0, 55.0, "脂肪")

    // 步数 & 饮水
    const val todaySteps = 6842
    const val stepsTarget = 8000
    const val waterMl = 1200
    const val waterTarget = 2000

    // 周报
    const val weeklyWeightChange = -0.3
    const val weeklyAvgCalories = 1680.0
    const val weeklyAdherenceRate = 85  // percent

    // AI 洞察
    const val aiInsight = "小明，你的蛋白质摄入持续偏低。今晚试试鸡胸肉沙拉？补充优质蛋白可以帮助维持肌肉量，提高基础代谢。"

    // 食物记录
    data class FoodItem(val name: String, val weight: String, val calories: Int)

    val breakfastFoods = listOf(
        FoodItem("全麦面包 ×2", "120g", 180),
        FoodItem("煮鸡蛋 ×1", "60g", 78),
        FoodItem("脱脂牛奶 250ml", "250ml", 90),
    )
    val lunchFoods = listOf(
        FoodItem("红烧肉", "280g", 380),
        FoodItem("米饭", "150g", 170),
        FoodItem("青菜", "100g", 30),
    )
    val snackFoods = listOf(
        FoodItem("苹果 1个", "200g", 52),
    )

    // 体重趋势（30天，从79到76.8的下降）
    val weightTrend = listOf(
        79.0, 78.8, 78.9, 78.6, 78.5, 78.3, 78.4, 78.1, 78.0, 78.2,
        77.9, 77.8, 77.7, 77.9, 77.6, 77.5, 77.3, 77.4, 77.2, 77.1,
        77.0, 77.2, 76.9, 76.8, 77.0, 76.7, 76.9, 76.8, 76.7, 76.8
    )

    // 聊天消息
    data class ChatMessage(val role: String, val content: String, val timestamp: String)
    val chatMessages = listOf(
        ChatMessage("assistant", "小明，下午好。今天已摄入 1,200 大卡，还剩 650 大卡额度。晚餐有什么安排？", "14:30"),
        ChatMessage("user", "室友说要去吃火锅", "14:31"),
        ChatMessage("assistant", "没关系，火锅也能吃得健康。建议：\n1. 选清汤锅底，避开麻辣和牛油锅\n2. 多吃蔬菜和豆腐，饱腹感强\n3. 肉类选牛肉和虾，少选五花肉\n4. 蘸料用酱油+醋，避开花生酱\n5. 预计控制在 500 大卡内完全可行", "14:31"),
    )

    // 成就
    data class Achievement(val emoji: String, val title: String, val subtitle: String)
    val achievements = listOf(
        Achievement("\uD83D\uDD25", "连续打卡 23 天", "Streak Record"),
        Achievement("\uD83C\uDFAF", "周达标率 85%", "Weekly Target"),
        Achievement("\u26A1", "累计减重 2.2 kg", "Total Lost"),
    )

    // 统计
    const val bmi = 25.1
    const val bodyFatPercent = 22.3
    const val daysStreak = 23

    // 识别食物（拍照页）
    val recognizedFoods = listOf(
        FoodItem("红烧肉", "280g", 380),
        FoodItem("米饭", "150g", 170),
        FoodItem("青菜", "100g", 30),
    )
    const val aiFoodComment = "蛋白质充足，但红烧肉脂肪偏高。晚餐建议选择清蒸或水煮方式，补充绿叶蔬菜。"

    // 周报详细
    const val weeklyOnTrackDays = 5
    const val weeklyBurnedCalories = 1200
}
