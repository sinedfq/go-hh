package com.example.trudomix

import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.repository.bodyOrThrow
import com.example.trudomix.data.repository.checkSuccess
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test

class RecommendationCacheTest {
    @Test fun dismissedRecommendationDoesNotReappearFromOfflineSnapshot()=runBlocking{
        val server=MockWebServer()
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse=when(request.requestUrl?.encodedPath){
            "/api/recommendations"->MockResponse().setHeader("Content-Type","application/json").setBody("""{"recommendations":[{"vacancy":{"id":7}},{"vacancy":{"id":8}}],"hidden_count":0}""")
            "/api/recommendations/7/hidden"->MockResponse().setHeader("Content-Type","application/json").setBody("{}")
            else->MockResponse().setResponseCode(404)
        }}
        server.start()
        val oldServer=ApiClient.baseUrl;val manager=ApiClient.getTokenManager();val oldToken=manager.getToken()
        try{
            ApiClient.setServer(server.url("/").toString());manager.saveToken("recommendation-cache-test")
            assertEquals(2,ApiClient.api.recommendations().bodyOrThrow().recommendations.size)
            ApiClient.api.dismissRecommendation(7).checkSuccess()
            ApiClient.updateRecommendationCache(7,"recommendation-cache-test")
            server.shutdown()
            val offline=ApiClient.api.recommendations().bodyOrThrow()
            assertEquals(listOf(8),offline.recommendations.map{it.vacancy.id})
            assertEquals(1,offline.hidden_count)
            assertEquals("recommendation-cache-test",manager.getToken())
        }finally{
            runCatching{server.shutdown()};ApiClient.setServer(oldServer)
            if(oldToken!=null)manager.saveToken(oldToken)else manager.clearSession()
            com.example.trudomix.startup.StartupReadiness.usingCachedData.value=false
        }
    }
}
