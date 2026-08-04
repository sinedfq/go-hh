package main

import (
	"encoding/json"
	"net/http"
)

type Health struct {
	Status string `json:"status"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	data := Health{
		Status: "ok",
	}
	json.NewEncoder(w).Encode(data)
}

func main() {
	http.HandleFunc("/health", healthHandler)
	http.ListenAndServe(":8080", nil)
}
