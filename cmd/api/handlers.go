package main

import (
	"encoding/json"
	"net/http"
)

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	data := Health{
		Status: "ok",
	}
	json.NewEncoder(w).Encode(data)
}

func vacancyHendler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	vacancies := []Vacancy{
		{
			ID:       1,
			Title:    "Go Backend Developer",
			Company:  "Some Company",
			Location: "Moscow",
			Remote:   true,
		},
		{
			ID:       2,
			Title:    "Junior Go Developer",
			Company:  "Another Company",
			Location: "Saint Petersburg",
			Remote:   false,
		},
	}

	json.NewEncoder(w).Encode(vacancies)
}
