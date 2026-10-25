package com.example.trudomix

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.AppIcons
import org.junit.Assert.*
import org.junit.Test

class AppIconTest {
    @Test fun everySuppliedVectorRendersWithoutResourceTruncation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (icon in AppIcons.choices) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, icon.resource)!!
            assertTrue("${icon.title} must use a vector", drawable is android.graphics.drawable.VectorDrawable)
            val bitmap = android.graphics.Bitmap.createBitmap(128,128,android.graphics.Bitmap.Config.ARGB_8888)
            drawable.setBounds(0,0,128,128)
            drawable.draw(android.graphics.Canvas(bitmap))
            val pixels=IntArray(128*128);bitmap.getPixels(pixels,0,128,0,0,128,128)
            assertTrue("${icon.title} must retain its artwork",pixels.toSet().size>4)
            bitmap.recycle()
        }
    }
    @Test fun selectingIconKeepsExactlyOneLauncherAndCanBeRestored() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = AppIcons.chosen(context)
        val alternative = AppIcons.choices.first { it.color=="blue"&&it.number != original.number }
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
