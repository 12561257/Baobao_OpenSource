package com.baobao.fatloss

import com.baobao.fatloss.data.model.CalorieCalc
import org.junit.Test
import org.junit.Assert.*

/**
 * 验证核心能量计算逻辑 (BMR / TDEE / Macros)
 */
class CoreLogicTest {

    @Test
    fun testMaleBMRCalculation() {
        // 男: 10×weight + 6.25×height - 5×age + 5
        // 10*80 + 6.25*180 - 5*25 + 5 = 800 + 1125 - 125 + 5 = 1805
        val bmr = CalorieCalc.calculateBMR(weightKg = 80.0, heightCm = 180, age = 25, gender = 1)
        assertEquals(1805.0, bmr, 0.1)
    }

    @Test
    fun testFemaleBMRCalculation() {
        // 女: 10×weight + 6.25×height - 5×age - 161
        // 10*60 + 6.25*165 - 5*30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        val bmr = CalorieCalc.calculateBMR(weightKg = 60.0, heightCm = 165, age = 30, gender = 2)
        assertEquals(1320.25, bmr, 0.1)
    }

    @Test
    fun testActivityLevelMultipliers() {
        // 1.2, 1.375, 1.55, 1.725
        assertEquals(1.2, CalorieCalc.activityMultiplier(1), 0.01)
        assertEquals(1.375, CalorieCalc.activityMultiplier(2), 0.01)
        assertEquals(1.55, CalorieCalc.activityMultiplier(3), 0.01)
        assertEquals(1.725, CalorieCalc.activityMultiplier(4), 0.01)
    }

    @Test
    fun testDailyBudgetFloor() {
        // 计算预算 = BMR - 400, 但不低于 1200
        // 情况 A: BMR=2000 -> 1600
        assertEquals(1600.0, CalorieCalc.dailyBudget(2000.0), 0.1)
        // 情况 B: BMR=1400 -> 1200 (触发下限)
        assertEquals(1200.0, CalorieCalc.dailyBudget(1400.0), 0.1)
    }

    @Test
    fun testMacroDistribution() {
        // 预算 2000 kcal
        // 碳水 (50%): 1000 kcal / 4 = 250g
        // 蛋白 (25%): 500 kcal / 4 = 125g
        // 脂肪 (25%): 500 kcal / 9 = 55.55g
        val macros = CalorieCalc.macroTargets(2000.0)
        assertEquals(250.0, macros.carbsG, 0.1)
        assertEquals(125.0, macros.proteinG, 0.1)
        assertEquals(55.55, macros.fatG, 0.1)
    }
}
