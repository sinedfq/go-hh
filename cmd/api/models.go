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

type Resume struct {
	ID              int      `json:"id"`
	Title           string   `json:"title"`
	Skills          []string `json:"skills"`
	ExperienceYears int      `json:"experience_years"`
	ExpectedSalary  int      `json:"expected_salary"`
	About           string   `json:"about"`
}
