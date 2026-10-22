package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.VacancyCard
import com.example.trudomix.ui.screens.auth.*
import com.example.trudomix.ui.screens.employer.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class WorkspacePolishTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test fun authBackgroundMovesInLoginAndRegistration(){
        val register=mutableStateOf(false)
        compose.mainClock.autoAdvance=false
        compose.setContent{TrudomixTheme(darkTheme=register.value){if(register.value)RegisterContent(AuthUiState.Idle,{_,_,_->},{})else LoginContent(AuthUiState.Idle,{_,_->},{})}}
        for(registration in listOf(false,true)){
            compose.runOnUiThread{register.value=registration}
            compose.mainClock.advanceTimeBy(1200);compose.waitForIdle()
            val first=compose.onRoot().captureToImage().asAndroidBitmap()
            compose.mainClock.advanceTimeBy(4000);compose.waitForIdle()
            val second=compose.onRoot().captureToImage().asAndroidBitmap()
            var difference=0
            for(y in 10 until first.height-10 step 30){val a=first.getPixel(5,y);val b=second.getPixel(5,y);difference+=kotlin.math.abs(android.graphics.Color.blue(a)-android.graphics.Color.blue(b))+kotlin.math.abs(android.graphics.Color.red(a)-android.graphics.Color.red(b))}
            assertTrue("Ambient gradient must visibly change: $difference",difference>50)
            capture(if(registration)"register-animated-dark"else "login-animated-light")
            first.recycle();second.recycle()
        }
    }

    @Test fun vacancyContentUsesOneLeadingEdge(){
        compose.setContent{TrudomixTheme{Column(Modifier.padding(20.dp)){VacancyCard(Vacancy(id=1,title="Разработчик",company="Команда",salary="Доход",skills=listOf("Kotlin")),{})}}}
        val title=compose.onNodeWithText("Разработчик",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.left
        val salary=compose.onNodeWithText("Доход",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.left
        val skill=compose.onNodeWithText("Kotlin",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.left
        assertEquals(title,salary,1f)
        assertTrue("Skill has only its own chip padding",skill-title in 0f..35f)
        capture("vacancy-alignment")
    }

    @Test fun employerFiltersAndConfirmationPreserveActions(){
        var archived=0;var status=""
        val tab=mutableIntStateOf(0)
        val state=EmployerState(company=Company(id=1,name="Команда"),loading=false,vacancies=listOf(Vacancy(id=1,title="Инженер",company="Команда"),Vacancy(id=2,title="Архивная",company="Команда",is_archived=true)),applications=listOf(JobApplication(id=8,resume_full_name="Александр",vacancy_title="Инженер",status="new")))
        compose.setContent{TrudomixTheme(darkTheme=true){EmployerWorkspace(state,tab.intValue,false,{tab.intValue=it},{},{},{},{},{},{archived++},{},{_,value->status=value},{})}}
        compose.onNodeWithText("Архив").performScrollTo().performClick()
        compose.onNodeWithText("Архивная").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Опубликовать").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(1,archived)}
        capture("employer-vacancies-polished")
        compose.onNodeWithText("Отклики").performScrollTo().performClick()
        compose.onNodeWithText("Пригласить").performScrollTo().performClick()
        compose.onNodeWithText("Пригласить кандидата?").assertIsDisplayed()
        compose.onAllNodesWithText("Пригласить").onLast().performClick()
        compose.runOnIdle{assertEquals("accepted",status)}
        capture("employer-applications-polished")
    }

    private fun capture(name:String){val bitmap=compose.onRoot().captureToImage().asAndroidBitmap();val context=InstrumentationRegistry.getInstrumentation().targetContext;File(context.getExternalFilesDir(null),"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
}
