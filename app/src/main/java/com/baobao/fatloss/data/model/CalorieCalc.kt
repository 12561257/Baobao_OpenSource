package com.baobao.fatloss.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

object CalorieCalc {
    /**
     * Mifflin-St Jeor 公式计算 BMR
     * @param gender 1=男 2=女
     * 男: BMR = 10×weight + 6.25×height - 5×age + 5
     * 女: BMR = 10×weight + 6.25×height - 5×age - 161
     */
    fun calculateBMR(weightKg: Double, heightCm: Int, age: Int, gender: Int = 1): Double {
        return if (gender == 2) {
            // 女性
            10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161.0
        } else {
            // 男性
            10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5.0
        }
    }

    fun activityMultiplier(level: Int): Double = when (level) {
        1 -> 1.2    // 久坐
        2 -> 1.375  // 轻度
        3 -> 1.55   // 中度
        4 -> 1.725  // 重度
        else -> 1.375
    }

    fun calculateTDEE(bmr: Double, activityLevel: Int): Double {
        return bmr * activityMultiplier(activityLevel)
    }

    /**
     * 每日热量预算 = BMR - 400
     * 節食通用缺口 400 大卡，约每周减 0.4kg 脂肪）
     * 不低于 1200 大卡安全线。
     */
    fun dailyBudget(bmr: Double): Double {
        return max(bmr - 400.0, 1200.0)
    }

    /** 兼容旧签名的调用方式 */
    fun dailyBudget(tdee: Double, currentWeight: Double, targetWeight: Double, targetDate: LocalDate): Double {
        return max(tdee - 400.0, 1200.0)
    }

    fun bmi(weightKg: Double, heightCm: Int): Double {
        val heightM = heightCm / 100.0
        return if (heightM > 0) weightKg / (heightM * heightM) else 0.0
    }

    /** 营养素目标：碳水50%，蛋白质25%，脂肪25% */
    data class MacroTargets(val carbsG: Double, val proteinG: Double, val fatG: Double)

    fun macroTargets(budget: Double): MacroTargets {
        val carbsCalories = budget * 0.50
        val proteinCalories = budget * 0.25
        val fatCalories = budget * 0.25
        return MacroTargets(
            carbsG = carbsCalories / 4.0,
            proteinG = proteinCalories / 4.0,
            fatG = fatCalories / 9.0
        )
    }
}
