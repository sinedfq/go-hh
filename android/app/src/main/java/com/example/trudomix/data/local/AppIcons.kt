package com.example.trudomix.data.local

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.trudomix.R

data class AppIcon(val number: Int, val title: String, val resource: Int)

object AppIcons {
    val choices = listOf(
        AppIcon(9, "Тёмные волны T", R.drawable.launcher_09_dark_waves),
        AppIcon(1, "Классика · ночная", R.drawable.launcher_01_dark_blue_purple),
        AppIcon(2, "Светлая классика · белая", R.drawable.launcher_02_light_blue),
        AppIcon(3, "Волна · тёмная", R.drawable.launcher_03_dark_pink_orbit),
        AppIcon(4, "Синяя T · голубая", R.drawable.launcher_04_light_multicolor),
        AppIcon(5, "Классика · коралловая", R.drawable.launcher_05_dark_sunset),
        AppIcon(6, "Светлая классика · кремовая", R.drawable.launcher_06_light_sunset),
        AppIcon(7, "Синяя T · ночная", R.drawable.launcher_07_dark_blue_dot),
        AppIcon(8, "Светлая синяя T · голубая", R.drawable.launcher_08_light_blue_dot),
        AppIcon(10, "Лента · белая", R.drawable.launcher_10_light_waves),
        AppIcon(11, "Самолёт · ночная", R.drawable.launcher_11_dark_orbit),
        AppIcon(12, "Волна · коралловая", R.drawable.launcher_12_light_pink_waves),
        AppIcon(13, "Самолёт · тёмная", R.drawable.launcher_13_tm_dark_classic),
        AppIcon(14, "Светлый самолёт · белая", R.drawable.launcher_14_tm_light_classic),
        AppIcon(15, "Светлый самолёт · фиолетовая", R.drawable.launcher_15_tm_dark_orbit),
        AppIcon(16, "Значок · кремовая", R.drawable.launcher_16_tm_light_pastel),
        AppIcon(17, "Самолёт · коралловая", R.drawable.launcher_17_tm_dark_sunset),
        AppIcon(18, "Значок · коралловая", R.drawable.launcher_18_tm_light_pink),
        AppIcon(19, "Синяя T · тёмная", R.drawable.launcher_19_tm_dark_cyan),
        AppIcon(20, "Синяя T · белая", R.drawable.launcher_20_tm_light_blue),
        AppIcon(21, "Классика · фиолетовая", R.drawable.launcher_21_tm_dark_violet),
        AppIcon(22, "Волна · мятная", R.drawable.launcher_22_tm_light_gradient),
        AppIcon(23, "Лента · ночная", R.drawable.launcher_23_tm_dark_cosmic),
        AppIcon(24, "Светлый самолёт · коралловая", R.drawable.launcher_24_tm_light_coral),
        AppIcon(101, "Классика · тёмная", R.drawable.launcher_101),
        AppIcon(102, "Классика · голубая", R.drawable.launcher_102),
        AppIcon(103, "Классика · кремовая", R.drawable.launcher_103),
        AppIcon(104, "Классика · мятная", R.drawable.launcher_104),
        AppIcon(105, "Классика · белая", R.drawable.launcher_105),
        AppIcon(106, "Светлая классика · тёмная", R.drawable.launcher_106),
        AppIcon(107, "Светлая классика · голубая", R.drawable.launcher_107),
        AppIcon(108, "Светлая классика · коралловая", R.drawable.launcher_108),
        AppIcon(109, "Светлая классика · мятная", R.drawable.launcher_109),
        AppIcon(110, "Светлая классика · ночная", R.drawable.launcher_110),
        AppIcon(111, "Светлая классика · фиолетовая", R.drawable.launcher_111),
        AppIcon(112, "Волна · голубая", R.drawable.launcher_112),
        AppIcon(113, "Волна · кремовая", R.drawable.launcher_113),
        AppIcon(114, "Волна · ночная", R.drawable.launcher_114),
        AppIcon(115, "Волна · фиолетовая", R.drawable.launcher_115),
        AppIcon(116, "Волна · белая", R.drawable.launcher_116),
        AppIcon(117, "Синяя T · коралловая", R.drawable.launcher_117),
        AppIcon(118, "Синяя T · кремовая", R.drawable.launcher_118),
        AppIcon(119, "Синяя T · мятная", R.drawable.launcher_119),
        AppIcon(120, "Синяя T · фиолетовая", R.drawable.launcher_120),
        AppIcon(121, "Светлая синяя T · тёмная", R.drawable.launcher_121),
        AppIcon(122, "Светлая синяя T · коралловая", R.drawable.launcher_122),
        AppIcon(123, "Светлая синяя T · кремовая", R.drawable.launcher_123),
        AppIcon(124, "Светлая синяя T · мятная", R.drawable.launcher_124),
        AppIcon(125, "Светлая синяя T · ночная", R.drawable.launcher_125),
        AppIcon(126, "Светлая синяя T · фиолетовая", R.drawable.launcher_126),
        AppIcon(127, "Светлая синяя T · белая", R.drawable.launcher_127),
        AppIcon(128, "Лента · голубая", R.drawable.launcher_128),
        AppIcon(129, "Лента · коралловая", R.drawable.launcher_129),
        AppIcon(130, "Лента · кремовая", R.drawable.launcher_130),
        AppIcon(131, "Лента · мятная", R.drawable.launcher_131),
        AppIcon(132, "Лента · фиолетовая", R.drawable.launcher_132),
        AppIcon(133, "Самолёт · голубая", R.drawable.launcher_133),
        AppIcon(134, "Самолёт · кремовая", R.drawable.launcher_134),
        AppIcon(135, "Самолёт · мятная", R.drawable.launcher_135),
        AppIcon(136, "Самолёт · фиолетовая", R.drawable.launcher_136),
        AppIcon(137, "Самолёт · белая", R.drawable.launcher_137),
        AppIcon(138, "Светлый самолёт · тёмная", R.drawable.launcher_138),
        AppIcon(139, "Светлый самолёт · голубая", R.drawable.launcher_139),
        AppIcon(140, "Светлый самолёт · кремовая", R.drawable.launcher_140),
        AppIcon(141, "Светлый самолёт · мятная", R.drawable.launcher_141),
        AppIcon(142, "Светлый самолёт · ночная", R.drawable.launcher_142),
        AppIcon(143, "Значок · тёмная", R.drawable.launcher_143),
        AppIcon(144, "Значок · голубая", R.drawable.launcher_144),
        AppIcon(145, "Значок · мятная", R.drawable.launcher_145),
        AppIcon(146, "Значок · ночная", R.drawable.launcher_146),
        AppIcon(147, "Значок · фиолетовая", R.drawable.launcher_147),
        AppIcon(148, "Значок · белая", R.drawable.launcher_148)
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
