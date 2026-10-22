package com.example.trudomix

import com.example.trudomix.data.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ApiContractTest {
    @Test fun companyLogosMatchVacancyAndCompanyApiFields() {
        val vacancy=Json.decodeFromString<Vacancy>("""{"id":1,"company_logo":"/uploads/company.png"}""")
        val company=Json.decodeFromString<Company>("""{"id":1,"logo_url":"/uploads/company.png"}""")
        assertEquals(company.logo_url,vacancy.company_logo)
    }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    @Test fun searchUsesEnvelopeAndCoordinates() {
        val page = json.decodeFromString<VacancySearchResponse>("""{"vacancies":[{"id":8,"latitude":55.02,"longitude":82.93}],"total_count":41,"limit":20,"offset":20}""")
        assertEquals(41, page.total_count)
        assertEquals(20, page.offset)
        assertEquals(55.02, page.vacancies.single().latitude!!, 0.0001)
    }
    @Test fun conversationsUseServerIdentifier() {
        val conversation = json.decodeFromString<Conversation>("""{"conversation_id":17,"candidate_id":3,"company_name":"Компания","vacancy_id":42,"cover_letter":"Хочу работать у вас","last_message_at":null}""")
        assertEquals(17, conversation.id)
        assertEquals("Компания", conversation.company_name)
        assertEquals(42, conversation.vacancy_id)
        assertEquals("Хочу работать у вас", conversation.cover_letter)
    }
    @Test fun resumeWritesServerFieldNames() {
        val resume = Resume(experience="middle", about_me="О себе", github_url="https://github.com/example", photo_url="/uploads/resume.jpg")
        val fields = json.parseToJsonElement(json.encodeToString(resume)).jsonObject
        assertTrue(fields.containsKey("experience"))
        assertTrue(fields.containsKey("about"))
        assertTrue(fields.containsKey("github"))
        assertFalse(fields.containsKey("about_me"))
        assertFalse(fields.containsKey("experience_years"))
        assertEquals(resume, json.decodeFromString<Resume>(json.encodeToString(resume)))
    }
    @Test fun serverAddressRequiresOriginAndNormalizesSlash() {
        assertEquals("http://192.168.0.4:8080/", com.example.trudomix.data.api.normalizeServerUrl(" http://192.168.0.4:8080 "))
        for (bad in listOf("ftp://server/", "https://server/api/", "https://user:pass@server/", "https://server/?token=secret", "https://server/#fragment")) {
            assertTrue("Must reject $bad", runCatching { com.example.trudomix.data.api.normalizeServerUrl(bad) }.isFailure)
        }
    }
    @Test fun applicationStatusContainsApplication() {
        val status = json.decodeFromString<ApplicationStatusResponse>("""{"applied":true,"application":{"id":12,"status":"viewed"}}""")
        assertTrue(status.applied)
        assertEquals(12, status.application?.id)
        assertEquals("viewed", status.application?.status)
    }
    @Test fun favoriteAndMessageBodiesMatchGo() {
        assertEquals("""{"vacancy_id":42}""", json.encodeToString(FavoriteRequest(42)))
        assertEquals("""{"content":"Здравствуйте"}""", json.encodeToString(MessageRequest("Здравствуйте")))
    }
}
