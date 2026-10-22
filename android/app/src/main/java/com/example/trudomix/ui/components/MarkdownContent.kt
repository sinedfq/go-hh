package com.example.trudomix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient
import kotlinx.coroutines.*
import org.commonmark.node.*
import org.commonmark.parser.Parser

private fun children(node:Node):List<Node> = buildList {var n=node.firstChild;while(n!=null){add(n);n=n.next}}
private fun safeMarkdownUrl(raw:String):String?=raw.takeIf{it.startsWith("https://",true)||it.startsWith("http://",true)||it.startsWith("/uploads/")}

@Composable
fun MarkdownContent(text:String){
    val document by produceState<Node?>(null,text){value=withContext(Dispatchers.Default){Parser.builder().build().parse(text.take(50000))}}
    document?.let{MarkdownBlock(it)}
}

@Composable
private fun MarkdownBlock(node:Node){
    when(node){
        is Document->Column(verticalArrangement=Arrangement.spacedBy(12.dp)){children(node).forEach{MarkdownBlock(it)}}
        is org.commonmark.node.Paragraph->{
            val content=markdownInline(node,MaterialTheme.colorScheme.primary)
            if(content.isNotBlank())SelectionContainer{Text(content,style=MaterialTheme.typography.bodyMedium)}
            children(node).filterIsInstance<Image>().forEach{image->safeMarkdownUrl(image.destination)?.let{url->AsyncImage(ApiClient.mediaUrl(url),children(image).filterIsInstance<org.commonmark.node.Text>().joinToString(""){it.literal},Modifier.fillMaxWidth().aspectRatio(16f/9f).heightIn(max=360.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit)}}
        }
        is Heading->Text(markdownInline(node,MaterialTheme.colorScheme.primary),style=if(node.level<=2)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        is BulletList->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){children(node).forEach{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("•");Column(Modifier.weight(1f)){children(it).forEach{n->MarkdownBlock(n)}}}}}
        is OrderedList->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){children(node).forEachIndexed{i,item->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("${node.markerStartNumber+i}.");Column(Modifier.weight(1f)){children(item).forEach{MarkdownBlock(it)}}}}}
        is BlockQuote->Surface(color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.padding(14.dp)){children(node).forEach{MarkdownBlock(it)}}}
        is FencedCodeBlock->Text(node.literal,Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).horizontalScroll(rememberScrollState()).padding(14.dp),fontFamily=FontFamily.Monospace)
        is IndentedCodeBlock->Text(node.literal,fontFamily=FontFamily.Monospace)
        is ThematicBreak->HorizontalDivider()
        is HtmlBlock->Unit // Raw HTML is never executed or embedded.
        else->children(node).forEach{MarkdownBlock(it)}
    }
}

private fun markdownInline(root:Node,accent:Color):AnnotatedString=buildAnnotatedString{
    fun appendNode(node:Node){
        when(node){
            is org.commonmark.node.Text->append(node.literal)
            is SoftLineBreak->append(" ")
            is HardLineBreak->append("\n")
            is StrongEmphasis->withStyle(SpanStyle(fontWeight=FontWeight.Bold)){children(node).forEach{appendNode(it)}}
            is Emphasis->withStyle(SpanStyle(fontStyle=FontStyle.Italic)){children(node).forEach{appendNode(it)}}
            is Code->withStyle(SpanStyle(fontFamily=FontFamily.Monospace)){append(node.literal)}
            is Link->{val url=safeMarkdownUrl(node.destination);if(url!=null)withLink(LinkAnnotation.Url(url,TextLinkStyles(style=SpanStyle(color=accent)))){children(node).forEach{appendNode(it)}} else children(node).forEach{appendNode(it)}}
            is Image,is HtmlInline->Unit
            else->children(node).forEach{appendNode(it)}
        }
    }
    children(root).forEach{appendNode(it)}
}
