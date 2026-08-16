package main

import "time"

type Health struct {
	Status string `json:"status"`
}

type Vacancy struct {
	ID           int      `json:"id"`
	Title        string   `json:"title"`
	Company      string   `json:"company"`
	CompanyID    int      `json:"company_id"`
	Location     string   `json:"location"`
	Experience   string   `json:"experience"`
	Remote       bool     `json:"remote"`
	Skills       []string `json:"skills"`
	Description  string   `json:"description"`
	Address      string   `json:"address"`
	Latitude     float64  `json:"latitude"`
	Longitude    float64  `json:"longitude"`
	Views        int      `json:"views"`
	AuthorUserID int      `json:"author_user_id"`
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
	StartDate   string  `json:"start_date"`
	EndDate     *string `json:"end_date"`
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
	Location    string   `json:"location"`
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
	Role         string    `json:"role"`
	CompanyID    *int      `json:"company_id"`
	CreatedAt    time.Time `json:"created_at"`
}

type RegisterRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
	Role     string `json:"role"`
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

type Company struct {
	ID          int       `json:"id"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	Industry    string    `json:"industry"`
	Size        string    `json:"size"`
	City        string    `json:"city"`
	Website     string    `json:"website"`
	LogoURL     string    `json:"logo_url"`
	PhotoURL    string    `json:"photo_url"`
	CreatedAt   time.Time `json:"created_at"`
}

type CompanyWithStats struct {
	Company
	VacanciesCount int `json:"vacancies_count"`
	TotalViews     int `json:"total_views"`
}

type SearchFilters struct {
	Query      string   `json:"query"`
	Location   string   `json:"location"`
	Experience string   `json:"experience"`
	Remote     *bool    `json:"remote"`
	Skills     []string `json:"skills"`
	Limit      int      `json:"limit"`
	Offset     int      `json:"offset"`
}

type CreateVacancyRequest struct {
	Title       string   `json:"title"`
	Location    string   `json:"location"`
	Experience  string   `json:"experience"`
	Remote      bool     `json:"remote"`
	Skills      []string `json:"skills"`
	Description string   `json:"description"`
	Address     string   `json:"address"`
	Latitude    float64  `json:"latitude"`
	Longitude   float64  `json:"longitude"`
}

type CreateCompanyRequest struct {
	Name        string `json:"name"`
	Description string `json:"description"`
	Industry    string `json:"industry"`
	Size        string `json:"size"`
	City        string `json:"city"`
	Website     string `json:"website"`
}

type MyVacanciesResponse struct {
	Company   Company   `json:"company"`
	Vacancies []Vacancy `json:"vacancies"`
}

// ====== ОТКЛИКИ ======
type Application struct {
	ID              int        `json:"id"`
	VacancyID       int        `json:"vacancy_id"`
	CandidateUserID int        `json:"candidate_user_id"`
	ResumeID        *int       `json:"resume_id"`
	CoverLetter     string     `json:"cover_letter"`
	Status          string     `json:"status"`
	CreatedAt       time.Time  `json:"created_at"`
	ViewedAt        *time.Time `json:"viewed_at,omitempty"`

	// Заполняется через JOIN
	CandidateEmail   string `json:"candidate_email,omitempty"`
	CandidatePhoto   string `json:"candidate_photo,omitempty"`
	ResumeFullName   string `json:"resume_full_name,omitempty"`
	ResumePosition   string `json:"resume_position,omitempty"`
	ResumeExperience string `json:"resume_experience,omitempty"`
	VacancyTitle     string `json:"vacancy_title,omitempty"`
	CompanyName      string `json:"company_name,omitempty"`
	VacancyCompanyID int    `json:"vacancy_company_id,omitempty"` // ← НОВОЕ (для проверок)
}

type CreateApplicationRequest struct {
	CoverLetter string `json:"cover_letter"`
}

// ====== УВЕДОМЛЕНИЯ ======
type Notification struct {
	ID        int                    `json:"id"`
	UserID    int                    `json:"user_id"`
	Type      string                 `json:"type"`
	Title     string                 `json:"title"`
	Message   string                 `json:"message"`
	Data      map[string]interface{} `json:"data"`
	IsRead    bool                   `json:"is_read"`
	CreatedAt time.Time              `json:"created_at"`
}

type CreateNotificationRequest struct {
	UserID  int
	Type    string
	Title   string
	Message string
	Data    map[string]interface{}
}
