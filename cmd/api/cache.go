package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"log"
	"sync"
	"time"
)

// Время жизни кэша рекомендаций
const recommendationsCacheTTL = 10 * time.Minute

// CacheEntry хранит закэшированные рекомендации
type CacheEntry struct {
	Recommendations []Recommendation
	ResumeHash      string
	ModelUsed       string
	CreatedAt       time.Time
}

// RecommendationsCache — потокобезопасный кэш рекомендаций в памяти
type RecommendationsCache struct {
	mu      sync.RWMutex
	entries map[int]*CacheEntry
}

func NewRecommendationsCache() *RecommendationsCache {
	return &RecommendationsCache{
		entries: make(map[int]*CacheEntry),
	}
}

// Get возвращает кэш если он валиден (не просрочен и резюме не менялось)
func (c *RecommendationsCache) Get(userID int, resumeHash string) (*CacheEntry, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()

	entry, ok := c.entries[userID]
	if !ok {
		return nil, false
	}

	// TTL истёк
	if time.Since(entry.CreatedAt) > recommendationsCacheTTL {
		return nil, false
	}

	// Резюме изменилось — кэш невалиден
	if entry.ResumeHash != resumeHash {
		return nil, false
	}

	return entry, true
}

// Set сохраняет результат в кэш
func (c *RecommendationsCache) Set(userID int, recs []Recommendation, resumeHash, modelUsed string) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.entries[userID] = &CacheEntry{
		Recommendations: recs,
		ResumeHash:      resumeHash,
		ModelUsed:       modelUsed,
		CreatedAt:       time.Now(),
	}
	log.Printf("Cache SET for user %d (%d recommendations)", userID, len(recs))
}

// Invalidate удаляет кэш конкретного пользователя
func (c *RecommendationsCache) Invalidate(userID int) {
	c.mu.Lock()
	defer c.mu.Unlock()
	delete(c.entries, userID)
	log.Printf("Cache INVALIDATED for user %d", userID)
}

// InvalidateAll очищает весь кэш (при добавлении новых вакансий)
func (c *RecommendationsCache) InvalidateAll() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.entries = make(map[int]*CacheEntry)
	log.Printf("Cache INVALIDATED for all users")
}

// HashResume создаёт отпечаток резюме для отслеживания изменений
func HashResume(resume Resume) string {
	data, _ := json.Marshal(map[string]any{
		"full_name":        resume.FullName,
		"desired_position": resume.DesiredPosition,
		"experience":       resume.Experience,
		"skills":           resume.Skills,
		"about":            resume.About,
		"city":             resume.City,
		"remote":           resume.Remote,
		"phone":            resume.Phone,
		"telegram":         resume.Telegram,
		"github":           resume.GitHub,
		"linkedin":         resume.LinkedIn,
		"work_experience":  resume.WorkExperience,
	})
	hash := sha256.Sum256(data)
	return hex.EncodeToString(hash[:])
}
