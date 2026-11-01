package com.example.trudomix

import com.example.trudomix.data.local.NavAnimationStyle
import com.example.trudomix.ui.components.navSectionEdges
import com.example.trudomix.ui.components.navRevealRadius
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class NavGeometryTest{
    @Test fun sectionEdgesPreserveExistingLargerCentralWeight(){
        val edges=navSectionEdges(515f)
        assertArrayEquals(floatArrayOf(0f,100f,200f,315f,415f,515f),edges,.001f)
    }
    @Test fun revealCoversEveryCornerAndUnknownPreferenceUsesSmooth(){
        for(width in listOf(40f,80f,120f))for(height in listOf(60f,72f)){
            assertEquals(hypot(width/2,height/2),navRevealRadius(width,height),0f)
        }
        assertEquals(NavAnimationStyle.SMOOTH,NavAnimationStyle.fromId("missing"))
        NavAnimationStyle.entries.forEach{assertEquals(it,NavAnimationStyle.fromId(it.id))}
    }
}
