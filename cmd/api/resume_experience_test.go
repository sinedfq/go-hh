package main

import (
	"testing"
	"time"
)

func TestExperienceUnionsDates(t *testing.T) {
	date := func(s string) time.Time {
		v, e := time.Parse("2006-01-02", s)
		if e != nil {
			t.Fatal(e)
		}
		return v
	}
	job := func(start, end string) WorkExperience {
		e := date(end)
		return WorkExperience{StartDate: date(start), EndDate: &e}
	}
	now := date("2026-10-21")
	for _, test := range []struct {
		name   string
		jobs   []WorkExperience
		months int
		label  string
	}{
		{"empty", nil, 0, "Не указан"},
		{"overlap", []WorkExperience{job("2022-01-01", "2024-01-01"), job("2023-01-01", "2025-01-01")}, 36, "3 года"},
		{"gaps", []WorkExperience{job("2020-01-01", "2021-01-01"), job("2022-01-01", "2022-04-01")}, 15, "1 год 3 месяца"},
		{"ongoing", []WorkExperience{{StartDate: date("2025-08-21")}}, 14, "1 год 2 месяца"},
		{"partial month", []WorkExperience{job("2026-10-02", "2026-10-20")}, 0, "Менее месяца"},
		{"short jobs accumulate", []WorkExperience{job("2024-01-01", "2024-01-21"), job("2024-02-01", "2024-02-21"), job("2024-03-01", "2024-03-21")}, 2, "2 месяца"},
		{"future ignored", []WorkExperience{job("2027-01-01", "2027-02-01")}, 0, "Не указан"},
	} {
		t.Run(test.name, func(t *testing.T) {
			months, has := resumeExperienceMonths(test.jobs, now)
			if months != test.months || experienceLabel(months, has) != test.label {
				t.Fatalf("%d %s", months, experienceLabel(months, has))
			}
		})
	}
}
