package com.example.trudomix

import com.example.trudomix.data.models.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RecommendationAndChatModelsTest {
    private val json=Json{ignoreUnknownKeys=true}
    @Test fun oldConversationStillWorksAndNewCompanyLogoIsIndependentOfCandidateAvatar(){
        val old=json.decodeFromString<Conversation>("""{"conversation_id":4,"company_name":"Team","candidate_id":7}""")
        assertEquals("",old.company_logo);assertEquals("",old.candidate_photo)
        val current=json.decodeFromString<Conversation>("""{"conversation_id":4,"company_logo":"/uploads/company.png","candidate_photo":"/uploads/candidate.png"}""")
        assertEquals("/uploads/company.png",current.company_logo);assertEquals("/uploads/candidate.png",current.candidate_photo)
    }
    @Test fun insightsAndDismissalCountSurviveDecodingAndOldResponsesHaveSafeDefaults(){
        val old=json.decodeFromString<RecommendationsResponse>("""{"recommendations":[{"vacancy":{"id":7},"score":0.7,"reasoning":"Matched"}]}""")
        assertTrue(old.recommendations.first().insights.reasons.isEmpty());assertEquals(0,old.hidden_count)
        val current=json.decodeFromString<RecommendationsResponse>("""{"hidden_count":2,"recommendations":[{"vacancy":{"id":7},"insights":{"matched_skills":["Go"],"missing_skills":["Kotlin"],"reasons":["Skills"],"considerations":["City"]}}]}""")
        assertEquals(2,current.hidden_count);assertEquals(listOf("Go"),current.recommendations.first().insights.matched_skills)
        assertEquals(listOf("City"),current.recommendations.first().insights.considerations)
    }
}
