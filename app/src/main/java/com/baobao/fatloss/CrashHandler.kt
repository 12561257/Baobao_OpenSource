package com.baobao.fatloss

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获器。
 * 当 App 发生未捕获异常（闪退）时，将完整堆栈写入本地文件，
 * 下次启动时可以读取并展示给用户/开发者。
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val CRASH_FILE_NAME = "last_crash.txt"

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }

        /** 读取上一次崩溃日志（如果有） */
        fun getLastCrash(context: Context): String? {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            return if (file.exists()) file.readText() else null
        }

        /** 清除崩溃日志 */
        fun clearCrash(context: Context) {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) file.delete()
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val report = buildString {
                appendLine("=== 崩溃报告 ===")
                appendLine("时间: $time")
                appendLine("线程: ${thread.name}")
                appendLine("异常: ${throwable.javaClass.name}")
                appendLine("消息: ${throwable.message}")
                appendLine()
                appendLine("--- 完整堆栈 ---")
                appendLine(stackTrace)
            }

            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(report)
        } catch (_: Exception) {
            // 写日志本身出错，无能为力
        }

        // 交给系统默认行为（终止进程）
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
