package main

type Health struct {
	Status string `json:"status"`
}

type Vacancy struct {
	ID         int    `json: "id"`
	Title      string `json: "title"`
	Company    string `json: "company"`
	Location   string `json: "city"`
	Experience string `json: "experience"`
	Remote     bool   `json: "remote"`
}
