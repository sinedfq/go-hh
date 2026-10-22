package com.example.trudomix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.ui.components.PhotoPicker
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class AvatarEditingTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun pickingPhotoCanBeCancelledAndConfirmedAfterEditing() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"avatar-editing-fixture.jpg")
        val bitmap=Bitmap.createBitmap(900,1500,Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(220,235,250))
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color=Color.rgb(20,100,190);drawRect(0f,0f,450f,1500f,paint)
            paint.color=Color.rgb(255,190,70);drawCircle(450f,750f,240f,paint)
            paint.color=Color.rgb(35,50,70);paint.textSize=80f;drawText("PHOTO",300f,790f,paint)
        }
        file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)}
        bitmap.recycle()
        val calls=AtomicInteger(0)
        val registry=object:ActivityResultRegistry() {
            override fun <I,O> onLaunch(requestCode:Int,contract:ActivityResultContract<I,O>,input:I,options:ActivityOptionsCompat?) {dispatchResult(requestCode,Uri.fromFile(file))}
        }
        val owner=object:ActivityResultRegistryOwner{override val activityResultRegistry=registry}
        compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            TrudomixTheme(darkTheme=false) {PhotoPicker("","Фото профиля") { part ->
                val buffer=okio.Buffer();part.body.writeTo(buffer)
                val bytes=buffer.readByteArray()
                val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                assertNotNull(decoded);assertEquals(decoded.width,decoded.height);assertTrue(decoded.width<=1024)
                assertTrue("Dragging must change the uploaded crop",Color.red(decoded.getPixel(10,decoded.height/2))<80)
                decoded.recycle();calls.incrementAndGet()
            }}
        } }
        compose.onNodeWithText("Добавить фото").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Кадрирование фото").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(0,calls.get())
        Thread.sleep(800)
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null),"avatar-editor-before.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
        compose.onNodeWithContentDescription("Отмена").performClick()
        compose.runOnIdle{assertEquals(0,calls.get())}
        compose.onNodeWithText("Добавить фото").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Кадрирование фото").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Повернуть").performClick()
        compose.waitForIdle()
        compose.waitUntil(5000){compose.onAllNodes(hasText("Сохранить фото") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Масштаб фото").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(1.5f)}
        compose.onNodeWithContentDescription("Круглая область аватарки. Фото можно перемещать и увеличивать.").performTouchInput {
            swipe(center,androidx.compose.ui.geometry.Offset(center.x,center.y+height*.2f),400)
        }
        compose.onNodeWithText("Сохранить фото").performClick()
        compose.waitUntil(10000){calls.get()==1}
        compose.onNodeWithText("Кадрирование фото").assertDoesNotExist()
        file.delete()
    }
}
