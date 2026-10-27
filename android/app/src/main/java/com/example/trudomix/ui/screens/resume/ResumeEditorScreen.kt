package com.example.trudomix.ui.screens.resume

import com.example.trudomix.ui.loading.InlineLoadingState

import com.example.trudomix.ui.loading.AppLoader

import com.example.trudomix.ui.components.AppScreenHeader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeEditorScreen(viewModel: ResumeViewModel,onNavigateBack:()->Unit) {
    val state by viewModel.uiState.collectAsState()
    val saving by viewModel.isSaving.collectAsState()
    val snackbar=remember{SnackbarHostState()}
    val directory:DirectoryViewModel=viewModel()
    val options by directory.state.collectAsState()
    LaunchedEffect(viewModel){viewModel.messageEvent.collect{snackbar.showSnackbar(it)}}
    Scaffold(topBar={AppScreenHeader(title={Text("Редактирование резюме")},navigationIcon={IconButton(onClick=onNavigateBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}})},snackbarHost={AppSnackbarHost(snackbar)}) { padding ->
        Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
            when(val current=state) {
                ResumeUiState.Loading -> AppLoader(Modifier.align(Alignment.Center))
                is ResumeUiState.Error -> Column(Modifier.padding(24.dp)){Text(current.message);Button(onClick=viewModel::loadResume){Text("Повторить")}}
                is ResumeUiState.Success -> ResumeEditorContent(current.resume,options,saving,directory::load,viewModel::uploadPhoto) { resume -> viewModel.saveResume(resume,current.resume!=null,onNavigateBack) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeEditorContent(existing:Resume?,options:DirectoryOptions,saving:Boolean,onRetry:()->Unit={},onPhoto:suspend(okhttp3.MultipartBody.Part)->Unit={},onSave:(Resume)->Unit) {
    // Key by resume ID: a photo update must not reset the unsaved text fields.
    var name by rememberSaveable(existing?.id){mutableStateOf(existing?.full_name.orEmpty())}
    var position by rememberSaveable(existing?.id){mutableStateOf(existing?.desired_position.orEmpty())}
    var experience by rememberSaveable(existing?.id){mutableStateOf(existing?.experience.orEmpty())}
    var city by rememberSaveable(existing?.id){mutableStateOf(existing?.city.orEmpty())}
    var phone by rememberSaveable(existing?.id){mutableStateOf(existing?.phone.orEmpty())}
    var telegram by rememberSaveable(existing?.id){mutableStateOf(existing?.telegram.orEmpty())}
    var about by rememberSaveable(existing?.id){mutableStateOf(existing?.about_me.orEmpty())}
    var github by rememberSaveable(existing?.id){mutableStateOf(existing?.github_url.orEmpty())}
    var linkedin by rememberSaveable(existing?.id){mutableStateOf(existing?.linkedin_url.orEmpty())}
    var remote by rememberSaveable(existing?.id){mutableStateOf(existing?.remote?:false)}
    var skills by rememberSaveable(existing?.id){mutableStateOf(existing?.skills.orEmpty())}
    var newSkill by rememberSaveable{mutableStateOf("")}
    val draft=Resume(id=existing?.id?:0,photo_url=existing?.photo_url.orEmpty(),full_name=name,desired_position=position,experience=experience,city=city,phone=phone,telegram=telegram,about_me=about,github_url=github,linkedin_url=linkedin,remote=remote,skills=skills)
    val complete=resumeCompletionCount(draft)
    fun addSkill(value:String){val trimmed=value.trim();if(trimmed.isNotEmpty()&&skills.none{it.equals(trimmed,true)})skills=skills+trimmed;newSkill=""}
    Column(Modifier.widthIn(max=760.dp).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Твой профессиональный портрет",style=MaterialTheme.typography.headlineSmall)
            Text("Заполнено ${resumeCompletionLabel(complete)}",color=MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress={complete/8f},modifier=Modifier.fillMaxWidth())
            Text("8 разделов по 12,5%. Проценты показывают заполненность, а не оценку работодателя.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        if(options.loading)InlineLoadingState()
        if(options.failed) {Text("Подсказки загружены частично. Можно вписать свой вариант.",style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry){Text("Обновить подсказки")}}
        ResumeSection("Фото · +12,5%") {
            if(existing!=null)PhotoPicker(existing.photo_url,"Фото резюме",onPhoto)
            else Text("Сначала сохрани резюме, затем добавь фото.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ResumeSection("Основное") {
            ResumeInput(name,{name=it},"ФИО · +12,5%")
            SuggestionField(position,{position=it},"Должность · +12,5%",options.positions,Modifier.fillMaxWidth(),allowCustom=true)
            SuggestionField(city,{city=it},"Город · +12,5%",(options.cities+existing?.city.orEmpty()).filter{it.isNotBlank()},Modifier.fillMaxWidth(),allowCustom=true)
            Row(verticalAlignment=Alignment.CenterVertically){Switch(remote,{remote=it});Text("Готов к удалённой работе · +0%",Modifier.weight(1f).padding(start=12.dp),style=MaterialTheme.typography.bodyMedium)}
        }
        ResumeSection("Опыт и навыки") {
            SuggestionField(experience,{experience=it},"Опыт / грейд · +12,5%",listOf("Без опыта","Junior","Middle","Senior","Lead","До 1 года","1-3 года","3-6 лет","6+ лет")+options.experiences,Modifier.fillMaxWidth(),allowCustom=true)
            Text("Навыки · +12,5% за раздел",style=MaterialTheme.typography.titleSmall)
            SuggestionField(newSkill,{newSkill=it},"Выбрать или добавить навык",options.skills.filter{option->skills.none{it.equals(option,true)}},Modifier.fillMaxWidth(),allowCustom=true,onSelect=::addSkill)
            TextButton(onClick={addSkill(newSkill)},enabled=newSkill.isNotBlank()){Text("Добавить навык")}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){skills.forEach{skill->InputChip(true,{skills=skills-skill},label={Text(skill)},trailingIcon={Icon(Icons.Default.Close,"Удалить $skill",Modifier.size(18.dp))})}}
            ResumeInput(about,{about=it},"О себе · +12,5%",multiline=true)
        }
        ResumeSection("Контакты · +12,5% за телефон или Telegram") {
            ResumeInput(phone,{phone=it},"Телефон · до +12,5% за контакты")
            ResumeInput(telegram,{telegram=it},"Telegram · до +12,5% за контакты")
            Text("Два контакта не суммируются: для заполнения раздела достаточно одного.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ResumeInput(github,{github=it},"GitHub · +0% (дополнительно)")
            ResumeInput(linkedin,{linkedin=it},"LinkedIn · +0% (дополнительно)")
        }
        Button(onClick={onSave(draft.copy(full_name=name.trim(),desired_position=position.trim(),city=city.trim(),experience=experience.trim()))},enabled=!saving&&name.isNotBlank()&&position.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){
            if(saving)AppLoader(Modifier.size(24.dp),strokeWidth=2.dp)else Text("Сохранить резюме")
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ResumeSection(title:String,content:@Composable ColumnScope.()->Unit) {
    OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text(title,style=MaterialTheme.typography.titleMedium);content()}}
}
@Composable
private fun ResumeInput(value:String,onChange:(String)->Unit,label:String,multiline:Boolean=false) {
    OutlinedTextField(value,onChange,label={Text(label)},shape=RoundedCornerShape(16.dp),singleLine=!multiline,minLines=if(multiline)4 else 1,maxLines=if(multiline)8 else 1,modifier=Modifier.fillMaxWidth())
}
