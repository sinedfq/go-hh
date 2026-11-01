package com.example.trudomix

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.trudomix.data.local.*
import org.junit.Assert.*
import org.junit.Test

class AccentPaletteTest{
    @Test fun hexRoundTripAndInvalidInput(){
        assertEquals("#12ABEF",accentHex(parseAccentHex(" #12abef ")!!))
        assertEquals("#000000",accentHex(parseAccentHex("000000")!!))
        for(value in listOf(null,"","#fff","#GG1122","#12345678"))assertNull(parseAccentHex(value))
    }
    @Test fun arbitraryColorsRemainReadableInBothThemes(){
        fun ratio(a:Color,b:Color)=(maxOf(a.luminance(),b.luminance())+.05f)/(minOf(a.luminance(),b.luminance())+.05f)
        for(raw in listOf(Color.Black,Color.White,Color.Red,Color.Green,Color.Blue,Color.Yellow,Color(0xFF777777)))for(dark in listOf(false,true)){
            val primary=readableAccent(raw,dark)
            assertTrue(ratio(primary,if(dark)Color(0xFF080816)else Color(0xFFF8F7FC))>=4.5f)
            assertTrue(ratio(primary,accentForeground(primary))>=4.5f)
        }
    }
}
