package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"sort"
	"strings"
)

func recommendationProfileKey(resume Resume, hidden map[int]bool) string {
	ids := make([]int, 0, len(hidden))
	for id, dismissed := range hidden {
		if dismissed {
			ids = append(ids, id)
		}
	}
	sort.Ints(ids)
	data, _ := json.Marshal(struct {
		Resume string
		Hidden []int
	}{HashResume(resume), ids})
	digest := sha256.Sum256(data)
	return hex.EncodeToString(digest[:])
}

type RecommendationInsights struct {
	MatchedSkills  []string `json:"matched_skills"`
	MissingSkills  []string `json:"missing_skills"`
	Reasons        []string `json:"reasons"`
	Considerations []string `json:"considerations"`
}

// Explanations describe observable profile fields, independently of the ranking model.
func recommendationInsights(resume Resume, vacancy Vacancy) RecommendationInsights {
	result := RecommendationInsights{[]string{}, []string{}, []string{}, []string{}}
	normalize := func(value string) string { return strings.ToLower(strings.TrimSpace(value)) }
	owned := make(map[string]bool)
	for _, skill := range resume.Skills {
		owned[normalize(skill)] = true
	}
	seen := make(map[string]bool)
	for _, skill := range vacancy.Skills {
		key := normalize(skill)
		if key == "" || seen[key] {
			continue
		}
		seen[key] = true
		if owned[key] {
			result.MatchedSkills = append(result.MatchedSkills, strings.TrimSpace(skill))
		} else {
			result.MissingSkills = append(result.MissingSkills, strings.TrimSpace(skill))
		}
	}
	if len(result.MatchedSkills) > 0 {
		result.Reasons = append(result.Reasons, "В резюме есть навыки из вакансии: "+strings.Join(result.MatchedSkills, ", "))
	}
	if len(result.MissingSkills) > 0 {
		result.Considerations = append(result.Considerations, "В резюме не указаны: "+strings.Join(result.MissingSkills, ", "))
	}
	if resume.Remote && vacancy.Remote {
		result.Reasons = append(result.Reasons, "Удалённый формат соответствует резюме")
	} else if resume.Remote && !vacancy.Remote {
		result.Considerations = append(result.Considerations, "В резюме выбран удалённый формат, вакансия не отмечена как удалённая")
	}
	if normalize(resume.City) != "" && normalize(vacancy.Location) != "" {
		if normalize(resume.City) == normalize(vacancy.Location) {
			result.Reasons = append(result.Reasons, "Совпадает город: "+vacancy.Location)
		} else if !vacancy.Remote {
			result.Considerations = append(result.Considerations, "Город вакансии отличается: "+vacancy.Location)
		}
	}
	if normalize(resume.Experience) != "" && normalize(vacancy.Experience) != "" {
		if normalize(resume.Experience) == normalize(vacancy.Experience) {
			result.Reasons = append(result.Reasons, "Совпадает уровень опыта: "+vacancy.Experience)
		} else {
			result.Considerations = append(result.Considerations, "Опыт в вакансии: "+vacancy.Experience+"; в резюме: "+resume.Experience)
		}
	}
	return result
}

func visibleRecommendations(items []Recommendation, hidden map[int]bool) []Recommendation {
	result := make([]Recommendation, 0, len(items))
	for _, item := range items {
		if !hidden[item.Vacancy.ID] {
			result = append(result, item)
		}
	}
	return result
}
