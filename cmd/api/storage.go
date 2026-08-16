package main

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Storage interface {
	// Вакансии
	GetAllVacancies(ctx context.Context) ([]Vacancy, error)
	GetVacancyByID(ctx context.Context, id int) (Vacancy, error)
	CreateVacancy(ctx context.Context, v Vacancy) (int, error)
	IncrementVacancyViews(ctx context.Context, vacancyID int) error
	SearchVacancies(ctx context.Context, filters SearchFilters) ([]Vacancy, int, error)
	GetCandidateVacancies(ctx context.Context, skills []string, desiredPosition string, experience string, limit int) ([]Vacancy, error)

	// Резюме
	GetAllResumes(ctx context.Context) ([]Resume, error)
	GetResumeByID(ctx context.Context, id int) (Resume, error)
	GetResumeByUserID(ctx context.Context, userID int) (Resume, error)
	CreateResume(ctx context.Context, userID int, req CreateResumeRequest) (int, error)
	UpdateResume(ctx context.Context, resumeID int, userID int, req CreateResumeRequest) error
	DeleteResume(ctx context.Context, resumeID, userID int) error

	// Опыт работы
	CreateWorkExperience(ctx context.Context, resumeID int, req CreateWorkExperienceRequest) (int, error)
	GetWorkExperienceByResumeID(ctx context.Context, resumeID int) ([]WorkExperience, error)
	DeleteWorkExperience(ctx context.Context, id int) error

	// Избранное
	AddFavorite(ctx context.Context, userID, vacancyID int) error
	RemoveFavorite(ctx context.Context, userID, vacancyID int) error
	GetFavorites(ctx context.Context, userID int) ([]Vacancy, error)

	// Пользователи
	CreateUser(ctx context.Context, email, passwordHash, role string) (int, error)
	GetUserByEmail(ctx context.Context, email string) (User, error)
	GetUserByID(ctx context.Context, id int) (User, error)
	UpdateUserPhoto(ctx context.Context, userID int, photoURL string) error

	// Навыки и должности
	GetAllSkills(ctx context.Context) ([]Skill, error)
	CreateSkill(ctx context.Context, name string) (Skill, error)
	GetAllPositions(ctx context.Context) ([]Position, error)
	CreatePosition(ctx context.Context, name string) (Position, error)

	// Компании (публичные)
	GetAllCompanies(ctx context.Context) ([]CompanyWithStats, error)
	GetCompanyByID(ctx context.Context, id int) (CompanyWithStats, error)
	GetVacanciesByCompanyID(ctx context.Context, companyID int) ([]Vacancy, error)

	// Компании работодателя
	CreateCompany(ctx context.Context, req CreateCompanyRequest, ownerUserID int) (int, error)
	GetMyVacancies(ctx context.Context, companyID int) ([]Vacancy, error)

	// Счётчики просмотров
	IncrementResumeViews(ctx context.Context, resumeID int) error
	UpdateResumePhoto(ctx context.Context, resumeID, userID int, photoURL string) error

	// Отклики
	CreateApplication(ctx context.Context, vacancyID, candidateUserID int, req CreateApplicationRequest) (int, error)
	GetApplicationByUserAndVacancy(ctx context.Context, userID, vacancyID int) (*Application, error)
	GetApplicationsByVacancyID(ctx context.Context, vacancyID int) ([]Application, error)
	GetApplicationsForEmployer(ctx context.Context, companyID int) ([]Application, error)
	GetMyApplications(ctx context.Context, userID int) ([]Application, error)
	UpdateApplicationStatus(ctx context.Context, applicationID int, status string) error
	MarkApplicationViewed(ctx context.Context, applicationID int) error

	// Уведомления
	CreateNotification(ctx context.Context, req CreateNotificationRequest) error
	GetNotifications(ctx context.Context, userID int, limit int) ([]Notification, error)
	MarkNotificationsRead(ctx context.Context, userID int) error
	GetUnreadCount(ctx context.Context, userID int) (int, error)
	GetApplicationByID(ctx context.Context, appID int) (*Application, error)

	// Статистика
	GetEmployerStats(ctx context.Context, companyID int) (*EmployerStats, error)
	GetResumeStats(ctx context.Context, resumeID int) (*ResumeStats, error)

	HasRecentResumeViewNotification(ctx context.Context, resumeOwnerUserID, viewerUserID int, hours int) (bool, error)

	// Чат
	CreateConversation(ctx context.Context, applicationID int) (int, error)
	GetConversationByApplicationID(ctx context.Context, applicationID int) (*Conversation, error)
	CreateMessage(ctx context.Context, conversationID, senderID int, content string) (int, error)
	GetMessages(ctx context.Context, conversationID int) ([]Message, error)
	MarkMessagesRead(ctx context.Context, conversationID, userID int) error
	GetUserConversations(ctx context.Context, userID int) ([]ChatInfo, error)
	GetUnreadMessagesCount(ctx context.Context, userID int) (int, error)
}

type PostgresStorage struct {
	pool *pgxpool.Pool
}

func NewPostgresStorage(ctx context.Context, connString string) (*PostgresStorage, error) {
	pool, err := pgxpool.New(ctx, connString)
	if err != nil {
		return nil, err
	}

	if err := pool.Ping(ctx); err != nil {
		return nil, err
	}

	return &PostgresStorage{pool: pool}, nil
}

func (s *PostgresStorage) Close() {
	s.pool.Close()
}
