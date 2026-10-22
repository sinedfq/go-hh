package com.example.trudomix.data.models

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

@Serializable
data class SavedSearchFilters(
 val query:String="",val location:String="",val company:String="",val experience:String="",val remote:Boolean?=null,
 val skills:List<String> = emptyList(),val has_coordinates:Boolean=false,val sort:String="newest",
 val salary_min:Int=0,val salary_max:Int=0,val currency:String="",val salary_only:Boolean=false,val employment:String="",val schedule:String=""
) {
 fun toQuery():String {
  val url="https://trudomix.local/".toHttpUrl().newBuilder()
  listOf("q" to query,"location" to location,"company" to company,"experience" to experience,"skills" to skills.joinToString(","),"sort" to sort,"currency" to currency,"employment" to employment,"schedule" to schedule).forEach{(name,value)->if(value.isNotBlank())url.addQueryParameter(name,value)}
  remote?.let {url.addQueryParameter("remote",it.toString())}
  if(has_coordinates)url.addQueryParameter("has_coordinates","true")
  if(salary_only)url.addQueryParameter("salary_only","true")
  if(salary_min>0)url.addQueryParameter("salary_min",salary_min.toString())
  if(salary_max>0)url.addQueryParameter("salary_max",salary_max.toString())
  return url.build().encodedQuery.orEmpty()
 }
 fun summary():String=listOfNotNull(query.takeIf{it.isNotBlank()},location.takeIf{it.isNotBlank()},company.takeIf{it.isNotBlank()},remote?.let{if(it)"Удалённо" else "В офисе"},experience.takeIf{it.isNotBlank()}?.replace('|','·'),skills.takeIf{it.isNotEmpty()}?.joinToString(", "),if(salary_min>0)"от $salary_min ${currency.ifBlank{"RUB"}}" else null,if(salary_max>0)"до $salary_max ${currency.ifBlank{"RUB"}}" else null,employment.takeIf{it.isNotBlank()}?.let{when(it){"full_time"->"Полная занятость";"part_time"->"Частичная занятость";"project"->"Проектная работа";else->"Стажировка"}}).joinToString(" · ").ifBlank{"Все вакансии"}
}
