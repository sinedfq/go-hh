package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

type MLClient struct {
	baseURL    string
	httpClient *http.Client
}

func NewMLClient(baseURL string) *MLClient {
	return &MLClient{
		baseURL: baseURL,
		httpClient: &http.Client{
			Timeout: 60 * time.Second,
		},
	}
}

// experienceToYears конвертирует строковый уровень опыта в число лет
func experienceToYears(exp string) int {
	switch exp {
	case "Junior":
		return 1
	case "Middle":
		return 3
	case "Senior":
		return 6
	case "Lead":
		return 8
	default:
		return 1
	}
}

func (c *MLClient) MatchResumeToVacancies(ctx context.Context, resume Resume, vacancies []Vacancy) ([]MLMatchResult, error) {
	mlResume := MLResume{
		ID:              resume.ID,
		FullName:        resume.FullName,
		DesiredPosition: resume.DesiredPosition,
		Experience:      resume.Experience,
		Skills:          resume.Skills,
		About:           resume.About,
		City:            resume.City,
		Remote:          resume.Remote,
	}

	mlVacancies := make([]MLVacancy, len(vacancies))
	for i, v := range vacancies {
		mlVacancies[i] = MLVacancy{
			ID:          v.ID,
			Title:       v.Title,
			Company:     v.Company,
			City:        v.Location, // Vacancy.Location → MLVacancy.City
			Experience:  v.Experience,
			Remote:      v.Remote,
			Skills:      v.Skills,
			Description: v.Description,
		}
	}

	request := MLMatchRequest{
		Resume:    mlResume,
		Vacancies: mlVacancies,
	}

	jsonData, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	url := fmt.Sprintf("%s/match", c.baseURL)
	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send request to ML service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("ML service returned status %d: %s", resp.StatusCode, string(body))
	}

	var mlResponse MLMatchResponse
	if err := json.NewDecoder(resp.Body).Decode(&mlResponse); err != nil {
		return nil, fmt.Errorf("failed to decode ML response: %w", err)
	}

	return mlResponse.Matches, nil
}
