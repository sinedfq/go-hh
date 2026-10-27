package com.example.trudomix.ui.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable fun OtpCodeField(value:String,onChange:(String)->Unit,label:String,verified:Boolean,enabled:Boolean){
    val success=Color(0xFF23965B)
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(label,style=MaterialTheme.typography.labelLarge)
        BasicTextField(value,onChange,enabled=enabled&&!verified,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),textStyle=MaterialTheme.typography.titleLarge.copy(color=Color.Transparent),cursorBrush=SolidColor(Color.Transparent),modifier=Modifier.fillMaxWidth().semantics{contentDescription=label},decorationBox={inner->
            Box{
                inner()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    repeat(6){index->
                        Box(Modifier.weight(1f).height(54.dp).background(if(verified)success.copy(alpha=.10f)else MaterialTheme.colorScheme.surface,RoundedCornerShape(12.dp)).border(if(verified||index==value.length)2.dp else 1.dp,if(verified)success else if(index==value.length)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
                            Text(value.getOrNull(index)?.toString().orEmpty(),style=MaterialTheme.typography.titleLarge,color=if(verified)success else MaterialTheme.colorScheme.onSurface,modifier=Modifier.clearAndSetSemantics{})
                        }
                    }
                }
            }
        })
    }
}
