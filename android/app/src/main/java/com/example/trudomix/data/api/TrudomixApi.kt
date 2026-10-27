package com.example.trudomix.data.api

import com.example.trudomix.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface TrudomixApi {
    @GET("api/phone-verification/config") suspend fun phoneOtpConfig():Response<PhoneOtpConfig>
    @POST("api/phone-verification/request") suspend fun requestPhoneOtp(@Body request:PhoneOtpRequest):Response<PhoneOtpChallenge>
    @POST("api/phone-verification/verify") suspend fun verifyPhoneOtp(@Body request:PhoneOtpVerify):Response<PhoneOtpReceipt>
    @POST("api/auth/phone-login") suspend fun phoneLogin(@Body request:PhoneLoginRequest):Response<AuthResponse>
    @POST("api/auth/mobile/register") suspend fun registerPhone(@Body request:RegisterRequest):Response<AuthResponse>
    @GET("api/companies/{id}/subscription") suspend fun companySubscription(@Path("id") id:Int):Response<CompanySubscription>
    @PUT("api/companies/{id}/subscription") suspend fun followCompany(@Path("id") id:Int):Response<CompanySubscription>
    @DELETE("api/companies/{id}/subscription") suspend fun unfollowCompany(@Path("id") id:Int):Response<CompanySubscription>
    @GET("api/feed") suspend fun socialFeed(@Query("scope") scope:String="all",@Query("before") before:Long?=null,@Query("company_id") companyId:Int?=null):Response<SocialFeedPage>
    @Multipart @POST("api/posts") suspend fun publishPost(@Part("body") body:okhttp3.RequestBody,@Part("as_company") asCompany:okhttp3.RequestBody,@Part media:okhttp3.MultipartBody.Part?=null):Response<SocialCreated>
    @Multipart @POST("api/stories") suspend fun publishStory(@Part("body") body:okhttp3.RequestBody,@Part media:okhttp3.MultipartBody.Part):Response<SocialCreated>
    @GET("api/stories") suspend fun timedStories(@Query("company_id") companyId:Int?=null):Response<List<TimedCompanyStory>>
    @DELETE("api/stories/{id}") suspend fun deleteStory(@Path("id") id:Long):Response<Unit>
    @DELETE("api/posts/{id}") suspend fun deletePost(@Path("id") id:Long):Response<Unit>
    @PUT("api/posts/{id}/like") suspend fun likePost(@Path("id") id:Long):Response<SocialLike>
    @DELETE("api/posts/{id}/like") suspend fun unlikePost(@Path("id") id:Long):Response<SocialLike>
    @GET("api/posts/{id}/comments") suspend fun postComments(@Path("id") id:Long,@Query("before") before:Long?=null):Response<SocialCommentsPage>
    @POST("api/posts/{id}/comments") suspend fun addPostComment(@Path("id") id:Long,@Body body:SocialBody):Response<SocialCreated>
    @DELETE("api/comments/{id}") suspend fun deletePostComment(@Path("id") id:Long):Response<Unit>
    @GET("api/saved-searches") suspend fun getSavedSearches():Response<List<SavedSearch>>
    @POST("api/saved-searches") suspend fun saveSearch(@Body request:SavedSearchRequest):Response<SavedSearch>
    @DELETE("api/saved-searches/{id}") suspend fun deleteSavedSearch(@Path("id") id:Int):Response<Unit>
    @POST("api/work-experience") suspend fun addWorkExperience(@Body request:WorkExperienceRequest):Response<Resume>
    @DELETE("api/work-experience/{id}") suspend fun deleteWorkExperience(@Path("id") id:Int):Response<Resume>
    @Multipart @POST("api/vacancies/images") suspend fun uploadVacancyImage(@Part photo:okhttp3.MultipartBody.Part):Response<UploadImageResponse>
    @Multipart @POST("api/my-company/photos") suspend fun uploadCompanyMedia(@Part media:okhttp3.MultipartBody.Part):Response<Company>
    @GET("api/skills") suspend fun getSkills(): Response<List<DirectoryEntry>>
    @GET("api/positions") suspend fun getPositions(): Response<List<DirectoryEntry>>
    @POST("api/skills") suspend fun createSkill(@Body entry: DirectoryEntry): Response<DirectoryEntry>
    @POST("api/positions") suspend fun createPosition(@Body entry: DirectoryEntry): Response<DirectoryEntry>
    @GET("api/my-resume/stats") suspend fun getResumeStats(): Response<ResumeStats>
    @GET("api/employer/stats") suspend fun getEmployerStats(): Response<EmployerStats>
    @POST("api/devices/status") suspend fun devicePushStatus(@Body request: DeviceTokenRequest): Response<PushStatus>
    @POST("api/devices/test") suspend fun testDevicePush(@Body request: DeviceTokenRequest): Response<Unit>
    @Multipart @POST("api/users/me/photo")
    suspend fun uploadUserPhoto(@Part photo: okhttp3.MultipartBody.Part): Response<User>

    @Multipart @POST("api/resumes/me/photo")
    suspend fun uploadResumePhoto(@Part photo: okhttp3.MultipartBody.Part): Response<Resume>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/me")
    suspend fun getMe(): Response<User>

    @GET("api/vacancies")
    suspend fun getVacancies(): Response<List<Vacancy>>

    @GET("api/vacancies/search")
    suspend fun searchVacancies(@Query("q") query: String = "", @Query("location") location: String? = null,
        @Query("experience") experience: String? = null, @Query("remote") remote: Boolean? = null,
        @Query("skills") skills: String? = null, @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0, @Query("company") company: String? = null,
        @Query("has_coordinates") hasCoordinates: Boolean = false,
        @Query("sort") sort: String = "newest",@Query("salary_min") salaryMin:Int?=null,@Query("salary_max") salaryMax:Int?=null,
        @Query("currency") currency:String?=null,@Query("salary_only") salaryOnly:Boolean=false,@Query("employment") employment:String?=null,@Query("schedule") schedule:String?=null): Response<VacancySearchResponse>

    @GET("api/vacancies/{id}")
    suspend fun getVacancyDetail(@Path("id") id: Int): Response<Vacancy>

    @POST("api/vacancies/{id}/apply")
    suspend fun applyToVacancy(
        @Path("id") vacancyId: Int,
        @Body request: ApplyRequest
    ): Response<ApplyResponse>

    @GET("api/vacancies/{id}/application")
    suspend fun getVacancyApplicationStatus(@Path("id") vacancyId: Int): Response<ApplicationStatusResponse>

    @GET("api/my-applications")
    suspend fun getMyApplications(): Response<List<JobApplication>>
    @GET("api/applications/{id}/history")
    suspend fun applicationHistory(@Path("id") id: Int): Response<List<ApplicationEvent>>

    @DELETE("api/applications/{id}")
    suspend fun cancelApplication(@Path("id") id: Int): Response<Unit>

    @GET("api/conversations")
    suspend fun getConversations(): Response<List<Conversation>>

    @GET("api/my-resume")
    suspend fun getMyResume(): Response<Resume>

    @Streaming
    @GET("api/my-resume/pdf")
    suspend fun getMyResumePdf(): Response<okhttp3.ResponseBody>

    @POST("api/resumes")
    suspend fun createResume(@Body resume: Resume): Response<Resume>

    @PUT("api/my-resume")
    suspend fun updateMyResume(@Body resume: Resume): Response<Resume>

    @POST("api/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): Response<Unit>

    @DELETE("api/favorites/{vacancyId}")
    suspend fun removeFavorite(@Path("vacancyId") vacancyId: Int): Response<Unit>

    @GET("api/favorites") suspend fun getFavorites(): Response<List<Vacancy>>
    @GET("api/conversations/{id}/messages") suspend fun getMessages(@Path("id") id: Int): Response<List<Message>>
    @POST("api/conversations/{id}/messages") suspend fun sendMessage(@Path("id") id: Int, @Body request: MessageRequest): Response<Message>
    @GET("api/conversations/{id}/typing") suspend fun getTyping(@Path("id") id: Int): Response<TypingResponse>
    @POST("api/conversations/{id}/typing") suspend fun markTyping(@Path("id") id: Int): Response<Unit>
    @GET("api/conversations/unread-count") suspend fun getUnreadCount(): Response<CountResponse>
    @POST("api/applications/{id}/start-chat") suspend fun startChat(@Path("id") id: Int): Response<StartChatResponse>
    @POST("api/devices") suspend fun registerDevice(@Body request: DeviceTokenRequest): Response<Unit>
    @HTTP(method = "DELETE", path = "api/devices", hasBody = true) suspend fun unregisterDevice(@Body request: DeviceTokenRequest): Response<Unit>
    @GET("api/companies/{id}") suspend fun getCompany(@Path("id") id: Int): Response<Company>
    @GET("api/companies/{id}/vacancies") suspend fun getCompanyVacancies(@Path("id") id: Int): Response<List<Vacancy>>
    @GET("api/my-vacancies") suspend fun getMyVacancies(): Response<MyVacanciesResponse>
    @POST("api/companies") suspend fun createCompany(@Body request: CompanyRequest): Response<Company>
    @POST("api/vacancies") suspend fun createVacancy(@Body request: VacancyRequest): Response<Vacancy>
    @PUT("api/vacancies/{id}") suspend fun updateVacancy(@Path("id") id: Int, @Body request: VacancyRequest): Response<Vacancy>
    @PATCH("api/vacancies/{id}/archive") suspend fun archiveVacancy(@Path("id") id: Int, @Body request: ArchiveRequest): Response<Unit>
    @DELETE("api/vacancies/{id}") suspend fun deleteVacancy(@Path("id") id: Int): Response<Unit>
    @GET("api/employer/applications") suspend fun employerApplications(): Response<List<JobApplication>>
    @PATCH("api/applications/{id}/status") suspend fun updateApplicationStatus(@Path("id") id: Int, @Body request: StatusRequest): Response<Unit>
    @GET("api/geocode") suspend fun geocode(@Query("address") address: String): Response<GeocodeResult>
}
