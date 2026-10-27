package com.example.trudomix.ui.screens.auth

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*

fun normalizedPhoneInput(value:String):String{
    var digits=value.filter{it in '0'..'9'}.take(15)
    if(!value.startsWith("+")&&digits.startsWith("8")&&digits.length==11)digits="7"+digits.drop(1)
    if(!value.startsWith("+")&&digits.length==10)digits="7$digits"
    return if(digits.isEmpty())""else "+$digits"
}

object PhoneVisualTransformation:VisualTransformation{
    override fun filter(text:AnnotatedString):TransformedText{
        val raw=text.text
        if(!raw.startsWith("+7")||raw.length>12)return TransformedText(text,OffsetMapping.Identity)
        val output=StringBuilder();val offsets=IntArray(raw.length+1)
        raw.forEachIndexed{i,c->
            when(i){2->output.append(" (");5->output.append(") ");8,10->output.append("-")}
            offsets[i]=output.length;output.append(c)
        }
        offsets[raw.length]=output.length
        return TransformedText(AnnotatedString(output.toString()),object:OffsetMapping{
            override fun originalToTransformed(offset:Int)=offsets[offset.coerceIn(0,raw.length)]
            override fun transformedToOriginal(offset:Int)=offsets.indexOfFirst{it>=offset}.let{if(it<0)raw.length else it}.coerceIn(0,raw.length)
        })
    }
}
