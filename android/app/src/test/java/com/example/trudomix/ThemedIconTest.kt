package com.example.trudomix

import com.example.trudomix.data.local.AppIcons
import org.junit.Assert.*
import org.junit.Test

class ThemedIconTest {
    @Test fun approvedWavePairIsTheDefaultAndFollowsSplashTheme() {
        assertEquals(AppIcons.nightWave,AppIcons.default)
        assertEquals(AppIcons.nightWave,AppIcons.waveForTheme(true))
        assertEquals(AppIcons.lightWave,AppIcons.waveForTheme(false))
        assertEquals(AppIcons.lightWave,AppIcons.forTheme(AppIcons.nightWave,false))
        assertEquals(AppIcons.nightWave,AppIcons.forTheme(AppIcons.lightWave,true))
    }
    @Test fun themeChangesBackgroundButKeepsChosenDesign(){
        for(icon in AppIcons.choices){
            assertEquals(icon.design,AppIcons.forTheme(icon,true).design)
            assertEquals(icon.design,AppIcons.forTheme(icon,false).design)
            assertEquals(icon,AppIcons.forTheme(icon,true,false))
        }
        assertEquals("white",AppIcons.forTheme(AppIcons.default,false).color)
        assertEquals("black",AppIcons.forTheme(AppIcons.default,true).color)
        assertEquals(2,AppIcons.choices.size)
    }
}
