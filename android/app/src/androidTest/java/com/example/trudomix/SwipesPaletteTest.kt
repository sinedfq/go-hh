package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.data.local.*
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.screens.home.*
import com.example.trudomix.ui.screens.settings.AccentPalette
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

private class FakeSwipeSource:SwipeDataSource{
    val changes=mutableListOf<Pair<Int,Boolean>>()
    var fail=false
    override suspend fun page(offset:Int)=VacancySearchResponse(vacancies=if(offset==0)(1..5).map{Vacancy(id=it,title="Android-разработчик $it",company="Трудомикс",location="Москва",experience="Middle",skills=listOf("Kotlin","Jetpack Compose","SQL"),salary_from=180000,salary_to=260000,remote=true,description="## О команде\nСоздаём удобные продукты. Ищем разработчика, которому важны качество и люди.")}else emptyList(),total_count=5)
    override suspend fun favorites()=listOf(Vacancy(id=1))
    override suspend fun favorite(id:Int,selected:Boolean){if(fail)throw java.io.IOException("offline");changes+=id to selected}
}

class SwipesPaletteTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun swipesUndoKeepExistingFavoritesAndFailuresKeepTheCard(){
        val source=FakeSwipeSource();lateinit var model:SwipeViewModel
        compose.runOnUiThread{model=SwipeViewModel(source)}
        compose.waitUntil(5000){!model.loading&&model.items.size==5}
        compose.setContent{TrudomixTheme{SwipeScreen({},model)}}
        shot("swipes-new.png")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5000){model.reviewed==1}
        compose.onNodeWithContentDescription("Вернуть последнюю вакансию").performClick()
        compose.waitUntil(5000){model.reviewed==0}
        assertTrue("Undo must retain a pre-existing favorite",source.changes.isEmpty())
        compose.onNodeWithText("Пропустить").performClick()
        compose.waitUntil(5000){model.items.firstOrNull()?.id==2}
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5000){model.items.firstOrNull()?.id==3}
        compose.onNodeWithContentDescription("Вернуть последнюю вакансию").performClick()
        compose.waitUntil(5000){model.items.firstOrNull()?.id==2}
        assertEquals(listOf(2 to true,2 to false),source.changes)
        val left=compose.onNodeWithTag("swipe-card").getUnclippedBoundsInRoot().left
        compose.onNodeWithTag("swipe-card").performTouchInput{down(center);moveBy(Offset(30f,0f));up()}
        compose.waitForIdle()
        assertEquals(left.value,compose.onNodeWithTag("swipe-card").getUnclippedBoundsInRoot().left.value,1f)
        source.fail=true
        compose.onNodeWithTag("swipe-card").performTouchInput{swipeRight(durationMillis=300)}
        compose.waitUntil(5000){model.error!=null}
        compose.waitForIdle()
        assertEquals(2,model.items.first().id)
        assertEquals(1,model.reviewed)
    }
    @Test fun paletteOnlyAppliesOnConfirmationAndPersistsCustomColor(){
        val context=compose.activity.applicationContext
        val original=AccentColors.selected.value;val custom=AccentColors.custom.value
        try{
            compose.setContent{TrudomixTheme{AccentPalette()}}
            compose.onNodeWithText("Открыть палитру").performClick()
            compose.onNodeWithText("HEX").performTextReplacement("#12ABEF")
            assertEquals(custom,AccentColors.custom.value)
            compose.onNodeWithText("Отмена").performClick()
            assertEquals(custom,AccentColors.custom.value)
            compose.onNodeWithText("Открыть палитру").performClick()
            compose.onNodeWithText("HEX").performTextReplacement("#12ABEF")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            shot("accent-palette-dialog.png")
            compose.onNodeWithText("Применить").performClick()
            compose.runOnIdle{assertEquals("#12ABEF",accentHex(AccentColors.custom.value!!));AccentColors.initialize(context);assertEquals("#12ABEF",accentHex(AccentColors.custom.value!!))}
            compose.onNodeWithContentDescription("Синий").performClick()
            compose.runOnIdle{assertNull(AccentColors.custom.value);assertEquals(AccentColor.BLUE,AccentColors.selected.value)}
            shot("accent-palette-presets.png")
        }finally{AccentColors.select(context,original);custom?.let{AccentColors.selectCustom(context,it)}}
    }
    private fun shot(name:String){
        val bitmap=compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
