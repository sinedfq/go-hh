package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
)

type VacancyTerms struct {
	SalaryFrom  int    `json:"salary_from"`
	SalaryTo    int    `json:"salary_to"`
	Currency    string `json:"currency"`
	SalaryGross bool   `json:"salary_gross"`
	Employment  string `json:"employment"`
	Schedule    string `json:"schedule"`
}

func (t VacancyTerms) validate() error {
	if t.SalaryFrom < 0 || t.SalaryTo < 0 || t.SalaryFrom > 1000000000 || t.SalaryTo > 1000000000 || (t.SalaryTo > 0 && t.SalaryFrom > t.SalaryTo) {
		return fmt.Errorf("invalid salary range")
	}
	if t.Currency != "" && t.Currency != "RUB" && t.Currency != "USD" && t.Currency != "EUR" {
		return fmt.Errorf("invalid currency")
	}
	if t.Employment != "" && t.Employment != "full_time" && t.Employment != "part_time" && t.Employment != "project" && t.Employment != "internship" {
		return fmt.Errorf("invalid employment")
	}
	if t.Schedule != "" && t.Schedule != "full_day" && t.Schedule != "flexible" && t.Schedule != "shift" {
		return fmt.Errorf("invalid schedule")
	}
	return nil
}
func (t VacancyTerms) json() []byte { data, _ := json.Marshal(t); return data }
func parseExtendedFilters(r *http.Request, f *SearchFilters) error {
	q := r.URL.Query()
	for key, target := range map[string]*int{"salary_min": &f.SalaryMin, "salary_max": &f.SalaryMax} {
		if q.Get(key) != "" {
			n, err := strconv.Atoi(q.Get(key))
			if err != nil {
				return fmt.Errorf("invalid salary")
			}
			*target = n
		}
	}
	f.Currency = q.Get("currency")
	f.Employment = q.Get("employment")
	f.Schedule = q.Get("schedule")
	f.SalaryOnly = q.Get("salary_only") == "true"
	return (VacancyTerms{SalaryFrom: f.SalaryMin, SalaryTo: f.SalaryMax, Currency: f.Currency, Employment: f.Employment, Schedule: f.Schedule}).validate()
}
