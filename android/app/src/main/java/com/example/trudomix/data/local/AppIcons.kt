package com.example.trudomix.data.local

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.trudomix.R
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow

data class AppIcon(val number: Int, val title: String, val resource: Int, val design:Int=10, val color:String="black")
data class IconAppearance(val icon:AppIcon,val followTheme:Boolean=true)

object AppIcons {
    val nightWave = AppIcon(201, "Ночная волна", R.drawable.trudomix_wave_night, design=10, color="black")
    val lightWave = AppIcon(202, "Белая волна", R.drawable.trudomix_wave_light, design=10, color="white")
    fun waveForTheme(dark: Boolean) = if (dark) nightWave else lightWave
    val choices = listOf(nightWave, lightWave)
    val default get() = nightWave
    val appearance=MutableStateFlow(IconAppearance(default,followTheme=false))
    fun chosen(context:Context):AppIcon=choices.firstOrNull{it.number==context.getSharedPreferences("app_icon",Context.MODE_PRIVATE).getInt("chosen",default.number)}?:default
    // Retired IDs fall back to Night Wave; an explicit theme preference is kept.
    fun forTheme(icon:AppIcon,dark:Boolean,followTheme:Boolean=true):AppIcon {
        if(!followTheme)return icon
        return waveForTheme(dark)
    }
    fun current(context:Context):AppIcon {
        val prefs=context.getSharedPreferences("app_icon",Context.MODE_PRIVATE)
        val icon=chosen(context)
        return forTheme(icon,resolvedDark(context),prefs.getBoolean("follow_theme",false))
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
        val follow=prefs.getBoolean("follow_theme",false)
        appearance.value=IconAppearance(selected,follow)
        prefs.edit().putBoolean("last_dark",dark).apply()
        val icon=forTheme(selected,dark,follow)
        val manager = context.packageManager
        val active=choices.filter{val value=manager.getComponentEnabledSetting(component(context,it));value==PackageManager.COMPONENT_ENABLED_STATE_ENABLED||(value==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&it.number==default.number)}
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
