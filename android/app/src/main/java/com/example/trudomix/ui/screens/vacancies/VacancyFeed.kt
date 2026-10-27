package com.example.trudomix.ui.screens.vacancies

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.trudomix.ui.components.VacancyCard
import com.example.trudomix.ui.components.AppScreenHeader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VacancyFeed(
    state: VacancyListUiState, query: String, favorites: Boolean, total: Int, loading: Boolean, error: String?,
    onQuery: (String) -> Unit, onFavorites: () -> Unit, onFilters: () -> Unit, onRefresh: () -> Unit,
    onMore: () -> Unit, onVacancy: (Int) -> Unit, onFavorite: (Int, Boolean) -> Unit,
    onSavedSearches:()->Unit={},onSaveSearch:()->Unit={},resetScrollKey:Int=0
) {
    val colors = MaterialTheme.colorScheme
    val grid = rememberSaveable(favorites,resetScrollKey,saver=LazyGridState.Saver){LazyGridState()}
    val cardWidth = if (LocalDensity.current.fontScale > 1.3f) 420.dp else 320.dp
    var scrollingUp by remember { mutableStateOf(false) }
    var floatingFocused by remember { mutableStateOf(false) }
    val focusManager=LocalFocusManager.current
    var toolbarTop by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var feedTop by remember { mutableFloatStateOf(0f) }
    val showFloating by remember { derivedStateOf { floatingFocused || (scrollingUp && grid.canScrollBackward && (grid.firstVisibleItemIndex>0 || toolbarTop<feedTop)) } }
    val scrollConnection=remember { object:NestedScrollConnection {
        override fun onPreScroll(available:Offset,source:NestedScrollSource):Offset {
            if(source==NestedScrollSource.UserInput && kotlin.math.abs(available.y)>1f) {
                scrollingUp=available.y>0
                if(available.y<0 && floatingFocused)focusManager.clearFocus()
            }
            return Offset.Zero
        }
    } }
    com.example.trudomix.ui.components.RefreshContent(loading&&state !is VacancyListUiState.Loading,onRefresh) {
    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().onGloballyPositioned{feedTop=it.boundsInRoot().top}.nestedScroll(scrollConnection), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(columns = GridCells.Adaptive(cardWidth), state = grid,
            modifier = Modifier.widthIn(max = 1200.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "search-header") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppScreenHeader(applyInsets=false,showProfileAction=false,contentPadding=PaddingValues(0.dp),title={
                            AnimatedContent(favorites, transitionSpec = { (fadeIn(tween(180)) + slideInVertically { it / 4 }).togetherWith(fadeOut(tween(100))) }, label = "vacancy-title") { saved ->
                                Text(if (saved) "Твои избранные" else "Найди свою команду", style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold, color = colors.onBackground)
                            }
                    },
                        subtitle=if(favorites)"Вакансии, к которым хочется вернуться" else "Новые возможности начинаются здесь")
                    VacancySearchRow(query,onQuery,onFilters,Modifier.onGloballyPositioned{toolbarTop=it.boundsInRoot().top})
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = favorites, onClick = onFavorites, label = { Text("Избранное") }, leadingIcon = { Icon(if (favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(18.dp)) })
                        TextButton(onClick=onSavedSearches){Text("Мои поиски")}
                        if(!favorites)TextButton(onClick=onSaveSearch){Text("Сохранить поиск")}
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                }
            }
            when (state) {
                VacancyListUiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    com.example.trudomix.ui.loading.TrudomixLoadingScreen(compact=true,label="Загружаем вакансии")
                }
                is VacancyListUiState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                    FeedNotice("Не получилось загрузить вакансии", state.message, "Попробовать снова", onRefresh)
                }
                is VacancyListUiState.Success -> {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Найдено вакансий: $total", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
                    if (state.vacancies.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        FeedNotice(if (favorites) "Здесь будут избранные" else "Пока ничего не найдено", if (favorites) "Сохраняй интересные вакансии нажатием на сердечко" else "Попробуй другой запрос или измени фильтры", if (favorites) "К поиску" else "Изменить фильтры", if (favorites) onFavorites else onFilters)
                    }
                    items(state.vacancies, key = { it.id }) { vacancy ->
                        Box(Modifier.animateItem()) { VacancyCard(vacancy, { onVacancy(vacancy.id) }, { onFavorite(vacancy.id, it) }) }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            error?.let { Text(it, color = colors.error) }
                            if (!favorites && state.vacancies.size < total) OutlinedButton(onClick = onMore, enabled = !loading, modifier = Modifier.heightIn(min = 52.dp)) { Text(if (loading) "Загружаем…" else "Показать ещё вакансии") }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(showFloating,enter=fadeIn()+slideInVertically{ -it },exit=fadeOut()+slideOutVertically{ -it },modifier=Modifier.align(Alignment.TopCenter).widthIn(max=1200.dp).fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp).testTag("floating-vacancy-search")) {
            Surface(shape=RoundedCornerShape(24.dp),color=colors.surface,shadowElevation=8.dp,modifier=Modifier.pointerInput(Unit){detectTapGestures{}}) {
                VacancySearchRow(query,onQuery,onFilters,Modifier.padding(10.dp),{floatingFocused=it})
            }
        }
    }
    }
}

@Composable
private fun VacancySearchRow(query:String,onQuery:(String)->Unit,onFilters:()->Unit,modifier:Modifier=Modifier,onFocus:(Boolean)->Unit={}) {
    val colors=MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
        OutlinedTextField(query,onQuery,placeholder={Text("Должность или компания",maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},singleLine=true,
            leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton(onClick={onQuery("")}){Icon(Icons.Default.Close,"Очистить поиск")}},
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),shape=RoundedCornerShape(18.dp),
            colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=colors.surface,focusedContainerColor=colors.surface),modifier=Modifier.weight(1f).onFocusChanged{onFocus(it.isFocused)})
        FilledTonalIconButton(onClick=onFilters,shape=RoundedCornerShape(18.dp),modifier=Modifier.size(56.dp)){Icon(Icons.Default.Tune,"Фильтры")}
        com.example.trudomix.ui.components.ProfileAction(toolbarStyle=true)
    }
}

@Composable
private fun FeedNotice(title: String, message: String, action: String, onAction: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
