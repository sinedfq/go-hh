package com.example.trudomix.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.models.Resume

@Composable
fun ProfileDashboard(state: ProfileOverview, candidate: Boolean, onResume: () -> Unit, onApplications: () -> Unit, onFavorites: () -> Unit, onRetry: () -> Unit) {
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    Column(verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()) {
        Text("Моя активность",style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ProfileMetric(if(candidate)"Просмотры резюме" else "Просмотры вакансий", if(candidate)state.views else state.employer?.total_vacancy_views, Icons.Default.Visibility,if(candidate)onResume else onApplications,Modifier.weight(1f))
            ProfileMetric("Отклики",if(candidate)state.applications else state.employer?.total_applications,Icons.Default.Send,onApplications,Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ProfileMetric(if(candidate)"Принятые отклики" else "Новые отклики",if(candidate)state.accepted else state.employer?.new_applications,Icons.Default.CheckCircleOutline,onApplications,Modifier.weight(1f))
            ProfileMetric(if(candidate)"Избранное" else "Вакансии",if(candidate)state.favorites else state.employer?.total_vacancies,Icons.Default.FavoriteBorder,if(candidate)onFavorites else onApplications,Modifier.weight(1f))
        }
        if (state.error) {
            Text("Часть статистики недоступна. Уже загруженные данные сохранены.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick=onRetry){Text("Обновить статистику")}
        }
        if(candidate && (!state.loading || state.resume!=null)) {
            val resume=state.resume
            val fields=resume?.let{resumeCompleteness(it)} ?: 0
            OutlinedCard(onClick=onResume,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(if(!state.resumeKnown)"Резюме пока недоступно" else if(resume==null)"Расскажи о себе работодателям" else resume.desired_position.ifBlank{"Моё резюме"},style=MaterialTheme.typography.titleMedium)
                    if(resume!=null) {
                        LinearProgressIndicator(progress={fields/8f},modifier=Modifier.fillMaxWidth())
                        Text("Заполнено: ${com.example.trudomix.data.models.resumeCompletionLabel(fields)}",style=MaterialTheme.typography.bodySmall)
                        Text(if(fields<8)"Добавь опыт, навыки и контакты — так компании узнают о тебе больше." else "Резюме заполнено. Поддерживай информацию актуальной.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    } else Text(if(state.resumeKnown)"Создай резюме, чтобы откликаться на вакансии." else "Открой раздел резюме или повтори загрузку статистики.",style=MaterialTheme.typography.bodyMedium)
                    Text(if(resume==null && state.resumeKnown)"Создать резюме →" else "Посмотреть и дополнить →",color=MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
fun resumeCompleteness(resume: Resume): Int = com.example.trudomix.data.models.resumeCompletionCount(resume)

@Composable
private fun ProfileMetric(label:String,value:Int?,icon:ImageVector,onClick:()->Unit,modifier:Modifier) {
    Card(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(22.dp))
            Text(value?.toString()?:"—",style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.onSurface)
            Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
