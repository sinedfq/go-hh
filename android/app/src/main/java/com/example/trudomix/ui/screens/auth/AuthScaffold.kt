package com.example.trudomix.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trudomix.ui.components.SelectedBrandImage

/** The same frame for both sign-in methods; form state and requests stay in their owners. */
@Composable internal fun AuthScaffold(phone:Boolean,onMethod:((Boolean)->Unit)?,enabled:Boolean,onRegister:()->Unit,content:@Composable ColumnScope.()->Unit){
    Box(Modifier.fillMaxSize()){
        AuthBackdrop(Modifier.fillMaxSize())
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()){
            val compact=maxHeight<680.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=if(compact)12.dp else 24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(if(compact)12.dp else 20.dp,Alignment.CenterVertically)){
                Row(Modifier.widthIn(max=420.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp,Alignment.CenterHorizontally)){
                    SelectedBrandImage("Trudomix",Modifier.size(if(compact)40.dp else 48.dp))
                    Column{
                        Text("ТРУДОМИКС",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                        if(!compact)Text("Люди. Работа. Возможности.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(Modifier.widthIn(max=420.dp).fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
                    Column(Modifier.padding(if(compact)18.dp else 24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Text("С возвращением",Modifier.fillMaxWidth(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                        if(!compact)Text("Войди, чтобы продолжить свой путь",Modifier.fillMaxWidth(),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
                        if(onMethod!=null)AuthMethodPicker(phone,onMethod,enabled)
                        content()
                        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                        FilledTonalButton(onClick=onRegister,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(16.dp)){
                            Text("Создать аккаунт",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
