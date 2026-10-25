package com.example.trudomix

import com.example.trudomix.data.local.AppIcons
import org.junit.Assert.*
import org.junit.Test

class ThemedIconTest {
    @Test fun themeChangesBackgroundButKeepsChosenDesign(){
        for(icon in AppIcons.choices){
            assertEquals(icon.design,AppIcons.forTheme(icon,true).design)
            assertEquals(icon.design,AppIcons.forTheme(icon,false).design)
            assertEquals(icon,AppIcons.forTheme(icon,true,false))
        }
        assertEquals("white",AppIcons.forTheme(AppIcons.default,false).color)
        assertEquals("black",AppIcons.forTheme(AppIcons.default,true).color)
        val mint=AppIcons.choices.first{it.color=="mint"}
        assertEquals(mint,AppIcons.forTheme(mint,true))
    }
}
