package com.example.trudomix

import com.example.trudomix.data.models.*
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test
import org.junit.Assert.*

class SavedSearchFiltersTest {
    @Test fun websiteSearchRestoresOfficeSalaryAndEscapedText(){
        val data=Json{ignoreUnknownKeys=true;coerceInputValues=true}.decodeFromString<SavedSearch>("""{"id":3,"name":"Go & Kotlin","filters":{"query":"Go & Kotlin","remote":false,"skills":["C++","Go"],"salary_min":120000,"salary_max":220000,"currency":"RUB","employment":"full_time","schedule":"flexible","location":"Новосибирск"}}""")
        val url=("https://test.local/?"+data.filters.toQuery()).toHttpUrl()
        assertEquals("Go & Kotlin",url.queryParameter("q"));assertEquals("false",url.queryParameter("remote"))
        assertEquals("C++,Go",url.queryParameter("skills"));assertEquals("Новосибирск",url.queryParameter("location"))
        assertEquals("120000",url.queryParameter("salary_min"));assertEquals("220000",url.queryParameter("salary_max"));assertEquals("flexible",url.queryParameter("schedule"))
        assertTrue(data.filters.summary().contains("В офисе"))
    }
    @Test fun emptySearchDoesNotBecomeRemoteOnly(){val url=("https://test.local/?"+SavedSearchFilters().toQuery()).toHttpUrl();assertNull(url.queryParameter("remote"));assertNull(url.queryParameter("salary_min"))}
}
