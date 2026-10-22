package com.example.trudomix.ui.screens.applications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.models.JobApplication
import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.components.AppSnackbarHost

@Composable
fun ApplicationsScreen(viewModel: ApplicationsViewModel = viewModel(), onVacancyClick:(Int)->Unit={}, onChatClick:(Int)->Unit={}) {
    val state by viewModel.uiState.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val snackbar=remember{SnackbarHostState()}
    LaunchedEffect(viewModel){viewModel.messageEvent.collect{snackbar.showSnackbar(it)}}
    Box(Modifier.fillMaxSize()) {
        ApplicationsContent(state,busy,viewModel::loadApplications,onVacancyClick,{app->
            app.conversation_id?.takeIf{it>0}?.let(onChatClick) ?: app.id?.let{viewModel.startChat(it,onChatClick)}
        },{it.id?.let(viewModel::cancelApplication)})
        AppSnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=108.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplicationsContent(state:ApplicationsUiState,busy:Set<Int> = emptySet(),onRefresh:()->Unit={},onVacancy:(Int)->Unit={},onChat:(JobApplication)->Unit={},onCancel:(JobApplication)->Unit={}) {
    var filter by rememberSaveable{mutableStateOf("all")}
    var cancelling by remember{mutableStateOf<JobApplication?>(null)}
    val apps=(state as? ApplicationsUiState.Success)?.applications.orEmpty()
    val filtered=apps.filter{when(filter){"active"->it.status in listOf("new","viewed");"accepted"->it.status=="accepted";"rejected"->it.status=="rejected";else->true}}
    val colors=MaterialTheme.colorScheme
    com.example.trudomix.ui.components.RefreshContent(state is ApplicationsUiState.Loading,onRefresh) {
    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),contentAlignment=Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max=760.dp).fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=20.dp,bottom=148.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                AppScreenHeader(title={Text("Отклики")},subtitle="Ответы компаний и твои следующие шаги",applyInsets=false,contentPadding=PaddingValues(0.dp),
)
            }
            item { LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(listOf("all" to "Все","active" to "В работе","accepted" to "Принятые","rejected" to "Отказы")){(key,label)->FilterChip(filter==key,{filter=key},label={Text(label)})}
            } }
            when(state) {
                ApplicationsUiState.Loading -> item{LinearProgressIndicator(Modifier.fillMaxWidth())}
                is ApplicationsUiState.Error -> item { OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text("Не удалось загрузить отклики",style=MaterialTheme.typography.titleMedium);Text(state.message,color=colors.onSurfaceVariant);Button(onClick=onRefresh){Text("Попробовать снова")}
                }} }
                is ApplicationsUiState.Success -> {
                    item{Text("${filtered.size} из ${apps.size} откликов",style=MaterialTheme.typography.labelMedium,color=colors.onSurfaceVariant)}
                    if(filtered.isEmpty())item { OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Icon(Icons.Default.MarkEmailUnread,null,tint=colors.primary,modifier=Modifier.size(32.dp))
                        Text(if(apps.isEmpty())"Первые возможности впереди" else "Здесь пока нет откликов",style=MaterialTheme.typography.titleMedium)
                        Text(if(apps.isEmpty())"Найди интересную вакансию и отправь отклик. Ответ компании появится здесь." else "Попробуй другой фильтр.",color=colors.onSurfaceVariant)
                    }} }
                    items(filtered,key={it.id?:it.hashCode()}){app->
                        var letter by rememberSaveable(app.id){mutableStateOf(false)}
                        OutlinedCard(modifier=Modifier.fillMaxWidth().animateItem(),shape=RoundedCornerShape(24.dp)){
                            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    val tone=when(app.status){"accepted"->colors.tertiary;"rejected"->colors.error;else->colors.primary}
                                    Surface(color=tone.copy(alpha=.1f),contentColor=tone,shape=CircleShape){Text(applicationLabel(app.status),Modifier.padding(horizontal=12.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium)}
                                    Spacer(Modifier.weight(1f))
                                    if(app.status=="new")IconButton(onClick={cancelling=app},enabled=app.id !in busy){Icon(Icons.Default.Close,"Отозвать отклик")}
                                }
                                Text(app.vacancy_title?:"Вакансия",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold,color=colors.onSurface)
                                Text(app.company_name.orEmpty(),style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)
                                if(app.vacancy_archived)Text("Вакансия в архиве",style=MaterialTheme.typography.labelMedium,color=colors.onSurfaceVariant)
                                app.created_at?.let{Text("Отправлен ${applicationDate(it)}",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)}
                                if(!app.cover_letter.isNullOrBlank()) {
                                    TextButton(onClick={letter=!letter}){Icon(Icons.Default.Description,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(letter)"Скрыть письмо" else "Моё сопроводительное письмо")}
                                    AnimatedVisibility(letter){Text(app.cover_letter,style=MaterialTheme.typography.bodyMedium)}
                                }
                                HorizontalDivider(color=colors.outlineVariant)
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                    Button(onClick={onChat(app)},enabled=app.id!=null&&app.id !in busy){Icon(Icons.Default.ChatBubbleOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(app.id in busy)"Открываем…" else "Открыть чат")}
                                    OutlinedButton(onClick={app.vacancy_id?.let(onVacancy)},enabled=app.vacancy_id!=null){Text("Вакансия")}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    cancelling?.let{app->AlertDialog(onDismissRequest={cancelling=null},title={Text("Отозвать отклик?")},text={Text("Отклик на «${app.vacancy_title.orEmpty()}» будет удалён.")},confirmButton={TextButton(onClick={cancelling=null;onCancel(app)}){Text("Отозвать",color=colors.error)}},dismissButton={TextButton(onClick={cancelling=null}){Text("Оставить")}})}
}
fun applicationLabel(status:String?):String=when(status){"new"->"Отправлен";"viewed"->"Просмотрен";"accepted"->"Принят";"rejected"->"Отказ";else->"На рассмотрении"}
private fun applicationDate(value:String):String=runCatching{java.time.OffsetDateTime.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrElse{value.take(10)}
