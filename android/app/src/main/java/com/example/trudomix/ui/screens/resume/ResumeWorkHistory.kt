package com.example.trudomix.ui.screens.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.models.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeWorkHistory(resume:Resume,busy:Boolean,onAdd:(WorkExperienceRequest)->Unit,onDelete:(Int)->Unit){
    var adding by remember{mutableStateOf(false)}
    var deleting by remember{mutableStateOf<WorkExperience?>(null)}
    Text("Общий опыт: ${resume.total_experience.ifBlank{"Не указан"}}",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
    Text("Периоды параллельной работы считаются один раз.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    resume.work_experience.forEach{job->
        HorizontalDivider()
        Text(job.position,style=MaterialTheme.typography.titleSmall)
        Text(job.company)
        Text("${job.start_date.take(10)} — ${job.end_date?.take(10)?:"настоящее время"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(job.description.isNotBlank())Text(job.description,style=MaterialTheme.typography.bodyMedium)
        TextButton(enabled=!busy,onClick={deleting=job}){Text("Удалить место работы",color=MaterialTheme.colorScheme.error)}
    }
    OutlinedButton(enabled=!busy,onClick={adding=true}){Text("Добавить место работы")}
    if(adding){
        var company by remember{mutableStateOf("")};var position by remember{mutableStateOf("")};var start by remember{mutableStateOf("")};var end by remember{mutableStateOf("")};var description by remember{mutableStateOf("")};var current by remember{mutableStateOf(true)}
        val startDate=runCatching{LocalDate.parse(start)}.getOrNull();val endDate=if(current)LocalDate.now()else runCatching{LocalDate.parse(end)}.getOrNull()
        val valid=startDate!=null&&endDate!=null&&!startDate.isAfter(endDate)&&!endDate.isAfter(LocalDate.now())
        AlertDialog(onDismissRequest={if(!busy)adding=false},title={Text("Место работы")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedTextField(company,{company=it},label={Text("Компания")},singleLine=true)
            OutlinedTextField(position,{position=it},label={Text("Должность")},singleLine=true)
            OutlinedTextField(start,{start=it},label={Text("Начало · ГГГГ-ММ-ДД")},singleLine=true,isError=start.isNotBlank()&&startDate==null)
            Row{Checkbox(current,{current=it});Text("Работаю сейчас",Modifier.padding(top=12.dp))}
            if(!current)OutlinedTextField(end,{end=it},label={Text("Окончание · ГГГГ-ММ-ДД")},singleLine=true,isError=end.isNotBlank()&&!valid)
            OutlinedTextField(description,{description=it.take(10000)},label={Text("Обязанности и достижения")},minLines=3)
            Text("Например: 2024-01-15. Дата окончания не может быть раньше начала или позже сегодня.",style=MaterialTheme.typography.bodySmall)
        }},confirmButton={TextButton(enabled=!busy&&company.isNotBlank()&&position.isNotBlank()&&valid,onClick={onAdd(WorkExperienceRequest(company.trim(),position.trim(),start,if(current)null else end,description));adding=false}){Text("Сохранить")}},dismissButton={TextButton(enabled=!busy,onClick={adding=false}){Text("Отмена")}})
    }
    deleting?.let{job->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить место работы?")},text={Text(job.company)},confirmButton={TextButton(onClick={onDelete(job.id);deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}
