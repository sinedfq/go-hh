package com.example.trudomix.ui.screens.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.components.PhotoPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.httpErrorMessage
import com.example.trudomix.ui.components.AppSnackbarHost
import kotlinx.coroutines.*

@Composable
fun ResumeScreen(viewModel:ResumeViewModel,onNavigateBack:()->Unit={},onEditResumeClick:()->Unit) {
    val state by viewModel.uiState.collectAsState()
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val snackbar=remember { SnackbarHostState() }
    val busy by viewModel.isSaving.collectAsState()
    LaunchedEffect(viewModel){viewModel.messageEvent.collect{snackbar.showSnackbar(it)}}
    var exporting by remember { mutableStateOf(false) }
    val savePdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if(uri!=null) scope.launch {
            exporting=true
            try {
                withContext(Dispatchers.IO) {
                    val response=ApiClient.api.getMyResumePdf()
                    if(!response.isSuccessful) { response.errorBody()?.close(); error(httpErrorMessage(response.code())) }
                    val body=response.body() ?: error("Сервер не вернул PDF")
                    body.use { pdf ->
                        check(pdf.contentType()?.subtype=="pdf") { "Сервер вернул неверный формат файла" }
                        val output=context.contentResolver.openOutputStream(uri,"wt") ?: error("Не удалось открыть файл")
                        output.use { pdf.byteStream().use { input -> input.copyTo(it) } }
                    }
                }
                snackbar.showSnackbar("Резюме сохранено в PDF")
            } catch(e:CancellationException) { throw e }
            catch(e:java.io.IOException) { snackbar.showSnackbar("Не удалось скачать или сохранить PDF. Проверь соединение и выбранную папку.") }
            catch(e:Exception) { snackbar.showSnackbar(e.message ?: "Не удалось сохранить резюме. Попробуйте ещё раз.") }
            finally { exporting=false }
        }
    }
    Scaffold(snackbarHost={AppSnackbarHost(snackbar)},topBar={AppScreenHeader(title={Text("Моё резюме")},subtitle="Твой опыт, навыки и следующие возможности",
        navigationIcon={IconButton(onClick=onNavigateBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}},
        )}) { padding ->
        com.example.trudomix.ui.components.RefreshContent(state is ResumeUiState.Loading,viewModel::loadResume) {
        Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max=760.dp).fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                when(val current=state) {
                    ResumeUiState.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    is ResumeUiState.Error -> item { ResumeSection("Не удалось загрузить резюме") {Text(current.message,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick=viewModel::loadResume){Text("Повторить")}} }
                    is ResumeUiState.Success -> {
                        val resume=current.resume
                        if(resume==null) item { ResumeSection("Расскажи о себе работодателям") {
                            Icon(Icons.Default.Description,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(40.dp))
                            Text("Создай резюме, чтобы откликаться на вакансии и знакомиться с будущей командой.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick=onEditResumeClick,modifier=Modifier.fillMaxWidth()){Text("Создать резюме")}
                        }} else {
                            item { ResumeSection(resume.full_name.ifBlank{"Моё резюме"}) {
                                PhotoPicker(resume.photo_url,"Фото резюме",viewModel::uploadPhoto)
                                Text(resume.desired_position,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
                                val count=resumeCompletionCount(resume)
                                LinearProgressIndicator(progress={count/8f},modifier=Modifier.fillMaxWidth())
                                Text("Заполнено ${resumeCompletionLabel(count)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedButton(onClick=onEditResumeClick,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Редактировать резюме")}
                                OutlinedButton(enabled=!exporting,onClick={savePdf.launch("trudomix-resume-${resume.id}.pdf")},modifier=Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Download,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(exporting)"Сохранение…" else "Скачать PDF")
                                }
                            }}
                            item { ResumeSection("Опыт и формат работы") {
                                ResumeDetail("Город",resume.city)
                                ResumeDetail("Опыт / грейд",formatResumeExperience(resume.experience))
                                ResumeDetail("Формат",if(resume.remote)"Готов к удалённой работе" else "Офис / гибрид")
                            }}
                            item {ResumeSection("История работы"){ResumeWorkHistory(resume,busy,viewModel::addWorkExperience,viewModel::deleteWorkExperience)}}
                            if(resume.skills.isNotEmpty())item { ResumeSection("Навыки") { ResumeSkills(resume.skills) } }
                            if(resume.about_me.isNotBlank())item { ResumeSection("О себе") {Text(resume.about_me,style=MaterialTheme.typography.bodyMedium)} }
                            item { ResumeSection("Контакты") {
                                ResumeDetail("Телефон",resume.phone)
                                ResumeDetail("Telegram",resume.telegram)
                                if(resume.github_url.isNotBlank())ResumeDetail("GitHub",resume.github_url)
                                if(resume.linkedin_url.isNotBlank())ResumeDetail("LinkedIn",resume.linkedin_url)
                            }}
                        }
                    }
                }
            }
        }
        }
    }
}

fun formatResumeExperience(value:String):String = value.trim().let { if(it.toDoubleOrNull()!=null)"$it лет" else it }

@Composable
private fun ResumeSection(title:String,content:@Composable ColumnScope.()->Unit) {
    OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ResumeDetail(label:String,value:String) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank{"Не указано"},style=MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumeSkills(skills:List<String>) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {skills.forEach {skill->SuggestionChip(onClick={},label={Text(skill)})}}
}
