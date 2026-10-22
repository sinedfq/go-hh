package com.example.trudomix

import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.matchingSuggestions
import org.junit.Assert.*
import org.junit.Test

class ResumeCompletionTest {
    @Test fun sharedContactSectionDoesNotDoubleCount() {
        val empty=Resume(full_name="",desired_position="")
        assertEquals(0,resumeCompletionCount(empty))
        assertEquals(1,resumeCompletionCount(empty.copy(phone="123",telegram="@name")))
        assertEquals("12,5%",resumeCompletionLabel(1))
        val complete=empty.copy(full_name="Name",desired_position="Kotlin",city="City",experience="Junior",skills=listOf("Go"),about_me="About",phone="123",photo_url="/photo")
        assertEquals(8,resumeCompletionCount(complete))
        assertEquals("100%",resumeCompletionLabel(resumeCompletionCount(complete)))
        assertEquals(8,resumeCompletionCount(complete.copy(github_url="url",linkedin_url="url",remote=true)))
    }
    @Test fun citySuggestionsIgnoreCaseAndRemoveDuplicates() {
        assertEquals(listOf("Новосибирск"),matchingSuggestions("НОВО",listOf("Москва","Новосибирск","новосибирск")))
        assertTrue(matchingSuggestions("missing",listOf("Москва")).isEmpty())
    }
}
