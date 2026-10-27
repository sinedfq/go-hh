package main

import (
	"context"
	"fmt"
	"log"
	"math"
	"sort"
	"strings"
	"time"
)

const basicRecommendationModel = "basic-skills-v1"

// Keep the app usable when the optional ML process is stopped or unresponsive.
func (s *Server) rankRecommendations(ctx context.Context, resume Resume, candidates []Vacancy) ([]MLMatchResult, string, error) {
	if len(candidates) == 0 {
		return []MLMatchResult{}, "none", nil
	}
	if err := ctx.Err(); err != nil {
		return nil, "", err
	}
	if s.mlClient != nil {
		mlCtx, cancel := context.WithTimeout(ctx, 8*time.Second)
		matches, err := s.mlClient.MatchResumeToVacancies(mlCtx, resume, candidates)
		cancel()
		if err == nil {
			return matches, "ml-service", nil
		}
		if ctx.Err() != nil {
			return nil, "", ctx.Err()
		}
		log.Printf("Recommendations: ML unavailable, using basic skill matching: %v", err)
	}
	return basicRecommendations(resume, candidates), basicRecommendationModel, nil
}

func basicRecommendations(resume Resume, candidates []Vacancy) []MLMatchResult {
	normalizeSkills := func(skills []string) map[string]bool {
		result := map[string]bool{}
		for _, skill := range skills {
			if value := strings.ToLower(strings.TrimSpace(skill)); value != "" {
				result[value] = true
			}
		}
		return result
	}
	owned := normalizeSkills(resume.Skills)
	role := getRoleGroup(resume.DesiredPosition)
	matches := []MLMatchResult{}
	for _, vacancy := range candidates {
		required := normalizeSkills(vacancy.Skills)
		overlap := 0
		for skill := range required {
			if owned[skill] {
				overlap++
			}
		}
		if overlap == 0 {
			continue
		}
		score := .75 * float64(overlap) / float64(len(required))
		vacancyRole := getRoleGroup(vacancy.Title)
		if role != "" && (role == vacancyRole || role == "fullstack" || vacancyRole == "fullstack") {
			score += .15
		}
		if strings.EqualFold(resume.Experience, vacancy.Experience) && resume.Experience != "" {
			score += .10
		} else if experienceToYears(vacancy.Experience) > experienceToYears(resume.Experience)+1 {
			score *= .8
		}
		if role != "" && vacancyRole != "" && role != vacancyRole && role != "fullstack" && vacancyRole != "fullstack" {
			score = math.Min(score, .35)
		}
		if resume.Remote && !vacancy.Remote {
			score *= .7
		}
		matches = append(matches, MLMatchResult{VacancyID: vacancy.ID, Score: math.Round(score*1000) / 1000,
			Reasoning: fmt.Sprintf("Базовый подбор: совпало навыков %d из %d. Учитываются должность, уровень опыта и формат работы.", overlap, len(required))})
	}
	sort.SliceStable(matches, func(i, j int) bool { return matches[i].Score > matches[j].Score })
	return matches
}
