package com.example.trudomix

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.ui.components.VacancyCard
import com.example.trudomix.ui.theme.TrudomixTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VacancyPresentationTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun profileReturnsWithSearchAndPullRefreshWorks(){
        var refreshed=0
        var opened=0
        val jobs=(1..30).map{Vacancy(id=it,title="Вакансия $it",company="Команда",location="Новосибирск")}
        compose.setContent {TrudomixTheme(darkTheme=true){com.example.trudomix.ui.components.ProfileAccessHost(0,true,{opened++}){
            com.example.trudomix.ui.screens.vacancies.VacancyFeed(com.example.trudomix.ui.screens.vacancies.VacancyListUiState.Success(jobs),"",false,30,false,null,{}, {}, {}, {refreshed++},{},{},{_,_->})
        }}}
        val grid=compose.onNode(hasScrollToIndexAction())
        repeat(3){grid.performTouchInput{swipeUp()}}
        grid.performTouchInput{swipeDown()}
        try{compose.waitUntil(10000){compose.onAllNodesWithTag("floating-profile-action").fetchSemanticsNodes().isNotEmpty()}}catch(e:Throwable){
            File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"profile-floating-tree.txt").writeText(compose.onRoot().printToString());throw e
        }
        compose.onNodeWithTag("floating-profile-action").assertIsDisplayed()
        compose.onNodeWithTag("floating-vacancy-search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Профиль").performClick()
        assertEquals(1,opened)
        grid.performScrollToIndex(0)
        grid.performTouchInput{swipeDown(startY=100f,endY=height*.8f,durationMillis=600)}
        compose.waitUntil(10000){refreshed>0}
    }
    @Test fun salaryAndInlineWorkFormatFitNarrowCard() {
        val v=Vacancy(title="Android-разработчик",company="Команда Trudomix",location="Новосибирск",experience="Middle",remote=true,salary_from=120000,salary_to=180000,employment="full_time",schedule="flexible",company_it_accredited=true)
        compose.setContent { TrudomixTheme(darkTheme=true) { Surface(color=MaterialTheme.colorScheme.background) { Box(Modifier.width(340.dp).padding(12.dp)) { VacancyCard(v,{}) } } } }
        compose.onNodeWithText("Удалённо").assertIsDisplayed()
        compose.onNodeWithText("Полная занятость · Гибкий график").assertIsDisplayed()
        compose.onNodeWithContentDescription("IT-аккредитация указана компанией").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("IT-аккредитация указана компанией").assertCountEquals(1)
        val city=compose.onNodeWithText("Новосибирск").fetchSemanticsNode().boundsInRoot
        val remote=compose.onNodeWithText("Удалённо").fetchSemanticsNode().boundsInRoot
        assertTrue("Metadata should stay compact when wrapping",remote.top-city.top<100)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        compose.waitForIdle()
        Thread.sleep(1200)
        val screenshot=compose.onRoot().captureToImage().asAndroidBitmap()
        File(instrumentation.targetContext.getExternalFilesDir(null),"vacancy-terms-dark.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) };screenshot.recycle()
    }
    @Test fun exportedPdfOpensInAndroidRenderer() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("livePdf")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.getExternalFilesDir(null),"resume-export-preview.pdf")
        ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertTrue(renderer.pageCount>1)
                renderer.openPage(0).use { page ->
                    val bitmap=Bitmap.createBitmap(page.width*2,page.height*2,Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    File(context.getExternalFilesDir(null),"resume-pdf-page.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
                }
            }
        }
    }
}
