package com.example.trudomix.data.repository
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.*
class EmployerRepository{
    private val api=ApiClient.api
    suspend fun vacancies()=api.getMyVacancies().bodyOrThrow()
    suspend fun applications()=api.employerApplications().bodyOrThrow()
    suspend fun createCompany(request:CompanyRequest)=api.createCompany(request).bodyOrThrow()
    suspend fun saveVacancy(id:Int?,request:VacancyRequest){if(id==null)api.createVacancy(request).bodyOrThrow()else api.updateVacancy(id,request).bodyOrThrow()}
    suspend fun archive(id:Int,archive:Boolean)=api.archiveVacancy(id,ArchiveRequest(archive)).checkSuccess()
    suspend fun delete(id:Int)=api.deleteVacancy(id).checkSuccess()
    suspend fun status(id:Int,status:String)=api.updateApplicationStatus(id,StatusRequest(status)).checkSuccess()
    suspend fun startChat(id:Int)=api.startChat(id).bodyOrThrow().conversation_id
    suspend fun geocode(address:String)=api.geocode(address).bodyOrThrow()
}
