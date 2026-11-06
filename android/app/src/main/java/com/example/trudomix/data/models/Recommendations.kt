package com.example.trudomix.data.models

import kotlinx.serialization.Serializable

@Serializable data class RecommendationInsights(
    val matched_skills:List<String> = emptyList(), val missing_skills:List<String> = emptyList(),
    val reasons:List<String> = emptyList(), val considerations:List<String> = emptyList()
)
@Serializable data class Recommendation(val vacancy:Vacancy=Vacancy(), val score:Double=0.0, val reasoning:String="", val insights:RecommendationInsights=RecommendationInsights())
@Serializable data class RecommendationsResponse(val recommendations:List<Recommendation> = emptyList(),val hidden_count:Int=0)
