package com.example.trudomix.data.local

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.trudomix.R

data class AppIcon(val number: Int, val title: String, val resource: Int)

object AppIcons {
    val choices = listOf(
        AppIcon(20, "Голубая TM", R.drawable.launcher_20_tm_light_blue),
        AppIcon(13, "Тёмная TM", R.drawable.launcher_13_tm_dark_classic),
        AppIcon(14, "Светлая TM", R.drawable.launcher_14_tm_light_classic),
        AppIcon(15, "Орбита TM", R.drawable.launcher_15_tm_dark_orbit),
        AppIcon(16, "Пастельная TM", R.drawable.launcher_16_tm_light_pastel),
        AppIcon(17, "Закат TM", R.drawable.launcher_17_tm_dark_sunset),
        AppIcon(18, "Розовая TM", R.drawable.launcher_18_tm_light_pink),
        AppIcon(19, "Синяя TM", R.drawable.launcher_19_tm_dark_cyan),
        AppIcon(21, "Фиолетовая TM", R.drawable.launcher_21_tm_dark_violet),
        AppIcon(22, "Градиентная TM", R.drawable.launcher_22_tm_light_gradient),
        AppIcon(23, "Космическая TM", R.drawable.launcher_23_tm_dark_cosmic),
        AppIcon(24, "Коралловая TM", R.drawable.launcher_24_tm_light_coral),
        AppIcon(1, "Тёмная T", R.drawable.launcher_01_dark_blue_purple),
        AppIcon(2, "Светлая T", R.drawable.launcher_02_light_blue),
        AppIcon(3, "Орбита T", R.drawable.launcher_03_dark_pink_orbit),
        AppIcon(4, "Цветная T", R.drawable.launcher_04_light_multicolor),
        AppIcon(5, "Закат T", R.drawable.launcher_05_dark_sunset),
        AppIcon(6, "Светлый закат T", R.drawable.launcher_06_light_sunset),
        AppIcon(7, "Тёмная синяя T", R.drawable.launcher_07_dark_blue_dot),
        AppIcon(8, "Голубая T", R.drawable.launcher_08_light_blue_dot),
        AppIcon(9, "Тёмные волны T", R.drawable.launcher_09_dark_waves),
        AppIcon(10, "Светлые волны T", R.drawable.launcher_10_light_waves),
        AppIcon(11, "Космос T", R.drawable.launcher_11_dark_orbit),
        AppIcon(12, "Розовые волны T", R.drawable.launcher_12_light_pink_waves)
    )
    val default get() = choices.first { it.number == 9 }
    fun current(context: Context): AppIcon = choices.firstOrNull { icon ->
        val state = context.packageManager.getComponentEnabledSetting(component(context, icon))
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon.number == 9)
    } ?: default

    fun select(context: Context, icon: AppIcon) {
        require(icon in choices)
        val manager = context.packageManager
        fun state(item: AppIcon) = if (item == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (Build.VERSION.SDK_INT >= 33) {
            manager.setComponentEnabledSettings(choices.map {
                PackageManager.ComponentEnabledSetting(component(context, it), state(it), PackageManager.DONT_KILL_APP)
            })
        } else {
            // Enable the new entry first so a failed update cannot remove the app from the launcher.
            manager.setComponentEnabledSetting(component(context, icon), state(icon), PackageManager.DONT_KILL_APP)
            choices.filter { it != icon }.forEach { manager.setComponentEnabledSetting(component(context, it), state(it), PackageManager.DONT_KILL_APP) }
        }
        context.getSharedPreferences("app_icon", Context.MODE_PRIVATE).edit().putInt("chosen", icon.number).apply()
    }
    fun initialize(context: Context) {
        val saved = context.getSharedPreferences("app_icon", Context.MODE_PRIVATE).getInt("chosen", 0)
        val old = choices.firstOrNull { context.packageManager.getComponentEnabledSetting(component(context,it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        val selected = choices.firstOrNull { it.number == saved } ?: old?.takeIf { it.number != 20 } ?: default
        if (old != selected || choices.count { icon ->
            val state = context.packageManager.getComponentEnabledSetting(component(context,icon))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon.number == 9)
        } != 1) select(context, selected)
    }
    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, "${context.packageName}.Launcher${icon.number}")
}
