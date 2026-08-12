package main

import "time"

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
	ID              int              `json:"id"`
	UserID          int              `json:"user_id"`
	FullName        string           `json:"full_name"`
	DesiredPosition string           `json:"desired_position"`
	Experience      string           `json:"experience"`
	Skills          []string         `json:"skills"`
	About           string           `json:"about"`
	City            string           `json:"city"`
	Remote          bool             `json:"remote"`
	Phone           string           `json:"phone"`
	Telegram        string           `json:"telegram"`
	GitHub          string           `json:"github"`
	LinkedIn        string           `json:"linkedin"`
	CreatedAt       time.Time        `json:"created_at"`
	WorkExperience  []WorkExperience `json:"work_experience,omitempty"`
	PhotoURL        string           `json:"photo_url"`
	Views           int              `json:"views"`
}

type WorkExperience struct {
	ID          int        `json:"id"`
	ResumeID    int        `json:"resume_id"`
	Company     string     `json:"company"`
	Position    string     `json:"position"`
	StartDate   time.Time  `json:"start_date"`
	EndDate     *time.Time `json:"end_date"`
	Description string     `json:"description"`
	CreatedAt   time.Time  `json:"created_at"`
}

type CreateWorkExperienceRequest struct {
	Company     string  `json:"company"`
	Position    string  `json:"position"`
	StartDate   string  `json:"start_date"` // приходит как строка "2024-01-01"
	EndDate     *string `json:"end_date"`   // строка или null
	Description string  `json:"description"`
}
type CreateResumeRequest struct {
	FullName        string   `json:"full_name"`
	DesiredPosition string   `json:"desired_position"`
	Experience      string   `json:"experience"`
	Skills          []string `json:"skills"`
	About           string   `json:"about"`
	City            string   `json:"city"`
	Remote          bool     `json:"remote"`
	Phone           string   `json:"phone"`
	Telegram        string   `json:"telegram"`
	GitHub          string   `json:"github"`
	LinkedIn        string   `json:"linkedin"`
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
	FullName        string   `json:"full_name"`
	DesiredPosition string   `json:"desired_position"`
	Experience      string   `json:"experience"`
	Skills          []string `json:"skills"`
	About           string   `json:"about"`
	City            string   `json:"city"`
	Remote          bool     `json:"remote"`
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

type User struct {
	ID           int       `json:"id"`
	Email        string    `json:"email"`
	PasswordHash string    `json:"-"`
	PhotoURL     string    `json:"photo_url"`
	CreatedAt    time.Time `json:"created_at"`
}

type RegisterRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type AuthResponse struct {
	Token string `json:"token"`
	User  User   `json:"user"`
}

type Skill struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
}

type Position struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
}

type Recommendation struct {
	Vacancy   Vacancy `json:"vacancy"`
	Score     float64 `json:"score"`
	Reasoning string  `json:"reasoning"`
}
