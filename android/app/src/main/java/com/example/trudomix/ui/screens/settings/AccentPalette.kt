package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trudomix.data.local.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AccentPalette(){
    val preset by AccentColors.selected.collectAsStateWithLifecycle()
    val custom by AccentColors.custom.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var open by remember{mutableStateOf(false)}
    val dark=MaterialTheme.colorScheme.background.luminance()<.5f
    val chosen=custom?:if(dark)preset.dark else preset.light
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Box(Modifier.size(44.dp).background(chosen,CircleShape).border(1.dp,MaterialTheme.colorScheme.outlineVariant,CircleShape))
                Column{Text("Цвет интерфейса",style=MaterialTheme.typography.titleMedium);Text(custom?.let(::accentHex)?:preset.title,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            FlowRow(Modifier.selectableGroup(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                AccentColor.entries.forEach{color->
                    val selected=custom==null&&preset==color
                    val swatch=if(dark)color.dark else color.light
                    Box(Modifier.size(46.dp).selectable(selected,role=Role.RadioButton,onClick={AccentColors.select(context,color)}).semantics{contentDescription=color.title},contentAlignment=Alignment.Center){
                        Box(Modifier.size(34.dp).background(swatch,CircleShape).border(if(selected)3.dp else 1.dp,if(selected)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,CircleShape),contentAlignment=Alignment.Center){if(selected)Icon(Icons.Default.Check,null,Modifier.size(19.dp),tint=accentForeground(swatch))}
                    }
                }
            }
            OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.Palette,null);Spacer(Modifier.width(8.dp));Text("Открыть палитру")}
            Text("Свой оттенок для кнопок и выделений. Для читаемости он адаптируется к теме. Цвета логотипа сохраняются.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if(open)AccentPaletteDialog(chosen,{open=false}){AccentColors.selectCustom(context,it);open=false}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun AccentPaletteDialog(initial:Color,onDismiss:()->Unit,onApply:(Color)->Unit){
    val initialHsv=remember(initial){FloatArray(3).also{android.graphics.Color.colorToHSV(initial.toArgb(),it)}}
    var hue by remember{mutableFloatStateOf(initialHsv[0])}
    var saturation by remember{mutableFloatStateOf(initialHsv[1])}
    var value by remember{mutableFloatStateOf(initialHsv[2])}
    var hex by remember{mutableStateOf(accentHex(initial))}
    val color=Color.hsv(hue.coerceIn(0f,360f),saturation,value)
    fun update(h:Float=hue,s:Float=saturation,v:Float=value){hue=h;saturation=s.coerceIn(0f,1f);value=v.coerceIn(0f,1f);hex=accentHex(Color.hsv(hue,saturation,value))}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Палитра интерфейса")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            val latest=rememberUpdatedState<(Offset,androidx.compose.ui.geometry.Size)->Unit>({point,size->update(s=point.x/size.width,v=1-point.y/size.height)})
            val hueBrush=remember(hue){Brush.horizontalGradient(listOf(Color.White,Color.hsv(hue,1f,1f)))}
            val darkness=remember{Brush.verticalGradient(listOf(Color.Transparent,Color.Black))}
            val rainbow=remember{Brush.horizontalGradient(listOf(Color.Red,Color.Yellow,Color.Green,Color.Cyan,Color.Blue,Color.Magenta,Color.Red))}
            Canvas(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).semantics{contentDescription="Насыщенность и яркость"}.pointerInput(Unit){detectTapGestures{latest.value(it,androidx.compose.ui.geometry.Size(size.width.toFloat(),size.height.toFloat()))}}.pointerInput(Unit){detectDragGestures{change,_->change.consume();latest.value(change.position,androidx.compose.ui.geometry.Size(size.width.toFloat(),size.height.toFloat()))}}){
                drawRect(hueBrush);drawRect(darkness)
                val point=Offset(saturation*size.width,(1-value)*size.height)
                drawCircle(Color.Black,7.dp.toPx(),point,style=Stroke(3.dp.toPx()));drawCircle(Color.White,7.dp.toPx(),point,style=Stroke(1.5.dp.toPx()))
            }
            Text("Оттенок",style=MaterialTheme.typography.labelLarge)
            Slider(hue,{update(h=it)},valueRange=0f..360f,modifier=Modifier.semantics{contentDescription="Оттенок цвета"},colors=SliderDefaults.colors(thumbColor=Color.hsv(hue,1f,1f)),track={Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(rainbow))})
            // Sliders provide keyboard/TalkBack access as well as the touch palette.
            Text("Насыщенность",style=MaterialTheme.typography.labelLarge)
            Slider(saturation,{update(s=it)},modifier=Modifier.semantics{contentDescription="Насыщенность цвета"})
            Text("Яркость",style=MaterialTheme.typography.labelLarge)
            Slider(value,{update(v=it)},modifier=Modifier.semantics{contentDescription="Яркость цвета"})
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Box(Modifier.size(48.dp).background(color,CircleShape).border(1.dp,MaterialTheme.colorScheme.outlineVariant,CircleShape))
                OutlinedTextField(hex,{text->hex=text.take(7);parseAccentHex(hex)?.let{parsed->val hsv=FloatArray(3);android.graphics.Color.colorToHSV(parsed.toArgb(),hsv);hue=hsv[0];saturation=hsv[1];value=hsv[2]}},label={Text("HEX")},placeholder={Text("#8257E6")},singleLine=true,isError=parseAccentHex(hex)==null,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),modifier=Modifier.weight(1f))
            }
            Text("Предпросмотр",style=MaterialTheme.typography.labelLarge)
            Surface(color=color,contentColor=accentForeground(color),shape=RoundedCornerShape(12.dp)){Text("Цвет кнопки",Modifier.padding(16.dp))}
        }
    },confirmButton={TextButton(enabled=parseAccentHex(hex)!=null,onClick={parseAccentHex(hex)?.let(onApply)}){Text("Применить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}
