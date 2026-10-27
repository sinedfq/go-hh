package com.example.trudomix

import com.example.trudomix.ui.screens.auth.*
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Test

class PhoneInputTest{
    @Test fun pastedAndInternationalPhonesPreserveCountryCodes(){
        for(value in listOf("89991234567","9991234567","+7 (999) 123-45-67"))assertEquals("+79991234567",normalizedPhoneInput(value))
        assertEquals("+442071234567",normalizedPhoneInput("+44 207 123 4567"))
        assertEquals("",normalizedPhoneInput(""))
    }
    @Test fun visualMaskKeepsEveryCaretOffsetInBounds(){
        val input="+79991234567"
        for(length in 0..input.length){
            val transformed=PhoneVisualTransformation.filter(AnnotatedString(input.take(length)))
            for(i in 0..length)assertTrue(transformed.offsetMapping.originalToTransformed(i) in 0..transformed.text.length)
            for(i in 0..transformed.text.length)assertTrue(transformed.offsetMapping.transformedToOriginal(i) in 0..length)
        }
        assertEquals("+7 (999) 123-45-67",PhoneVisualTransformation.filter(AnnotatedString(input)).text.text)
    }
}
