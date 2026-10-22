package com.example.trudomix.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.example.trudomix.data.api.*
import com.example.trudomix.data.repository.bodyOrThrow
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun VacancyImageUpload(onAdd:(String)->Unit){
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val latest by rememberUpdatedState(onAdd)
    var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null&&!busy)scope.launch{
        busy=true;error=null
        try{
            val part=withContext(Dispatchers.IO){
                val mime=context.contentResolver.getType(uri).orEmpty();require(mime in listOf("image/jpeg","image/png","image/webp")){"Выбери JPG, PNG или WebP"}
                val data=java.io.ByteArrayOutputStream()
                context.contentResolver.openInputStream(uri)?.use{input->val buffer=ByteArray(8192);var size=input.read(buffer);while(size>=0){data.write(buffer,0,size);require(data.size()<=4*1024*1024){"Изображение должно быть не больше 4 МБ"};size=input.read(buffer)}}?:error("Не удалось открыть изображение")
                MultipartBody.Part.createFormData("photo","vacancy.${mime.substringAfter('/')}",data.toByteArray().toRequestBody(mime.toMediaType()))
            }
            val url=ApiClient.api.uploadVacancyImage(part).bodyOrThrow().url
            latest("\n\n![Фотография компании]($url)\n")
        }catch(e:CancellationException){throw e}catch(e:IllegalArgumentException){error=e.message}catch(e:Exception){error=userErrorMessage(e)}finally{busy=false}
    }}
    Column{OutlinedButton(onClick={picker.launch("image/*")},enabled=!busy){Text(if(busy)"Загрузка…" else "Добавить картинку")};error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}
}
