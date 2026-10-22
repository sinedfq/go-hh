package com.example.trudomix.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient
import kotlinx.coroutines.*
import okhttp3.MultipartBody

@Composable
fun PhotoPicker(photoUrl: String, label: String, onUpload: suspend (MultipartBody.Part) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cropBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var imageFailed by remember(photoUrl) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true; error = null
            try { cropBitmap=loadAvatarForCrop(context,uri) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "Не удалось открыть фото. Выбери JPG, PNG или WebP размером до 20 МБ." }
            finally { busy = false }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                if (photoUrl.isBlank() || imageFailed) Icon(Icons.Default.PersonOutline, label, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                else AsyncImage(ApiClient.mediaUrl(photoUrl), label, contentScale = ContentScale.Crop,
                    onError = { imageFailed = true }, modifier = Modifier.fillMaxSize().clip(CircleShape))
                if (busy) CircularProgressIndicator(Modifier.size(32.dp))
            }
        }
        TextButton(enabled = !busy && cropBitmap==null, onClick = { picker.launch("image/*") }) {
            Text(if (busy) "Загрузка…" else if (photoUrl.isBlank()) "Добавить фото" else "Изменить фото")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    cropBitmap?.let { bitmap ->
        AvatarCropDialog(bitmap,busy,error,onDismiss={cropBitmap=null;error=null}) { source,pose ->
            if(!busy)scope.launch {
                busy=true;error=null
                try { onUpload(croppedAvatarPart(source,pose));cropBitmap=null }
                catch(e:CancellationException){throw e}
                catch(e:Exception){error=com.example.trudomix.data.api.userErrorMessage(e)}
                finally{busy=false}
            }
        }
    }
}
