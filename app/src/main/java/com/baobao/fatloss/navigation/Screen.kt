package com.baobao.fatloss.navigation

/**
 * 应用内所有路由定义。
 * 每个子类对应一个页面目的地。
 */
sealed class Screen(val route: String) {
    /** 首页 */
    data object Home : Screen("home")

    /** 拍照识别 */
    data object Camera : Screen("camera")

    /** 饮食记录 */
    data object MealLog : Screen("meal_log")

    /** 进度统计 */
    data object Progress : Screen("progress")

    /** AI 聊天 */
    data object AiChat : Screen("ai_chat")

    /** 个人中心 */
    data object Profile : Screen("profile")

    /** 记忆管理 */
    data object MemoryManagement : Screen("memory_management")

    /** API Key 设置 */
    data object ApiKeySetup : Screen("api_key_setup")
}
