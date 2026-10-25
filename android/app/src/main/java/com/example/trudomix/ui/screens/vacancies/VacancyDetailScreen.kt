package com.example.trudomix.ui.screens.vacancies
import com.example.trudomix.ui.components.CompanyAvatar
import com.example.trudomix.ui.components.VacancyTerms
import com.example.trudomix.ui.components.ITAccreditationBadge

import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.components.AppSnackbarHost

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VacancyDetailScreen(
    vacancyId: Int,
    viewModel: VacancyDetailViewModel,
    onNavigateBack: () -> Unit,
    onCompanyClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasResume by viewModel.hasResume.collectAsState()
    val hasApplied by viewModel.hasApplied.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showApplySheet by remember { mutableStateOf(false) }
    var coverLetter by remember { mutableStateOf("") }
    val isFavorite = (uiState as? VacancyDetailUiState.Success)?.vacancy?.is_favorite == true

    LaunchedEffect(vacancyId) {
        viewModel.loadVacancyDetail(vacancyId)
    }


    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            AppScreenHeader(
                title = { Text("Детали вакансии") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(vacancyId, !isFavorite)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is VacancyDetailUiState.Loading -> {
                    com.example.trudomix.ui.loading.TrudomixLoadingScreen(label="Загружаем вакансию")
                }

                is VacancyDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadVacancyDetail(vacancyId) }) {
                            Text("Повторить")
                        }
                    }
                }

                is VacancyDetailUiState.Success -> {
                    val vacancy = state.vacancy
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CompanyAvatar(vacancy.company_logo, vacancy.company, 56.dp)


                            Spacer(modifier = Modifier.width(16.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = vacancy.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Text(
                                        text = vacancy.company,
                                        modifier=Modifier.weight(1f,fill=false),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if(vacancy.company_it_accredited) ITAccreditationBadge()
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        VacancyTerms(vacancy)
                        if (vacancy.company_id > 0) TextButton(onClick = { onCompanyClick(vacancy.company_id) }) { Text("О компании") }

                        if (vacancy.skills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Требуемые навыки:", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(vacancy.skills) { skill ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(skill) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Описание вакансии",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.trudomix.ui.components.MarkdownContent(vacancy.description.ifEmpty { "Описание не указано" })

                        Spacer(modifier = Modifier.height(32.dp))

                        if (hasResume == false && !hasApplied) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Для отклика на вакансию необходимо создать резюме в профиле.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showApplySheet = true },
                            enabled = hasResume == true && !hasApplied && !vacancy.is_archived,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (hasApplied) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Вы уже откликнулись")
                            } else if (vacancy.is_archived) {
                                Text("Вакансия в архиве")
                            } else {
                                Text("Откликнуться на вакансию")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApplySheet) {
        ModalBottomSheet(
            onDismissRequest = { showApplySheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Сопроводительное письмо",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Расскажите работодателю, почему вы подходите на эту должность:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = coverLetter,
                    onValueChange = { coverLetter = it },
                    placeholder = { Text("Здравствуйте! Меня заинтересовала ваша вакансия...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showApplySheet = false
                        viewModel.applyToVacancy(vacancyId, coverLetter)
                        coverLetter = ""
                    },
                    enabled = !isApplying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Отправить отклик")
                    }
                }
            }
        }
    }
}
