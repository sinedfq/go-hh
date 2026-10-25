package com.example.trudomix.data.models

import kotlinx.serialization.Serializable

@Serializable data class CompanySubscription(val following:Boolean=false,val followers:Int=0)
@Serializable data class SocialPost(val id:Long=0,val user_id:Int=0,val company_id:Int?=null,val author_name:String="",val author_photo:String="",val body:String="",val media_url:String="",val media_type:String="",val created_at:String="",val likes:Int=0,val comments:Int=0,val liked:Boolean=false,val can_delete:Boolean=false)
@Serializable data class SocialFeedPage(val posts:List<SocialPost> = emptyList(),val next_cursor:Long=0)
@Serializable data class SocialComment(val id:Long=0,val user_id:Int=0,val author_name:String="",val author_photo:String="",val body:String="",val created_at:String="",val can_delete:Boolean=false)
@Serializable data class SocialCommentsPage(val comments:List<SocialComment> = emptyList(),val next_cursor:Long=0)
@Serializable data class SocialBody(val body:String)
@Serializable data class SocialCreated(val id:Long=0)
@Serializable data class SocialLike(val likes:Int=0,val liked:Boolean=false)
@Serializable data class TimedCompanyStory(val id:Long=0,val company_id:Int=0,val company_name:String="",val company_logo:String="",val caption:String="",val media_url:String="",val media_type:String="image",val created_at:String="",val expires_at:String="",val can_delete:Boolean=false)
