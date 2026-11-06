package com.example.trudomix.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.SessionIdentity
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.bodyOrThrow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Fetch read models before mounting lazy screens; no per-row network discovery. */
class InitialDataViewModel : ViewModel() {
    val loading = MutableStateFlow(true)
    var catalog: VacancySearchResponse? = null; private set
    private val ticket = Any()
    init {
        StartupReadiness.begin(ticket)
        StartupReadiness.preloading.value=true
        viewModelScope.launch {
            try { preload() }
            finally { StartupReadiness.preloading.value=false;loading.value = false; StartupReadiness.finish(ticket) }
        }
    }
    private suspend fun preload() = supervisorScope {
        val api = ApiClient.api
        val images = linkedSetOf<String>()
        fun image(path: String?) { if (!path.isNullOrBlank()) images.add(path) }
        fun jobs(items: List<Vacancy>) { items.forEach { image(it.company_logo) } }
        suspend fun <T> optional(block: suspend () -> T): T? = try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        val user = optional { api.getMe().bodyOrThrow() } ?: SessionIdentity.user.value
        if (user == null) return@supervisorScope
        SessionIdentity.user.value = user
        image(user.photo_url)
        val candidate = user.role == "candidate"
        val tasks = mutableListOf<Deferred<*>>()
        val network = Semaphore(4)
        fun task(block: suspend () -> Unit) { tasks += async { network.withPermit { optional(block) } } }
        task {
            val feed = api.socialFeed().bodyOrThrow()
            feed.posts.forEach { image(it.author_photo); if (it.media_type != "video") image(it.media_url) }
        }
        task { api.timedStories().bodyOrThrow().forEach { image(it.company_logo); if (it.media_type != "video") image(it.media_url) } }
        task { api.getConversations().bodyOrThrow() }
        task { api.getSavedSearches().bodyOrThrow() }
        task { api.getSkills().bodyOrThrow(); api.getPositions().bodyOrThrow() }
        task { jobs(api.searchVacancies(limit = 4).bodyOrThrow().vacancies) }
        if (candidate) {
            task {
                val items = mutableListOf<Vacancy>()
                var page = api.searchVacancies().bodyOrThrow()
                items.addAll(page.vacancies)
                // Bounded startup work; very large catalogs retain ordinary pagination.
                while (items.size < page.total_count && items.size < 1000 && page.vacancies.isNotEmpty()) {
                    page = api.searchVacancies(offset = items.size).bodyOrThrow()
                    items.addAll(page.vacancies)
                }
                catalog = page.copy(vacancies = items.distinctBy { it.id }, offset = 0, limit = items.size)
                jobs(items)
            }
            task { jobs(api.getFavorites().bodyOrThrow()) }
            task { api.getMyApplications().bodyOrThrow() }
            task { val response = api.getMyResume(); if (response.isSuccessful) image(response.body()?.photo_url); api.getResumeStats().bodyOrThrow() }
        } else {
            task {
                val mine = api.getMyVacancies().bodyOrThrow()
                image(mine.company.logo_url); mine.company.photos.forEach(::image); jobs(mine.vacancies)
            }
            task { api.employerApplications().bodyOrThrow(); api.getEmployerStats().bodyOrThrow() }
        }
        tasks.awaitAll()
        val context = ApiClient.applicationContext
        val decoding = Semaphore(3)
        // Disk cache is shared with AsyncImage, including images outside the viewport.
        images.take(256).map { path -> async(Dispatchers.IO) {
            decoding.withPermit { optional {
                context.imageLoader.execute(ImageRequest.Builder(context).data(ApiClient.mediaUrl(path)).size(640)
                    .networkCachePolicy(if(StartupReadiness.usingCachedData.value)coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED).build())
            } }
        } }.awaitAll()
    }
}
