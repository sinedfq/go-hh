package main

type Health struct {
	Status string `json:"status"`
}

type Vacancy struct {
	ID          int      `json:"id"`
	Title       string   `json:"title"`
	Company     string   `json:"company"`
	Location    string   `json:"city"`
	Experience  string   `json:"experience"`
	Remote      bool     `json:"remote"`
	Skills      []string `json:"skills"`
	Description string   `json:"description"`
}

type Resume struct {
	ID              int      `json:"id"`
	Title           string   `json:"title"`
	Skills          []string `json:"skills"`
	ExperienceYears int      `json:"experience_years"`
	ExpectedSalary  int      `json:"expected_salary"`
	About           string   `json:"about"`
}

type MLVacancy struct {
	ID          int      `json:"id"`
	Title       string   `json:"title"`
	Company     string   `json:"company"`
	City        string   `json:"city"`
	Experience  string   `json:"experience"`
	Remote      bool     `json:"remote"`
	Skills      []string `json:"skills"`
	Description string   `json:"description"`
}

type MLResume struct {
	ID              int      `json:"id"`
	Title           string   `json:"title"`
	Skills          []string `json:"skills"`
	ExperienceYears int      `json:"experience_years"`
	ExpectedSalary  int      `json:"expected_salary"`
	About           string   `json:"about"`
}

type MLMatchRequest struct {
	Resume    MLResume    `json:"resume"`
	Vacancies []MLVacancy `json:"vacancies"`
}

type MLMatchResult struct {
	VacancyID int     `json:"vacancy_id"`
	Score     float64 `json:"score"`
	Reasoning string  `json:"reasoning"`
}

type MLMatchResponse struct {
	ResumeID  int             `json:"resume_id"`
	Matches   []MLMatchResult `json:"matches"`
	ModelUsed string          `json:"model_used"`
}

type Match struct {
	Vacancy Vacancy `json:"vacancy"`
	Score   float64 `json:"score"`
}
