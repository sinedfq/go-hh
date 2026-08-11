package main

import (
	"fmt"
	"math"
	"sort"
	"strings"
)

// Match представляет результат сопоставления
type Match struct {
	VacancyID int     `json:"vacancy_id"`
	Score     float64 `json:"score"`
	Reasoning string  `json:"reasoning"`
}

// SimpleMatch выполняет простое сопоставление без ML-сервиса
func SimpleMatch(resume Resume, vacancies []Vacancy) []Match {
	var matches []Match

	for _, v := range vacancies {
		score := 0.0
		var reasons []string

		// Совпадение навыков
		skillMatches := 0
		for _, rSkill := range resume.Skills {
			for _, vSkill := range v.Skills {
				if strings.EqualFold(rSkill, vSkill) {
					skillMatches++
					reasons = append(reasons, fmt.Sprintf("Навык: %s", rSkill))
				}
			}
		}

		if len(resume.Skills) > 0 {
			skillScore := float64(skillMatches) / float64(len(resume.Skills))
			score += skillScore * 0.6 // 60% веса на навыки
		}

		// Совпадение уровня опыта
		if resume.Experience == v.Experience {
			score += 0.2 // 20% веса на опыт
			reasons = append(reasons, "Подходит уровень опыта")
		}

		// Совпадение по удалёнке
		if resume.Remote && v.Remote {
			score += 0.1 // 10% веса
			reasons = append(reasons, "Удалённая работа")
		}

		// Совпадение по городу
		if resume.City != "" && strings.EqualFold(resume.City, v.Location) {
			score += 0.1 // 10% веса
			reasons = append(reasons, "Город совпадает")
		}

		// Округляем до 2 знаков
		score = math.Round(score*100) / 100

		reasoning := strings.Join(reasons, ", ")
		if reasoning == "" {
			reasoning = "Частичное совпадение"
		}

		matches = append(matches, Match{
			VacancyID: v.ID,
			Score:     score,
			Reasoning: reasoning,
		})
	}

	// Сортируем по score (убывание)
	sort.Slice(matches, func(i, j int) bool {
		return matches[i].Score > matches[j].Score
	})

	return matches
}
