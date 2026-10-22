package com.example.trudomix.data.models

/** Eight equally weighted sections. A shared calculation for editor and profile. */
fun resumeCompletionCount(resume: Resume): Int = listOf(resume.full_name.isNotBlank(),resume.desired_position.isNotBlank(),
    resume.city.isNotBlank(),resume.experience.isNotBlank(),resume.skills.any{it.isNotBlank()},resume.about_me.isNotBlank(),
    resume.phone.isNotBlank()||resume.telegram.isNotBlank(),resume.photo_url.isNotBlank()).count{it}

fun resumeCompletionLabel(count: Int): String = if(count % 2 == 0) "${count*100/8}%" else "${count*100/8},5%"
