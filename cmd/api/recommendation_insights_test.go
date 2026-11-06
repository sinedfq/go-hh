package main

import (
	"reflect"
	"strings"
	"testing"
)

func TestRecommendationInsightsUseFactsAndNormalizeSkillDuplicates(t *testing.T) {
	resume := Resume{Skills: []string{" go ", "SQL"}, City: " Новосибирск ", Experience: "Middle", Remote: true}
	vacancy := Vacancy{Skills: []string{"Go", " go ", "Kotlin", "", "SQL"}, Location: "новосибирск", Experience: "Middle", Remote: true}
	got := recommendationInsights(resume, vacancy)
	if !reflect.DeepEqual(got.MatchedSkills, []string{"Go", "SQL"}) || !reflect.DeepEqual(got.MissingSkills, []string{"Kotlin"}) {
		t.Fatalf("wrong skills: %+v", got)
	}
	if len(got.Reasons) != 4 || len(got.Considerations) != 1 {
		t.Fatalf("wrong facts: %+v", got)
	}
	vacancy.Location = "Москва"
	vacancy.Remote = false
	vacancy.Experience = "Senior"
	got = recommendationInsights(resume, vacancy)
	text := strings.Join(got.Considerations, " ")
	for _, want := range []string{"Москва", "Senior", "удалённый", "Kotlin"} {
		if !strings.Contains(text, want) {
			t.Fatalf("missing %q: %+v", want, got)
		}
	}
}

func TestRecommendationInsightsDoNotInferMissingProfileFields(t *testing.T) {
	got := recommendationInsights(Resume{}, Vacancy{Remote: true})
	if len(got.Reasons) != 0 || len(got.Considerations) != 0 || got.MatchedSkills == nil || got.MissingSkills == nil {
		t.Fatalf("invented information or null arrays: %+v", got)
	}
	visible := visibleRecommendations([]Recommendation{{Vacancy: Vacancy{ID: 7}}, {Vacancy: Vacancy{ID: 8}}}, map[int]bool{7: true})
	if len(visible) != 1 || visible[0].Vacancy.ID != 8 {
		t.Fatalf("cached hidden result reappeared: %+v", visible)
	}
}

func TestRecommendationCacheKeyIncludesPreferencesIndependentOfMapOrder(t *testing.T) {
	resume := Resume{Skills: []string{"Go"}}
	first := recommendationProfileKey(resume, map[int]bool{7: true, 8: true})
	if first != recommendationProfileKey(resume, map[int]bool{8: true, 7: true}) {
		t.Fatal("map order must not invalidate cache")
	}
	if first == recommendationProfileKey(resume, map[int]bool{7: true}) || first == recommendationProfileKey(resume, nil) {
		t.Fatal("in-flight stale preferences must not share a cache key")
	}
}
