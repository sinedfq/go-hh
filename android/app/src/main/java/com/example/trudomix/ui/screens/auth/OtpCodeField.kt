package com.example.trudomix.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.*

const val OTP_LENGTH=4
fun otpDigits(value:String)=value.filter{it in '0'..'9'}.take(OTP_LENGTH)

@Composable fun OtpCodeField(value:String,onChange:(String)->Unit,label:String,verified:Boolean,enabled:Boolean,verifying:Boolean=false){
    val success=Color(0xFF23965B)
    val blue=Color(0xFF408CFF)
    val fill by animateColorAsState(if(verified)success.copy(alpha=.12f)else if(verifying)blue.copy(alpha=.08f)else MaterialTheme.colorScheme.surface,tween(400),label="otp-fill")
    val outline by animateColorAsState(if(verified)success else if(verifying)blue else MaterialTheme.colorScheme.outlineVariant,tween(400),label="otp-outline")
    val position=remember{Animatable(0f)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(verifying,lifecycle){
        if(verifying)lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(android.animation.ValueAnimator.areAnimatorsEnabled()){position.animateTo(3f,tween(900,easing=LinearEasing));position.animateTo(0f,tween(900,easing=LinearEasing))}}
        else position.snapTo(0f)
    }
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(label,style=MaterialTheme.typography.labelLarge,modifier=Modifier.semantics{liveRegion=LiveRegionMode.Polite})
        BasicTextField(value,{onChange(otpDigits(it))},enabled=enabled&&!verified&&!verifying,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),textStyle=MaterialTheme.typography.titleLarge.copy(color=Color.Transparent),cursorBrush=SolidColor(Color.Transparent),modifier=Modifier.fillMaxWidth().semantics{contentDescription=label},decorationBox={inner->
            Box{
                inner()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    repeat(OTP_LENGTH){index->
                        val active=index==value.length&&enabled&&!verified
                        Box(Modifier.weight(1f).height(50.dp).background(fill,RoundedCornerShape(14.dp)).border(if(verified||verifying||active)2.dp else 1.dp,if(active&&!verifying)MaterialTheme.colorScheme.primary else outline,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){
                            AnimatedContent(value.getOrNull(index)?.toString().orEmpty(),transitionSpec={(fadeIn(tween(180))+scaleIn(initialScale=.7f)+slideInVertically{it/3}) togetherWith fadeOut(tween(80))},label="otp-digit"){digit->
                                Text(digit,style=MaterialTheme.typography.titleLarge,color=if(verified)success else MaterialTheme.colorScheme.onSurface,modifier=Modifier.clearAndSetSemantics{})
                            }
                        }
                    }
                }
                if(verifying)Canvas(Modifier.matchParentSize().clearAndSetSemantics{}){
                    val gap=8.dp.toPx();val cell=(size.width-gap*3)/4
                    val p=position.value
                    drawCircle(blue,3.dp.toPx(),Offset(cell/2+p*(cell+gap),size.height-6.dp.toPx()-abs(sin(p*PI).toFloat())*12.dp.toPx()))
                }
            }
        })
    }
}
