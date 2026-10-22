package com.example.trudomix.ui.screens.companies

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.userErrorMessage
import com.example.trudomix.data.models.Company
import com.example.trudomix.data.repository.bodyOrThrow
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun CompanyMediaUpload(company:Company,onUpdated:()->Unit){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null&&!busy){scope.launch{
            busy=true;error=null
            try{
                withContext(Dispatchers.IO){
                    val mime=context.contentResolver.getType(uri).orEmpty()
                    val video=mime in listOf("video/mp4","video/webm")
                    require(video||mime in listOf("image/jpeg","image/png","image/webp")){"Поддерживаются JPG, PNG, WebP, MP4 и WebM."}
                    require(if(video)company.videos.size<4 else company.photos.size<6){"Можно добавить до 6 фото и 4 видео."}
                    val maxBytes=(if(video)50L else 4L)*1024*1024
                    val file=File.createTempFile("company_media_",".upload",context.cacheDir)
                    try{
                        context.contentResolver.openInputStream(uri)?.use{input->file.outputStream().use{output->
                            val buffer=ByteArray(8192);var total=0L
                            while(true){ensureActive();val count=input.read(buffer);if(count<0)break;total+=count;require(total<=maxBytes){if(video)"Видео должно быть не больше 50 МБ."else "Фото должно быть не больше 4 МБ."};output.write(buffer,0,count)}
                        }}?:error("Не удалось открыть файл")
                        val part=MultipartBody.Part.createFormData(if(video)"video"else "photo","company.${mime.substringAfter('/')}",file.asRequestBody(mime.toMediaType()))
                        ApiClient.api.uploadCompanyMedia(part).bodyOrThrow()
                    }finally{file.delete()}
                }
                onUpdated()
            }catch(e:CancellationException){throw e}catch(e:IllegalArgumentException){error=e.message}catch(e:Exception){error=userErrorMessage(e)}finally{busy=false}
        }}
    }
    OutlinedCard(Modifier.fillMaxWidth()){
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Истории вашей команды",style=MaterialTheme.typography.titleMedium)
            Text("До 6 фото (4 МБ) и 4 видео MP4/WebM (50 МБ)",style=MaterialTheme.typography.bodySmall)
            Button(onClick={picker.launch(arrayOf("image/jpeg","image/png","image/webp","video/mp4","video/webm"))},enabled=!busy&&company.photos.size+company.videos.size<10){Text(if(busy)"Загружаем…"else "Добавить фото или видео")}
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
    }
}
