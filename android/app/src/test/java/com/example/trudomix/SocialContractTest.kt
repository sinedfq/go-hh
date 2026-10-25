package com.example.trudomix

import com.example.trudomix.data.models.*
import com.example.trudomix.ui.screens.social.activeStories
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SocialContractTest {
    private val json=Json{ignoreUnknownKeys=true}
    @Test fun expiredAndMalformedStoriesDoNotRemainOnHome(){
        val stories=listOf(TimedCompanyStory(id=1,expires_at="2026-10-25T12:00:00Z"),TimedCompanyStory(id=2,expires_at="2026-10-25T12:00:01Z"),TimedCompanyStory(id=3,expires_at="invalid"))
        assertEquals(listOf(2L),activeStories(stories,java.time.Instant.parse("2026-10-25T12:00:00Z").toEpochMilli()).map{it.id})
    }
    @Test fun personalAndCompanyPostsKeepSeparateIdentityAndPermissions(){
        val page=json.decodeFromString<SocialFeedPage>("""{"posts":[{"id":1,"company_id":null,"can_delete":false,"liked":true,"likes":2},{"id":2,"company_id":99,"author_name":"Команда","can_delete":true}],"next_cursor":1}""")
        assertNull(page.posts[0].company_id);assertTrue(page.posts[0].liked);assertFalse(page.posts[0].can_delete)
        assertEquals(99,page.posts[1].company_id);assertEquals(1L,page.next_cursor)
    }
}
