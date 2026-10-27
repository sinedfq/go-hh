package com.example.trudomix.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    val id: Int = 0,
    val email: String = "",
    val photo_url: String = "",
    val role: String = "candidate",
    val company_id: Int? = null,
    val created_at: String = ""
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val role: String,val phone:String="",val phone_receipt:String="",val device_key:String="")

@Serializable
data class Vacancy(
    val created_at: String = "",
    val id: Int = 0,
    val title: String = "",
    val company: String = "",
    val company_logo: String? = null,
    val location: String = "",
    val experience: String = "",
    val salary: String? = null,
    val salary_from: Int = 0,
    val salary_to: Int = 0,
    val currency: String = "RUB",
    val salary_gross: Boolean = false,
    val employment: String = "",
    val schedule: String = "",
    val company_it_accredited: Boolean = false,
    val remote: Boolean = false,
    val skills: List<String> = emptyList(),
    val description: String = "",
    val views: Int = 0,
    val is_archived: Boolean = false,
    val is_favorite: Boolean = false,
    val company_id: Int = 0,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class ApplyRequest(
    val cover_letter: String = ""
)

@Serializable
data class ApplyResponse(
    val message: String = "",
    val id: Int = 0,
    val vacancy_id: Int = 0,
    val status: String = ""
)

@Serializable
data class ApplicationStatusResponse(
    val applied: Boolean = false,
    val application: JobApplication? = null
)

@Serializable
data class JobApplication(
    val id: Int? = null,
    val vacancy_id: Int? = null,
    val candidate_user_id: Int? = null, // Добавь это поле
    val resume_id: Int? = null,         // И это
    val cover_letter: String? = null,
    val status: String? = null,
    val created_at: String? = null,
    val viewed_at: String? = null,      // И это
    val feedback: String = "",
    val interview_at: String? = null,
    val vacancy_title: String? = null,
    val company_name: String? = null,
    val conversation_id: Int? = null,
    val resume_full_name: String = "",
    val candidate_email: String = "",
    val resume_position: String = "",
    val vacancy_archived: Boolean = false
)

@Serializable
data class Conversation(
    @SerialName("conversation_id")
    val id: Int = 0,
    val application_id: Int = 0,
    val company_name: String = "",
    val candidate_id: Int = 0,
    val vacancy_archived: Boolean = false,
    val vacancy_id: Int? = null, // Было Int, стало Int?
    val vacancy_title: String? = null,
    val participant_name: String? = null,
    val candidate_name: String? = null,
    val cover_letter: String = "",
    val employer_name: String? = null,
    val last_message: String? = null,
    val last_message_time: String? = null,
    val last_message_at: String? = null,
    val unread_count: Int = 0,
    val application_status: String? = null
)

@Serializable
data class Resume(
    val total_experience_months: Int = 0,
    val total_experience: String = "",
    val work_experience: List<WorkExperience> = emptyList(),
    val id: Int = 0,
    val views: Int = 0,
    val photo_url: String = "",
    val user_id: Int = 0,
    val full_name: String = "",
    val desired_position: String = "",
    val experience: String = "",
    val skills: List<String> = emptyList(),
    @SerialName("about") val about_me: String = "",
    val city: String = "",
    val remote: Boolean = false,
    @SerialName("github") val github_url: String = "",
    @SerialName("linkedin") val linkedin_url: String = "",
    val phone: String = "",
    val telegram: String = ""
)

@Serializable data class WorkExperience(val id:Int=0,val company:String="",val position:String="",val start_date:String="",val end_date:String?=null,val description:String="")
@Serializable data class WorkExperienceRequest(val company:String,val position:String,val start_date:String,val end_date:String?=null,val description:String="")
@Serializable data class SavedSearch(val id:Int=0,val name:String="",val filters:SavedSearchFilters=SavedSearchFilters())
@Serializable data class SavedSearchRequest(val name:String,val query:String)
@Serializable data class UploadImageResponse(val url:String="")

@Serializable
data class VacancySearchResponse(val vacancies: List<Vacancy> = emptyList(), val total_count: Int = 0, val limit: Int = 20, val offset: Int = 0)
@Serializable data class FavoriteRequest(val vacancy_id: Int)
@Serializable data class Message(val id: Int, val conversation_id: Int, val sender_id: Int, val content: String, val created_at: String, val is_read: Boolean = false, val read_at: String? = null, val sender_name: String = "", val sender_photo: String = "", val sender_role: String = "")
@Serializable data class MessageRequest(val content: String)
@Serializable data class TypingResponse(val typing_users: List<Int> = emptyList())
@Serializable data class CountResponse(val count: Int = 0)
@Serializable data class StartChatResponse(val conversation_id: Int)
@Serializable data class DeviceTokenRequest(val token: String)
@Serializable data class PushStatus(val server_enabled: Boolean = false, val registered: Boolean = false)
@Serializable data class ResumeStats(val total_views: Int = 0)
@Serializable data class EmployerStats(val total_vacancies: Int = 0, val total_vacancy_views: Int = 0, val total_applications: Int = 0, val new_applications: Int = 0)
@Serializable data class Company(val id: Int = 0, val name: String = "", val description: String = "", val city: String = "", val website: String = "", val industry: String = "", val logo_url: String = "", val it_accredited: Boolean = false, val size:String="",val team_story:String="",val benefits:List<String> = emptyList(),val hiring_process:String="",val photos:List<String> = emptyList(),val videos:List<String> = emptyList())
@Serializable data class MyVacanciesResponse(val company: Company = Company(), val vacancies: List<Vacancy> = emptyList())
@Serializable data class CompanyRequest(val name: String, val description: String = "", val city: String = "", val website: String = "", val industry: String = "", val size: String = "")
@Serializable data class VacancyRequest(val title: String, val location: String, val experience: String, val remote: Boolean, val skills: List<String>, val description: String, val address: String, val latitude: Double? = null, val longitude: Double? = null)
@Serializable data class StatusRequest(val status: String)
@Serializable data class ArchiveRequest(val archive: Boolean)
@Serializable data class GeocodeResult(val address: String, val latitude: Double, val longitude: Double)
