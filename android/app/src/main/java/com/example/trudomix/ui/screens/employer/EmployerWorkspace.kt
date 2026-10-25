package com.example.trudomix.ui.screens.employer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.*
import com.example.trudomix.ui.screens.applications.ApplicationDetailsSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployerWorkspace(state:EmployerState,tab:Int,embedded:Boolean,onTab:(Int)->Unit,onBack:()->Unit,onRefresh:()->Unit,onCompany:()->Unit,onNew:()->Unit,onEdit:(Vacancy)->Unit,onArchive:(Vacancy)->Unit,onDelete:(Vacancy)->Unit,onStatus:(Int,String)->Unit,onChat:(Int)->Unit) {
    var query by rememberSaveable(tab){mutableStateOf("")}
    var filter by rememberSaveable(tab){mutableStateOf(if(tab==0)"active"else "all")}
    var confirmation by remember{mutableStateOf<Pair<JobApplication,String>?>(null)}
    val colors=MaterialTheme.colorScheme
    val jobs=state.vacancies.filter{it.is_archived==(filter=="archive")&&"${it.title} ${it.location}".contains(query.trim(),true)}
    val apps=state.applications.filter{(filter=="all"||it.status==filter)&&"${it.resume_full_name} ${it.candidate_email} ${it.vacancy_title}".contains(query.trim(),true)}
    RefreshContent(state.loading,onRefresh){
        Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),contentAlignment=Alignment.TopCenter){
            LazyColumn(Modifier.widthIn(max=760.dp).fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=20.dp,bottom=if(embedded)148.dp else 32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                item{AppScreenHeader(title={Text(if(!embedded)"Кабинет работодателя"else if(tab==0)"Мои вакансии"else "Отклики")},subtitle=if(tab==0)"Публикации и люди для вашей команды"else "Кандидаты и следующие шаги",applyInsets=false,contentPadding=PaddingValues(0.dp),navigationIcon={if(!embedded)IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}})}
                if(!embedded)item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(tab==0,{onTab(0)},label={Text("Вакансии")});FilterChip(tab==1,{onTab(1)},label={Text("Отклики")})}}
                if(state.error!=null)item{OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(state.error,color=colors.error);TextButton(onClick=onRefresh,enabled=!state.busy){Text("Попробовать снова")}}}}
                if(state.busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
                if(state.loading&&state.company.id==0)item{com.example.trudomix.ui.loading.TrudomixLoadingScreen(compact=true,label="Загружаем кабинет")}
                else if(state.company.id==0)item{WorkspaceEmpty("Начните с вашей компании","Добавьте название и описание, чтобы публиковать вакансии."){Button(onClick=onCompany){Text("Создать компанию")}}}
                else {
                    item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){CompanyAvatar(state.company.logo_url,state.company.name,44.dp);Column(Modifier.weight(1f)){Text(state.company.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(if(tab==0)"Рабочее пространство команды"else "Знакомство с кандидатами",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}}}
                    item{Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        EmployerMetric(state.vacancies.count{!it.is_archived}.toString(),"Активных вакансий",Modifier.weight(1f))
                        EmployerMetric(state.applications.size.toString(),"Всего откликов",Modifier.weight(1f))
                        EmployerMetric(if(tab==0)state.vacancies.sumOf{it.views}.toString()else state.applications.count{it.status=="new"}.toString(),if(tab==0)"Просмотров"else "Новых кандидатов",Modifier.weight(1f))
                    }}
                    if(tab==0)item{Button(onClick=onNew,enabled=!state.busy,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(16.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Новая вакансия")}}
                    item{OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(if(tab==0)"Должность или город"else "Кандидат или вакансия")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(16.dp))}
                    item{LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        val options=if(tab==0)listOf("active" to "Активные","archive" to "Архив")else listOf("all" to "Все","new" to "Новые","viewed" to "В работе","accepted" to "Приглашения","rejected" to "Отказы")
                        items(options){(value,label)->FilterChip(filter==value,{filter=value},label={Text(label)})}
                    }}
                    if(tab==0){
                        if(jobs.isEmpty()&&!state.loading)item{WorkspaceEmpty("Вакансии не найдены",if(query.isNotBlank())"Измените запрос или выберите другую вкладку."else if(filter=="archive")"Здесь будут храниться завершённые публикации."else "Опубликуйте первую вакансию для вашей команды.")}
                        items(jobs,key={it.id}){job->VacancyCard(job,{onEdit(job)},showFavorite=false,footer={
                            HorizontalDivider(color=colors.outlineVariant)
                            EmployerStatus(if(job.is_archived)"В архиве"else "Опубликована",false)
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                OutlinedButton(onClick={onEdit(job)},enabled=!state.busy){Text("Изменить")}
                                OutlinedButton(onClick={onArchive(job)},enabled=!state.busy){Text(if(job.is_archived)"Опубликовать"else "В архив")}
                                TextButton(onClick={onDelete(job)},enabled=!state.busy){Text("Удалить",color=colors.error)}
                            }
                        })}
                    }else{
                        if(apps.isEmpty()&&!state.loading)item{WorkspaceEmpty("Откликов пока нет",if(query.isNotBlank()||filter!="all")"Измените поиск или выберите другой этап."else "Когда кандидаты откликнутся, их резюме появятся здесь.")}
                        items(apps,key={it.id?:0}){app->
                            var letter by rememberSaveable(app.id){mutableStateOf(false)}
                            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,colors.outlineVariant),colors=CardDefaults.cardColors(containerColor=colors.surface)){
                                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                        Surface(shape=CircleShape,color=colors.primaryContainer){Box(Modifier.size(44.dp),contentAlignment=Alignment.Center){Text((app.resume_full_name.ifBlank{app.candidate_email}).take(1).uppercase(),color=colors.onPrimaryContainer)}}
                                        Column(Modifier.weight(1f)){Text(app.resume_full_name.ifBlank{app.candidate_email},style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);if(app.resume_position.isNotBlank())Text(app.resume_position,style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}
                                    }
                                    EmployerStatus(applicationStatusLabel(app.status),app.status=="rejected")
                                    Text(app.vacancy_title.orEmpty(),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                                    ApplicationDetailsSummary(app.feedback,app.interview_at)
                                    if(!app.cover_letter.isNullOrBlank()){TextButton(onClick={letter=!letter}){Text(if(letter)"Скрыть письмо"else "Сопроводительное письмо")};if(letter)Text(app.cover_letter,style=MaterialTheme.typography.bodyMedium)}
                                    HorizontalDivider(color=colors.outlineVariant)
                                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                        Button(onClick={app.id?.let(onChat)},enabled=!state.busy){Icon(Icons.Default.ChatBubbleOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Чат")}
                                        if(app.status=="new")OutlinedButton(onClick={app.id?.let{onStatus(it,"viewed")}},enabled=!state.busy){Text("В работу")}
                                        if(app.status!="accepted")OutlinedButton(onClick={confirmation=app to "accepted"},enabled=!state.busy){Text("Пригласить")}
                                        if(app.status!="rejected")TextButton(onClick={confirmation=app to "rejected"},enabled=!state.busy){Text("Отказать",color=colors.error)}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmation?.let{(app,status)->AlertDialog(onDismissRequest={confirmation=null},title={Text(if(status=="accepted")"Пригласить кандидата?"else "Отправить отказ?")},text={Text("Кандидат получит уведомление по вакансии «${app.vacancy_title.orEmpty()}».")},confirmButton={TextButton(onClick={app.id?.let{onStatus(it,status)};confirmation=null}){Text(if(status=="accepted")"Пригласить"else "Отказать")}},dismissButton={TextButton(onClick={confirmation=null}){Text("Отмена")}})}
}

@Composable
private fun EmployerMetric(value:String,label:String,modifier:Modifier){OutlinedCard(modifier.fillMaxHeight(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable
private fun EmployerStatus(label:String,error:Boolean){val tone=if(error)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary;Surface(shape=CircleShape,color=tone.copy(alpha=.1f),contentColor=tone){Text(label,Modifier.padding(horizontal=12.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium)}}
@Composable
private fun WorkspaceEmpty(title:String,description:String,action:(@Composable ()->Unit)?=null){OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.WorkOutline,null,tint=MaterialTheme.colorScheme.primary);Text(title,style=MaterialTheme.typography.titleLarge);Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant);action?.invoke()}}}
