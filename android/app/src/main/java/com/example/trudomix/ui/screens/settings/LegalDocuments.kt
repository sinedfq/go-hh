package com.example.trudomix.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class LegalCatalog(val version:String,val status:String,val contactEmail:String,val notice:String,val documents:List<LegalDocument>)
@Serializable data class LegalDocument(val id:String,val title:String,val subtitle:String,val sections:List<LegalSection>)
@Serializable data class LegalSection(val heading:String,val text:String)

@Composable fun LegalDocuments(){
    val context=LocalContext.current
    val catalog=remember(context){runCatching{context.assets.open("documents.json").bufferedReader().use{Json{ignoreUnknownKeys=true}.decodeFromString<LegalCatalog>(it.readText())}}.getOrNull()}
    var selected by remember{mutableStateOf<LegalDocument?>(null)}
    fun contact(){
        val email=catalog?.contactEmail?:return
        runCatching{context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:$email")))}.onFailure{Toast.makeText(context,"Почта для обращений: $email",Toast.LENGTH_LONG).show()}
    }
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Документы",style=MaterialTheme.typography.titleMedium)
            if(catalog==null)Text("Не удалось открыть документы",style=MaterialTheme.typography.bodyMedium)
            else{
                Text("Проекты документов сервиса",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                catalog.documents.forEach{document->
                    OutlinedCard(onClick={selected=document},modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(document.title,style=MaterialTheme.typography.bodyLarge)
                            Text(document.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TextButton(onClick=::contact){Text("Связаться с нами")}
                SelectionContainer{Text(catalog.contactEmail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
    }
    selected?.let{document->
        Dialog(onDismissRequest={selected=null},properties=DialogProperties(usePlatformDefaultWidth=false)){
            Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.92f),shape=MaterialTheme.shapes.extraLarge){
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text(document.title,style=MaterialTheme.typography.titleLarge)
                    Box(Modifier.weight(1f).fillMaxWidth()){
                    SelectionContainer{
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
                            Text(catalog?.notice.orEmpty(),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
                            Text("Редакция ${catalog?.version}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            document.sections.forEach{section->
                                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                                    Text(section.heading,style=MaterialTheme.typography.titleMedium)
                                    Text(section.text,style=MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        TextButton(onClick=::contact){Text("Написать нам")}
                        TextButton(onClick={selected=null}){Text("Закрыть")}
                    }
                }
            }
        }
    }
}
