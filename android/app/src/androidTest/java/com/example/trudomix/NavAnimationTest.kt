package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.*
import com.example.trudomix.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class NavAnimationTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test fun stylesHandleAdjacentDistantAndRapidTapsThenStop(){
        compose.mainClock.autoAdvance=false
        val selected=mutableIntStateOf(0);val style=mutableStateOf(NavAnimationStyle.SMOOTH)
        lateinit var motion:NavHighlightState
        compose.setContent{motion=rememberNavHighlight(selected.intValue,style.value);Box(Modifier.size(320.dp,72.dp).testTag("nav").navHighlight(motion,Color.Black,Color.Cyan,Color.Gray))}
        compose.mainClock.advanceTimeBy(64)
        for(choice in NavAnimationStyle.entries){
            compose.runOnIdle{style.value=choice;selected.intValue=0};compose.mainClock.advanceTimeBy(64)
            compose.runOnIdle{selected.intValue=1};compose.mainClock.advanceTimeBy(96)
            if(choice!=NavAnimationStyle.NONE)compose.runOnIdle{assertTrue(motion.reveal.value<1f)}
            compose.runOnIdle{selected.intValue=4};compose.mainClock.advanceTimeBy(64)
            for(target in listOf(0,3,1,4,2)){compose.runOnIdle{selected.intValue=target};compose.mainClock.advanceTimeBy(48)}
            compose.mainClock.advanceTimeBy(3000)
            compose.runOnIdle{assertEquals(2,motion.slot);assertEquals(2f,motion.position.value,.001f);assertEquals(1f,motion.reveal.value,0f);assertFalse(motion.position.isRunning);assertFalse(motion.reveal.isRunning);assertFalse(motion.crest.isRunning)}
            val bitmap=compose.onNodeWithTag("nav").captureToImage().asAndroidBitmap()
            File(compose.activity.getExternalFilesDir(null),"nav-${choice.id}-final.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
        compose.runOnIdle{style.value=NavAnimationStyle.ELASTIC;selected.intValue=4};compose.mainClock.advanceTimeBy(96)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitUntil(3000){!motion.position.isRunning&&kotlin.math.abs(motion.position.value-4f)<.001f}
        assertFalse(motion.position.isRunning)
        assertEquals(4f,motion.position.value,.001f)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test fun centerRevealStartsSmallAndFinishesAsFullSection(){
        compose.mainClock.autoAdvance=false
        val selected=mutableIntStateOf(0)
        lateinit var motion:NavHighlightState
        compose.setContent{motion=rememberNavHighlight(selected.intValue,NavAnimationStyle.CENTER_REVEAL);Box(Modifier.size(320.dp,72.dp).testTag("nav").navHighlight(motion,Color.Black,Color.Cyan,Color.Gray))}
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle{selected.intValue=2};compose.mainClock.advanceTimeBy(96)
        val middle=compose.onNodeWithTag("nav").captureToImage().toPixelMap()
        val edges=navSectionEdges(middle.width.toFloat());val x=((edges[2]+edges[3])/2).toInt()
        assertTrue(middle[x,middle.height/2].green>.8f)
        assertTrue("Corner remains unfilled during radial reveal",middle[(edges[2]+5).toInt(),5].green<.3f)
        compose.runOnIdle{selected.intValue=4};compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle{assertTrue(motion.previous.any{it.slot==2&&it.fraction<1f})}
        compose.runOnIdle{selected.intValue=2};compose.mainClock.advanceTimeBy(1000)
        val final=compose.onNodeWithTag("nav").captureToImage().toPixelMap()
        for(px in listOf((edges[2]+5).toInt(),x,(edges[3]-5).toInt()))for(py in listOf(5,final.height/2,final.height-5))assertTrue("Entire target section must be filled",final[px,py].green>.95f)
        assertTrue("Previous section fades completely",final[((edges[1]+edges[2])/2).toInt(),final.height/2].green<.3f)
    }

    @Test fun processRestartPersistence()=runBlocking{
        val phase=InstrumentationRegistry.getArguments().getString("navPersistencePhase")
        assumeTrue(phase=="write"||phase=="read")
        val manager=ThemeManager(compose.activity.applicationContext)
        val backup=File(compose.activity.getExternalFilesDir(null),"nav-style-test-backup.txt")
        if(phase=="write"){
            backup.writeText(manager.navAnimationStyleFlow.first().id)
            manager.setNavAnimationStyle(NavAnimationStyle.ELASTIC)
        }else{
            try{assertEquals(NavAnimationStyle.ELASTIC,manager.navAnimationStyleFlow.first())}
            finally{manager.setNavAnimationStyle(NavAnimationStyle.fromId(backup.readText()));backup.delete()}
        }
    }

    @Test fun disabledSystemAnimationsSnapToTheSelectedSection(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=compose.activity
        val original=android.provider.Settings.Global.getFloat(context.contentResolver,android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)
        fun scale(value:Float){instrumentation.uiAutomation.executeShellCommand("settings put global animator_duration_scale $value").use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()}}
        compose.mainClock.autoAdvance=false
        val selected=mutableIntStateOf(0)
        lateinit var motion:NavHighlightState
        try{
            scale(0f)
            compose.setContent{motion=rememberNavHighlight(selected.intValue,NavAnimationStyle.CENTER_REVEAL);Box(Modifier.size(320.dp,72.dp).navHighlight(motion,Color.Black,Color.Cyan,Color.Gray))}
            compose.mainClock.advanceTimeBy(64)
            compose.runOnIdle{selected.intValue=4};compose.mainClock.advanceTimeBy(64)
            compose.runOnIdle{assertEquals(4f,motion.position.value,0f);assertEquals(1f,motion.reveal.value,0f);assertFalse(motion.reveal.isRunning)}
        }finally{scale(original)}
    }
}
