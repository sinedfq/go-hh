package com.example.trudomix.ui.screens.companies

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.userErrorMessage
import com.example.trudomix.data.models.TimedCompanyStory
import com.example.trudomix.data.repository.*
import com.example.trudomix.ui.screens.social.SocialEditor
import com.example.trudomix.ui.screens.social.TimedStoriesBar
import kotlinx.coroutines.*

@Composable
fun CompanySocialPanel(companyId:Int,manage:Boolean){
    var stories by remember(companyId){mutableStateOf<List<TimedCompanyStory>>(emptyList())}
    var publishing by remember{mutableStateOf(false)};var revision by remember{mutableIntStateOf(0)};var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(companyId,revision){while(true){try{stories=ApiClient.api.timedStories(companyId).bodyOrThrow();error=null}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e)};delay(60000)}}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(manage)OutlinedButton(onClick={publishing=true},modifier=Modifier.fillMaxWidth()){Text("Опубликовать историю на 24 часа")}
        TimedStoriesBar(stories){revision++}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={revision++}){Text("Повторить загрузку историй")}}
    }
    if(publishing)SocialEditor(true,true,{publishing=false}){revision++}
}
