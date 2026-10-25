package com.example.trudomix.data.local

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.trudomix.R
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow

data class AppIcon(val number: Int, val title: String, val resource: Int, val design:Int=6, val color:String="black")
data class IconAppearance(val icon:AppIcon,val followTheme:Boolean=true)

object AppIcons {
    val choices = listOf(
        AppIcon(9, "Тёмные волны T", R.drawable.launcher_09_dark_waves, design=6, color="black"),
        AppIcon(1, "Классика · ночная", R.drawable.launcher_01_dark_blue_purple, design=1, color="navy"),
        AppIcon(2, "Светлая классика · белая", R.drawable.launcher_02_light_blue, design=2, color="white"),
        AppIcon(3, "Волна · тёмная", R.drawable.launcher_03_dark_pink_orbit, design=3, color="black"),
        AppIcon(4, "Синяя T · голубая", R.drawable.launcher_04_light_multicolor, design=4, color="blue"),
        AppIcon(5, "Классика · коралловая", R.drawable.launcher_05_dark_sunset, design=1, color="coral"),
        AppIcon(6, "Светлая классика · кремовая", R.drawable.launcher_06_light_sunset, design=2, color="cream"),
        AppIcon(7, "Синяя T · ночная", R.drawable.launcher_07_dark_blue_dot, design=4, color="navy"),
        AppIcon(8, "Светлая синяя T · голубая", R.drawable.launcher_08_light_blue_dot, design=5, color="blue"),
        AppIcon(10, "Лента · белая", R.drawable.launcher_10_light_waves, design=6, color="white"),
        AppIcon(11, "Самолёт · ночная", R.drawable.launcher_11_dark_orbit, design=7, color="navy"),
        AppIcon(12, "Волна · коралловая", R.drawable.launcher_12_light_pink_waves, design=3, color="coral"),
        AppIcon(13, "Самолёт · тёмная", R.drawable.launcher_13_tm_dark_classic, design=7, color="black"),
        AppIcon(14, "Светлый самолёт · белая", R.drawable.launcher_14_tm_light_classic, design=8, color="white"),
        AppIcon(15, "Светлый самолёт · фиолетовая", R.drawable.launcher_15_tm_dark_orbit, design=8, color="violet"),
        AppIcon(16, "Значок · кремовая", R.drawable.launcher_16_tm_light_pastel, design=9, color="cream"),
        AppIcon(17, "Самолёт · коралловая", R.drawable.launcher_17_tm_dark_sunset, design=7, color="coral"),
        AppIcon(18, "Значок · коралловая", R.drawable.launcher_18_tm_light_pink, design=9, color="coral"),
        AppIcon(19, "Синяя T · тёмная", R.drawable.launcher_19_tm_dark_cyan, design=4, color="black"),
        AppIcon(20, "Синяя T · белая", R.drawable.launcher_20_tm_light_blue, design=4, color="white"),
        AppIcon(21, "Классика · фиолетовая", R.drawable.launcher_21_tm_dark_violet, design=1, color="violet"),
        AppIcon(22, "Волна · мятная", R.drawable.launcher_22_tm_light_gradient, design=3, color="mint"),
        AppIcon(23, "Лента · ночная", R.drawable.launcher_23_tm_dark_cosmic, design=6, color="navy"),
        AppIcon(24, "Светлый самолёт · коралловая", R.drawable.launcher_24_tm_light_coral, design=8, color="coral"),
        AppIcon(101, "Классика · тёмная", R.drawable.launcher_101, design=1, color="black"),
        AppIcon(102, "Классика · голубая", R.drawable.launcher_102, design=1, color="blue"),
        AppIcon(103, "Классика · кремовая", R.drawable.launcher_103, design=1, color="cream"),
        AppIcon(104, "Классика · мятная", R.drawable.launcher_104, design=1, color="mint"),
        AppIcon(105, "Классика · белая", R.drawable.launcher_105, design=1, color="white"),
        AppIcon(106, "Светлая классика · тёмная", R.drawable.launcher_106, design=2, color="black"),
        AppIcon(107, "Светлая классика · голубая", R.drawable.launcher_107, design=2, color="blue"),
        AppIcon(108, "Светлая классика · коралловая", R.drawable.launcher_108, design=2, color="coral"),
        AppIcon(109, "Светлая классика · мятная", R.drawable.launcher_109, design=2, color="mint"),
        AppIcon(110, "Светлая классика · ночная", R.drawable.launcher_110, design=2, color="navy"),
        AppIcon(111, "Светлая классика · фиолетовая", R.drawable.launcher_111, design=2, color="violet"),
        AppIcon(112, "Волна · голубая", R.drawable.launcher_112, design=3, color="blue"),
        AppIcon(113, "Волна · кремовая", R.drawable.launcher_113, design=3, color="cream"),
        AppIcon(114, "Волна · ночная", R.drawable.launcher_114, design=3, color="navy"),
        AppIcon(115, "Волна · фиолетовая", R.drawable.launcher_115, design=3, color="violet"),
        AppIcon(116, "Волна · белая", R.drawable.launcher_116, design=3, color="white"),
        AppIcon(117, "Синяя T · коралловая", R.drawable.launcher_117, design=4, color="coral"),
        AppIcon(118, "Синяя T · кремовая", R.drawable.launcher_118, design=4, color="cream"),
        AppIcon(119, "Синяя T · мятная", R.drawable.launcher_119, design=4, color="mint"),
        AppIcon(120, "Синяя T · фиолетовая", R.drawable.launcher_120, design=4, color="violet"),
        AppIcon(121, "Светлая синяя T · тёмная", R.drawable.launcher_121, design=5, color="black"),
        AppIcon(122, "Светлая синяя T · коралловая", R.drawable.launcher_122, design=5, color="coral"),
        AppIcon(123, "Светлая синяя T · кремовая", R.drawable.launcher_123, design=5, color="cream"),
        AppIcon(124, "Светлая синяя T · мятная", R.drawable.launcher_124, design=5, color="mint"),
        AppIcon(125, "Светлая синяя T · ночная", R.drawable.launcher_125, design=5, color="navy"),
        AppIcon(126, "Светлая синяя T · фиолетовая", R.drawable.launcher_126, design=5, color="violet"),
        AppIcon(127, "Светлая синяя T · белая", R.drawable.launcher_127, design=5, color="white"),
        AppIcon(128, "Лента · голубая", R.drawable.launcher_128, design=6, color="blue"),
        AppIcon(129, "Лента · коралловая", R.drawable.launcher_129, design=6, color="coral"),
        AppIcon(130, "Лента · кремовая", R.drawable.launcher_130, design=6, color="cream"),
        AppIcon(131, "Лента · мятная", R.drawable.launcher_131, design=6, color="mint"),
        AppIcon(132, "Лента · фиолетовая", R.drawable.launcher_132, design=6, color="violet"),
        AppIcon(133, "Самолёт · голубая", R.drawable.launcher_133, design=7, color="blue"),
        AppIcon(134, "Самолёт · кремовая", R.drawable.launcher_134, design=7, color="cream"),
        AppIcon(135, "Самолёт · мятная", R.drawable.launcher_135, design=7, color="mint"),
        AppIcon(136, "Самолёт · фиолетовая", R.drawable.launcher_136, design=7, color="violet"),
        AppIcon(137, "Самолёт · белая", R.drawable.launcher_137, design=7, color="white"),
        AppIcon(138, "Светлый самолёт · тёмная", R.drawable.launcher_138, design=8, color="black"),
        AppIcon(139, "Светлый самолёт · голубая", R.drawable.launcher_139, design=8, color="blue"),
        AppIcon(140, "Светлый самолёт · кремовая", R.drawable.launcher_140, design=8, color="cream"),
        AppIcon(141, "Светлый самолёт · мятная", R.drawable.launcher_141, design=8, color="mint"),
        AppIcon(142, "Светлый самолёт · ночная", R.drawable.launcher_142, design=8, color="navy"),
        AppIcon(143, "Значок · тёмная", R.drawable.launcher_143, design=9, color="black"),
        AppIcon(144, "Значок · голубая", R.drawable.launcher_144, design=9, color="blue"),
        AppIcon(145, "Значок · мятная", R.drawable.launcher_145, design=9, color="mint"),
        AppIcon(146, "Значок · ночная", R.drawable.launcher_146, design=9, color="navy"),
        AppIcon(147, "Значок · фиолетовая", R.drawable.launcher_147, design=9, color="violet"),
        AppIcon(148, "Значок · белая", R.drawable.launcher_148, design=9, color="white")
    )
    val default get() = choices.first { it.number == 9 }
    val appearance=MutableStateFlow(IconAppearance(default))
    fun chosen(context:Context):AppIcon=choices.firstOrNull{it.number==context.getSharedPreferences("app_icon",Context.MODE_PRIVATE).getInt("chosen",9)}?:default
    fun forTheme(icon:AppIcon,dark:Boolean,followTheme:Boolean=true):AppIcon {
        if(!followTheme)return icon
        val color=when(icon.color){"black","white"->if(dark)"black"else "white";"navy","cream"->if(dark)"navy"else "cream";else->icon.color}
        return choices.firstOrNull{it.design==icon.design&&it.color==color}?:icon
    }
    fun current(context:Context):AppIcon {
        val prefs=context.getSharedPreferences("app_icon",Context.MODE_PRIVATE)
        return forTheme(chosen(context),resolvedDark(context),prefs.getBoolean("follow_theme",true))
    }
    private fun resolvedDark(context:Context):Boolean=when(ThemeManager.cachedMode(context)){
        AppThemeMode.DARK->true
        AppThemeMode.LIGHT->false
        else->context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES
    }

    fun select(context: Context, icon: AppIcon) {
        require(icon in choices)
        context.getSharedPreferences("app_icon",Context.MODE_PRIVATE).edit().putInt("chosen",icon.number).apply()
        applyTheme(context,context.getSharedPreferences("app_icon",Context.MODE_PRIVATE).getBoolean("last_dark",resolvedDark(context)))
    }
    fun followTheme(context:Context,enabled:Boolean){
        context.getSharedPreferences("app_icon",Context.MODE_PRIVATE).edit().putBoolean("follow_theme",enabled).apply()
        applyTheme(context,context.getSharedPreferences("app_icon",Context.MODE_PRIVATE).getBoolean("last_dark",resolvedDark(context)))
    }
    fun applyTheme(context:Context,dark:Boolean){
        val prefs=context.getSharedPreferences("app_icon",Context.MODE_PRIVATE)
        val selected=chosen(context)
        val follow=prefs.getBoolean("follow_theme",true)
        appearance.value=IconAppearance(selected,follow)
        prefs.edit().putBoolean("last_dark",dark).apply()
        val icon=forTheme(selected,dark,follow)
        val manager = context.packageManager
        val active=choices.filter{val value=manager.getComponentEnabledSetting(component(context,it));value==PackageManager.COMPONENT_ENABLED_STATE_ENABLED||(value==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&it.number==9)}
        if(active.size==1&&active.single()==icon)return
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
    }
    fun initialize(context: Context) {
        applyTheme(context,resolvedDark(context))
    }
    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, "${context.packageName}.Launcher${icon.number}")
}
