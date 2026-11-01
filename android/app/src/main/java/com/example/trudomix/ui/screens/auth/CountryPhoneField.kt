package com.example.trudomix.ui.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlin.math.abs
import kotlinx.coroutines.flow.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

data class PhoneCountry(val id:String,val flag:String,val dial:String,val name:String)
val phoneCountries=listOf(
    PhoneCountry("RU","🇷🇺","+7","Россия"),PhoneCountry("KZ","🇰🇿","+7","Казахстан"),
    PhoneCountry("BY","🇧🇾","+375","Беларусь"),PhoneCountry("UA","🇺🇦","+380","Украина"),
    PhoneCountry("UZ","🇺🇿","+998","Узбекистан"),PhoneCountry("KG","🇰🇬","+996","Кыргызстан"),
    PhoneCountry("AM","🇦🇲","+374","Армения"),PhoneCountry("GE","🇬🇪","+995","Грузия"),
    PhoneCountry("AZ","🇦🇿","+994","Азербайджан"),PhoneCountry("TR","🇹🇷","+90","Турция"),
    PhoneCountry("AE","🇦🇪","+971","ОАЭ"),PhoneCountry("DE","🇩🇪","+49","Германия"),
    PhoneCountry("FR","🇫🇷","+33","Франция"),PhoneCountry("GB","🇬🇧","+44","Великобритания"),
    PhoneCountry("US","🇺🇸","+1","США"),PhoneCountry("CA","🇨🇦","+1","Канада"),
    PhoneCountry("IT","🇮🇹","+39","Италия"),PhoneCountry("ES","🇪🇸","+34","Испания"),
    PhoneCountry("IL","🇮🇱","+972","Израиль"),PhoneCountry("IN","🇮🇳","+91","Индия"),
    PhoneCountry("CN","🇨🇳","+86","Китай"),PhoneCountry("JP","🇯🇵","+81","Япония"),
    PhoneCountry("AU","🇦🇺","+61","Австралия")
)
fun phoneCountry(value:String,preferred:String="RU"):PhoneCountry=
    phoneCountries.firstOrNull{it.id==preferred&&value.startsWith(it.dial)}
        ?:phoneCountries.sortedByDescending{it.dial.length}.firstOrNull{value.startsWith(it.dial)}
        ?:if(value.isBlank())phoneCountries.first()else PhoneCountry("ZZ","🌐","+","Другой код")

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable fun CountryPhoneField(value:String,onChange:(String)->Unit,enabled:Boolean){
    var preferred by rememberSaveable{mutableStateOf("RU")}
    var choosing by rememberSaveable{mutableStateOf(false)}
    val country=phoneCountry(value,preferred)
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val national=value.removePrefix(country.dial)
    OutlinedTextField(national,{input->
        if(input.startsWith("+")||(input.filter(Char::isDigit).length==11&&input.startsWith("8")&&country.dial=="+7"))onChange(normalizedPhoneInput(input))
        else onChange(country.dial+input.filter{it in '0'..'9'}.take(if(country.dial=="+7")10 else 15-country.dial.length+1))
    },label={Text("Номер телефона")},placeholder={Text(if(country.dial=="+7")"(999) 123-45-67"else "Номер без кода страны")},
        leadingIcon={TextButton(enabled=enabled,onClick={focus.clearFocus();keyboard?.hide();choosing=true},contentPadding=PaddingValues(horizontal=10.dp)){Text("${country.flag} ${country.dial} ▾")}},
        visualTransformation=if(country.dial=="+7")NationalPhoneTransformation else VisualTransformation.None,
        singleLine=true,enabled=enabled,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth())
    if(choosing){
        val initial=phoneCountries.indexOfFirst{it.id==country.id}.coerceAtLeast(0)
        val wheel=rememberLazyListState(initialFirstVisibleItemIndex=initial)
        var selected by remember{mutableIntStateOf(initial)}
        LaunchedEffect(wheel){snapshotFlow{
            val info=wheel.layoutInfo;val center=(info.viewportStartOffset+info.viewportEndOffset)/2
            info.visibleItemsInfo.minByOrNull{abs(it.offset+it.size/2-center)}?.index?:initial
        }.distinctUntilChanged().collect{selected=it}}
        ModalBottomSheet(onDismissRequest={choosing=false}){
            Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("Код страны",style=MaterialTheme.typography.titleLarge)
                Text("Прокрутите список и выберите страну",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().height(240.dp)){
                    Box(Modifier.align(Alignment.Center).fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(12.dp)))
                    LazyColumn(state=wheel,flingBehavior=rememberSnapFlingBehavior(wheel),contentPadding=PaddingValues(vertical=96.dp),modifier=Modifier.fillMaxSize().semantics{contentDescription="Коды стран"}){
                        itemsIndexed(phoneCountries,key={_,item->item.id}){index,item->
                            val distance=abs(index-selected)
                            Row(Modifier.fillMaxWidth().height(48.dp).graphicsLayer{alpha=when(distance){0->1f;1->.65f;else->.35f}}.clickable{selected=index;preferred=item.id;onChange(item.dial+national.take(if(item.dial=="+7")10 else 15-item.dial.length+1));choosing=false}.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                Text(item.flag,style=MaterialTheme.typography.titleLarge)
                                Text(item.dial,style=MaterialTheme.typography.titleMedium,modifier=Modifier.width(64.dp))
                                Text(item.name,style=MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Button(onClick={val item=phoneCountries[selected];preferred=item.id;onChange(item.dial+national.take(if(item.dial=="+7")10 else 15-item.dial.length+1));choosing=false},modifier=Modifier.fillMaxWidth()){Text("Выбрать ${phoneCountries[selected].flag} ${phoneCountries[selected].dial}")}
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

object NationalPhoneTransformation:VisualTransformation{
    override fun filter(text:AnnotatedString):TransformedText{
        val raw=text.text;if(raw.isEmpty())return TransformedText(text,OffsetMapping.Identity)
        val output=StringBuilder();val offsets=IntArray(raw.length+1)
        raw.forEachIndexed{i,c->when(i){0->output.append("(");3->output.append(") ");6,8->output.append("-")};offsets[i]=output.length;output.append(c)}
        offsets[raw.length]=output.length
        return TransformedText(AnnotatedString(output.toString()),object:OffsetMapping{
            override fun originalToTransformed(offset:Int)=offsets[offset.coerceIn(0,raw.length)]
            override fun transformedToOriginal(offset:Int)=offsets.indexOfFirst{it>=offset}.let{if(it<0)raw.length else it}.coerceIn(0,raw.length)
        })
    }
}

fun formattedAccountPhone(phone:String):String=PhoneVisualTransformation.filter(AnnotatedString(phone)).text.text
