package com.example.trudomix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.models.Vacancy

/** Main content shares one 20dp leading edge in search and employer cards. */
@Composable
fun VacancyCard(vacancy:Vacancy,onClick:()->Unit,onFavoriteToggle:(Boolean)->Unit={},showFavorite:Boolean=true,footer:(@Composable ()->Unit)?=null) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                CompanyAvatar(vacancy.company_logo,vacancy.company,44.dp)
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(vacancy.company,Modifier.weight(1f,fill=false),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    if(vacancy.company_it_accredited)ITAccreditationBadge()
                }
                if(showFavorite)IconButton(onClick={onFavoriteToggle(!vacancy.is_favorite)}){Icon(if(vacancy.is_favorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Избранное",tint=if(vacancy.is_favorite)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            Text(vacancy.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface,maxLines=3,overflow=TextOverflow.Ellipsis)
            VacancyTerms(vacancy)
            if(vacancy.skills.isNotEmpty())LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                items(vacancy.skills){skill->Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){Text(skill,Modifier.padding(horizontal=10.dp,vertical=7.dp),style=MaterialTheme.typography.labelSmall)}}
            }
            footer?.invoke()
        }
    }
}
