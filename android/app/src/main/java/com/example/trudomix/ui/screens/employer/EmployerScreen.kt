package com.example.trudomix.ui.screens.employer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.models.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerScreen(onBack:()->Unit,onChat:(Int)->Unit,initialTab:Int=0,embedded:Boolean=false){
    val model:EmployerViewModel=viewModel()
    val state by model.state.collectAsState()
    var tab by remember(initialTab){mutableIntStateOf(initialTab)}
    var editing by remember{mutableStateOf<Vacancy?>(null)}
    var form by remember{mutableStateOf(false)}
    var companyForm by remember{mutableStateOf(false)}
    var deleting by remember{mutableStateOf<Vacancy?>(null)}
    EmployerWorkspace(state,tab,embedded,{tab=it},onBack,model::load,{companyForm=true},{editing=null;form=true},{editing=it;form=true},model::archive,{deleting=it},model::status,{model.chat(it,onChat)})
    if(companyForm)ModalBottomSheet(onDismissRequest={companyForm=false}){
        var name by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var about by remember{mutableStateOf("")}
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp).imePadding(),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Компания",style=MaterialTheme.typography.titleLarge)
            OutlinedTextField(name,{name=it},label={Text("Название")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(city,{city=it},label={Text("Город")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(about,{about=it},label={Text("Описание")},modifier=Modifier.fillMaxWidth())
            Button(onClick={model.company(CompanyRequest(name.trim(),description=about,city=city)){companyForm=false}},enabled=name.isNotBlank()&&!state.busy){Text("Создать")}
        }
    }
    if(form)VacancyEditor(editing,state.busy,model,{form=false}){request->model.save(editing?.id,request){form=false}}
    deleting?.let{v->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить вакансию?")},text={Text(v.title+". Связанные отклики и переписка могут быть удалены.")},confirmButton={TextButton(onClick={model.delete(v);deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}
fun applicationStatusLabel(status:String?)=when(status){"new"->"Новый";"viewed"->"Просмотрен";"accepted"->"Приглашение";"rejected"->"Отказ";else->status.orEmpty()}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacancyEditor(v:Vacancy?,busy:Boolean,model:EmployerViewModel,onClose:()->Unit,onSave:(VacancyRequest)->Unit){
    var title by remember{mutableStateOf(v?.title.orEmpty())};var city by remember{mutableStateOf(v?.location.orEmpty())}
    var experience by remember{mutableStateOf(v?.experience.orEmpty())};var remote by remember{mutableStateOf(v?.remote?:false)}
    var skills by remember{mutableStateOf(v?.skills?.joinToString(", ").orEmpty())};var description by remember{mutableStateOf(v?.description.orEmpty())}
    var address by remember{mutableStateOf(v?.address.orEmpty())};var latitude by remember{mutableStateOf(v?.latitude?.takeUnless{it==0.0}?.toString().orEmpty())};var longitude by remember{mutableStateOf(v?.longitude?.takeUnless{it==0.0}?.toString().orEmpty())}
    var geocoding by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope()
    val lat=latitude.toDoubleOrNull();val lon=longitude.toDoubleOrNull()
    val validCoordinates=(latitude.isBlank()&&longitude.isBlank())||(lat!=null&&lon!=null&&lat in -90.0..90.0&&lon in -180.0..180.0)
    ModalBottomSheet(onDismissRequest=onClose){
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp).imePadding(),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(v==null)"Новая вакансия" else "Редактирование",style=MaterialTheme.typography.titleLarge)
            OutlinedTextField(title,{title=it},label={Text("Название")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(city,{city=it},label={Text("Город")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(experience,{experience=it},label={Text("Опыт: junior, middle, senior")},modifier=Modifier.fillMaxWidth())
            Row{Switch(remote,{remote=it});Text("Удалённая работа",Modifier.padding(12.dp))}
            OutlinedTextField(skills,{skills=it},label={Text("Навыки через запятую")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(description,{description=it.take(50000)},label={Text("Описание · Markdown")},modifier=Modifier.fillMaxWidth(),minLines=5)
            Text("## Заголовок · **жирный** · - список · [ссылка](https://example.com)",style=MaterialTheme.typography.bodySmall)
            com.example.trudomix.ui.components.VacancyImageUpload {description+=it}
            var preview by remember{mutableStateOf(false)}
            TextButton(onClick={preview=!preview}){Text(if(preview)"Скрыть предпросмотр" else "Предпросмотр описания")}
            if(preview)com.example.trudomix.ui.components.MarkdownContent(description)
            OutlinedTextField(address,{address=it},label={Text("Адрес")},modifier=Modifier.fillMaxWidth())
            TextButton(onClick={geocoding=true;scope.launch{try{val point=model.geocode(address);latitude=point.latitude.toString();longitude=point.longitude.toString();error=null}catch(e:CancellationException){throw e}catch(_:Exception){error="Не удалось определить координаты. Можно указать их вручную."}finally{geocoding=false}}},enabled=address.isNotBlank()&&!geocoding){Text(if(geocoding)"Определяем…" else "Определить координаты по адресу")}
            OutlinedTextField(latitude,{latitude=it},label={Text("Широта (необязательно)")},modifier=Modifier.fillMaxWidth(),isError=!validCoordinates)
            OutlinedTextField(longitude,{longitude=it},label={Text("Долгота (необязательно)")},modifier=Modifier.fillMaxWidth(),isError=!validCoordinates)
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button(onClick={onSave(VacancyRequest(title.trim(),city.trim(),experience.trim(),remote,skills.split(',').map{it.trim()}.filter{it.isNotEmpty()},description,address,lat,lon))},enabled=title.isNotBlank()&&city.isNotBlank()&&experience.isNotBlank()&&validCoordinates&&!busy){Text(if(busy)"Сохранение…" else "Сохранить")}
        }
    }
}
