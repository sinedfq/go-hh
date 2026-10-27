package main

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestBasicRecommendationsRankRealSkillsAndPreferences(t *testing.T) {
	resume := Resume{DesiredPosition: "Go Developer", Experience: "Middle", Remote: true, Skills: []string{" Go ", "POSTGRESQL", "go"}}
	vacancies := []Vacancy{
		{ID: 1, Title: "Go Developer", Experience: "Middle", Remote: true, Skills: []string{"go", "PostgreSQL", "GO"}},
		{ID: 2, Title: "Go Developer", Experience: "Senior", Skills: []string{"Go", "Docker", "Redis"}},
		{ID: 3, Title: "React Developer", Experience: "Middle", Remote: true, Skills: []string{"React"}},
		{ID: 4, Title: "Go Developer", Experience: "Middle", Remote: true, Skills: nil},
	}
	matches := basicRecommendations(resume, vacancies)
	if len(matches) != 2 || matches[0].VacancyID != 1 || matches[1].VacancyID != 2 {
		t.Fatalf("unexpected ordering or unrelated vacancies: %#v", matches)
	}
	if matches[0].Score != 1 || matches[1].Score >= .5 || !strings.Contains(matches[0].Reasoning, "2 из 2") {
		t.Fatalf("scores must reflect unique skills, experience and remote preference: %#v", matches)
	}
}

func TestRecommendationsUseFallbackOnMLFailureAndRecover(t *testing.T) {
	resume := Resume{DesiredPosition: "Go Developer", Skills: []string{"Go"}}
	candidates := []Vacancy{{ID: 7, Title: "Go Developer", Skills: []string{"Go"}}}
	for _, status := range []int{503, 200} {
		t.Run(http.StatusText(status), func(t *testing.T) {
			ml := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(status)
				if status == 200 {
					_, _ = w.Write([]byte(`{"matches":[{"vacancy_id":7,"score":0.91,"reasoning":"Model result"}]}`))
				}
			}))
			defer ml.Close()
			s := &Server{mlClient: NewMLClient(ml.URL)}
			matches, model, err := s.rankRecommendations(context.Background(), resume, candidates)
			if err != nil || len(matches) != 1 || matches[0].VacancyID != 7 {
				t.Fatalf("recommendations unavailable: %v %#v", err, matches)
			}
			if status == 503 && (model != basicRecommendationModel || !strings.Contains(matches[0].Reasoning, "Базовый подбор")) {
				t.Fatalf("fallback must be identified: %s %#v", model, matches)
			}
			if status == 200 && (model != "ml-service" || matches[0].Score != .91) {
				t.Fatalf("working ML must retain its scores: %s %#v", model, matches)
			}
		})
	}
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	if _, _, err := (&Server{}).rankRecommendations(ctx, resume, candidates); err != context.Canceled {
		t.Fatalf("cancelled request must stop: %v", err)
	}
}

func TestBasicRecommendationCacheExpiresSoonerThanMLCache(t *testing.T) {
	cache := NewRecommendationsCache()
	cache.Set(1, nil, "resume", basicRecommendationModel)
	cache.Set(2, nil, "resume", "ml-service")
	cache.entries[1].CreatedAt = time.Now().Add(-time.Minute)
	cache.entries[2].CreatedAt = time.Now().Add(-time.Minute)
	if _, found := cache.Get(1, "resume"); found {
		t.Fatal("fallback must not mask recovered ML for ten minutes")
	}
	if _, found := cache.Get(2, "resume"); !found {
		t.Fatal("normal ML cache TTL changed")
	}
}
