package main

import (
	"sort"
	"strings"
)

func MatchVacancies(resume Resume, vacancies []Vacancy) []Match {
	var matches []Match

	for _, v := range vacancies {
		score := calculateMatchScore(resume, v)
		matches = append(matches, Match{
			Vacancy: v,
			Score:   score,
		})
	}

	sort.Slice(matches, func(i, j int) bool {
		return matches[i].Score > matches[j].Score
	})

	return matches
}

func calculateMatchScore(resume Resume, vacancy Vacancy) float64 {
	score := 0.0
	maxScore := 0.0

	maxScore += 0.6
	titleLower := strings.ToLower(vacancy.Title)

	if len(resume.Skills) > 0 {
		matchedSkills := 0
		for _, skill := range resume.Skills {
			if strings.Contains(titleLower, strings.ToLower(skill)) {
				matchedSkills++
			}
		}
		score += 0.6 * float64(matchedSkills) / float64(len(resume.Skills))
	}

	// 2. Удалёнка (вес 0.2)
	maxScore += 0.2
	if vacancy.Remote {
		score += 0.2
	}

	// 3. Опыт (вес 0.2)
	maxScore += 0.2
	expLower := strings.ToLower(vacancy.Experience)

	if resume.ExperienceYears >= 5 && strings.Contains(expLower, "senior") {
		score += 0.2
	} else if resume.ExperienceYears >= 2 && resume.ExperienceYears < 5 && strings.Contains(expLower, "middle") {
		score += 0.2
	} else if resume.ExperienceYears <= 2 && strings.Contains(expLower, "junior") {
		score += 0.2
	}

	return score / maxScore
}
