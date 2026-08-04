package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/health", healthHandler)
	http.HandleFunc("/api/vacancies", vacancyHendler)
	http.ListenAndServe(":8080", nil)
}
