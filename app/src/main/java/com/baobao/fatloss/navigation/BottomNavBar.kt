package com.baobao.fatloss.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.stringResource
import com.baobao.fatloss.R
import com.baobao.fatloss.ui.theme.Dimen
import com.baobao.fatloss.ui.theme.FusionBackground
import com.baobao.fatloss.ui.theme.TextPrimary
import com.baobao.fatloss.ui.theme.TextTertiary

/**
 * 底部导航栏各 Tab 定义
 */
private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

@Composable
private fun rememberBottomNavItems(): List<BottomNavItem> {
    return listOf(
        BottomNavItem(Screen.Home, Icons.Filled.Home, stringResource(R.string.nav_home)),
        BottomNavItem(Screen.MealLog, Icons.Filled.Restaurant, stringResource(R.string.nav_meal_log)),
        BottomNavItem(Screen.Progress, Icons.AutoMirrored.Filled.TrendingUp, stringResource(R.string.nav_progress)),
        BottomNavItem(Screen.AiChat, Icons.AutoMirrored.Filled.Chat, stringResource(R.string.nav_ai_chat)),
        BottomNavItem(Screen.Profile, Icons.Filled.Person, stringResource(R.string.nav_profile)),
    )
}

/**
 * 应用底部导航栏。
 *
 * @param navController   导航控制器，用于执行页面跳转
 * @param currentRoute    当前页面路由，用于高亮当前 Tab
 */
@Composable
fun BottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    val bottomNavItems = rememberBottomNavItems()
    NavigationBar(
        containerColor = FusionBackground,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            // 避免重复堆积同一目的地
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(text = item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = TextPrimary,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor = FusionBackground,
                ),
            )
        }
    }
}
