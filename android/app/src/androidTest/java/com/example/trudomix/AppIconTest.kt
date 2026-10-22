package com.example.trudomix

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.AppIcons
import org.junit.Assert.*
import org.junit.Test

class AppIconTest {
    @Test fun selectingIconKeepsExactlyOneLauncherAndCanBeRestored() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = AppIcons.current(context)
        val alternative = AppIcons.choices.first { it.number != original.number }
        try {
            AppIcons.select(context, alternative)
            assertEquals(alternative, AppIcons.current(context))
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
            @Suppress("DEPRECATION") val activities = context.packageManager.queryIntentActivities(launcher,0)
            assertEquals(1, activities.size)
            assertEquals("${context.packageName}.Launcher${alternative.number}", activities.single().activityInfo.name)
            assertNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        } finally { AppIcons.select(context, original) }
    }
}
