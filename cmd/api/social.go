package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"path"
	"path/filepath"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/jackc/pgx/v5"
)

type SocialPost struct {
	ID          int64     `json:"id"`
	UserID      int       `json:"user_id"`
	CompanyID   *int      `json:"company_id"`
	AuthorName  string    `json:"author_name"`
	AuthorPhoto string    `json:"author_photo"`
	Body        string    `json:"body"`
	MediaURL    string    `json:"media_url"`
	MediaType   string    `json:"media_type"`
	CreatedAt   time.Time `json:"created_at"`
	Likes       int       `json:"likes"`
	Comments    int       `json:"comments"`
	Liked       bool      `json:"liked"`
	CanDelete   bool      `json:"can_delete"`
}
type SocialComment struct {
	ID          int64     `json:"id"`
	UserID      int       `json:"user_id"`
	AuthorName  string    `json:"author_name"`
	AuthorPhoto string    `json:"author_photo"`
	Body        string    `json:"body"`
	CreatedAt   time.Time `json:"created_at"`
	CanDelete   bool      `json:"can_delete"`
}
type TimedStory struct {
	ID          int64     `json:"id"`
	CompanyID   int       `json:"company_id"`
	CompanyName string    `json:"company_name"`
	CompanyLogo string    `json:"company_logo"`
	Caption     string    `json:"caption"`
	MediaURL    string    `json:"media_url"`
	MediaType   string    `json:"media_type"`
	CreatedAt   time.Time `json:"created_at"`
	ExpiresAt   time.Time `json:"expires_at"`
	CanDelete   bool      `json:"can_delete"`
}

func socialJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
func socialError(w http.ResponseWriter, status int, message string) {
	socialJSON(w, status, map[string]string{"error": message})
}
func socialID(w http.ResponseWriter, r *http.Request) (int64, bool) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil || id <= 0 {
		socialError(w, 400, "Некорректный идентификатор")
		return 0, false
	}
	return id, true
}
func socialCursor(w http.ResponseWriter, r *http.Request) (int64, bool) {
	raw := r.URL.Query().Get("before")
	if raw == "" {
		return 0, true
	}
	v, e := strconv.ParseInt(raw, 10, 64)
	if e != nil || v < 1 {
		socialError(w, 400, "Некорректная страница")
		return 0, false
	}
	return v, true
}
func (s *Server) socialUser(w http.ResponseWriter, r *http.Request) (User, bool) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		socialError(w, 401, "Войдите в аккаунт")
		return User{}, false
	}
	u, e := s.storage.GetUserByID(r.Context(), claims.UserID)
	if e != nil {
		socialError(w, 401, "Войдите в аккаунт заново")
		return User{}, false
	}
	return u, true
}
func socialOwnCompany(u User) int {
	if (u.Role == "employer" || u.Role == "admin") && u.CompanyID != nil {
		return *u.CompanyID
	}
	return 0
}

func (s *Server) registerSocialRoutes(mux *http.ServeMux) {
	for _, pattern := range []string{"GET /api/companies/{id}/subscription", "PUT /api/companies/{id}/subscription", "DELETE /api/companies/{id}/subscription"} {
		mux.HandleFunc(pattern, s.authMiddleware(s.companySubscriptionHandler))
	}
	mux.HandleFunc("GET /api/feed", s.authMiddleware(s.socialFeedHandler))
	mux.HandleFunc("POST /api/posts", s.authMiddleware(s.createSocialPostHandler))
	mux.HandleFunc("DELETE /api/posts/{id}", s.authMiddleware(s.deleteSocialPostHandler))
	for _, pattern := range []string{"PUT /api/posts/{id}/like", "DELETE /api/posts/{id}/like"} {
		mux.HandleFunc(pattern, s.authMiddleware(s.socialLikeHandler))
	}
	for _, pattern := range []string{"GET /api/posts/{id}/comments", "POST /api/posts/{id}/comments"} {
		mux.HandleFunc(pattern, s.authMiddleware(s.socialCommentsHandler))
	}
	mux.HandleFunc("DELETE /api/comments/{id}", s.authMiddleware(s.deleteSocialCommentHandler))
	mux.HandleFunc("GET /api/stories", s.authMiddleware(s.socialStoriesHandler))
	mux.HandleFunc("POST /api/stories", s.authMiddleware(s.createSocialStoryHandler))
	mux.HandleFunc("DELETE /api/stories/{id}", s.authMiddleware(s.deleteSocialStoryHandler))
	mux.HandleFunc("GET /api/stories/{id}/media", s.socialStoryMediaHandler)
	// Story files must never bypass their database expiry check via static uploads.
	mux.HandleFunc("/uploads/.stories/", http.NotFound)
}

// Check the decoded path as well: an encoded slash can bypass ServeMux's
// segment-specific route while FileServer still resolves the private directory.
func protectStoryUploads(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		clean := path.Clean(r.URL.Path)
		if clean == "/uploads/.stories" || strings.HasPrefix(clean, "/uploads/.stories/") {
			http.NotFound(w, r)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func (s *Server) companySubscriptionHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	var exists bool
	if e := s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM companies WHERE id=$1)`, id).Scan(&exists); e != nil {
		socialError(w, 500, "Не удалось загрузить компанию")
		return
	}
	if !exists {
		socialError(w, 404, "Компания не найдена")
		return
	}
	var err error
	if r.Method == http.MethodPut {
		_, err = s.pool.Exec(r.Context(), `INSERT INTO company_subscriptions(user_id,company_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, u.ID, id)
	}
	if r.Method == http.MethodDelete {
		_, err = s.pool.Exec(r.Context(), `DELETE FROM company_subscriptions WHERE user_id=$1 AND company_id=$2`, u.ID, id)
	}
	var following bool
	var count int
	if err == nil {
		err = s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM company_subscriptions WHERE user_id=$1 AND company_id=$2),(SELECT COUNT(*) FROM company_subscriptions WHERE company_id=$2)`, u.ID, id).Scan(&following, &count)
	}
	if err != nil {
		socialError(w, 500, "Не удалось обновить подписку")
		return
	}
	socialJSON(w, 200, map[string]any{"following": following, "followers": count})
}

const socialPostSelect = `SELECT p.id,p.user_id,p.company_id,CASE WHEN p.company_id IS NOT NULL THEN c.name ELSE COALESCE(NULLIF((SELECT full_name FROM resumes WHERE user_id=u.id LIMIT 1),''),split_part(u.email,'@',1)) END,CASE WHEN p.company_id IS NOT NULL THEN COALESCE(c.logo_url,'') ELSE COALESCE(u.photo_url,'') END,p.body,p.media_url,p.media_type,p.created_at,(SELECT COUNT(*) FROM social_likes WHERE post_id=p.id),(SELECT COUNT(*) FROM social_comments WHERE post_id=p.id),EXISTS(SELECT 1 FROM social_likes WHERE post_id=p.id AND user_id=$1),(p.user_id=$1 OR (p.company_id IS NOT NULL AND p.company_id=$2)) FROM social_posts p JOIN users u ON u.id=p.user_id LEFT JOIN companies c ON c.id=p.company_id `

func (s *Server) socialFeedHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	before, ok := socialCursor(w, r)
	if !ok {
		return
	}
	following := r.URL.Query().Get("scope") == "following"
	company := 0
	if raw := r.URL.Query().Get("company_id"); raw != "" {
		v, e := strconv.Atoi(raw)
		if e != nil || v <= 0 {
			socialError(w, 400, "Некорректная компания")
			return
		}
		company = v
	}
	rows, e := s.pool.Query(r.Context(), socialPostSelect+`WHERE ($3::bigint=0 OR p.id<$3) AND ($5::int=0 OR p.company_id=$5) AND (NOT $4::boolean OR p.user_id=$1 OR p.company_id=$2 OR EXISTS(SELECT 1 FROM company_subscriptions f WHERE f.user_id=$1 AND f.company_id=p.company_id)) ORDER BY p.id DESC LIMIT 21`, u.ID, socialOwnCompany(u), before, following, company)
	if e != nil {
		socialError(w, 500, "Не удалось загрузить ленту")
		return
	}
	defer rows.Close()
	posts := []SocialPost{}
	for rows.Next() {
		var p SocialPost
		if e = rows.Scan(&p.ID, &p.UserID, &p.CompanyID, &p.AuthorName, &p.AuthorPhoto, &p.Body, &p.MediaURL, &p.MediaType, &p.CreatedAt, &p.Likes, &p.Comments, &p.Liked, &p.CanDelete); e != nil {
			socialError(w, 500, "Не удалось загрузить ленту")
			return
		}
		posts = append(posts, p)
	}
	if rows.Err() != nil {
		socialError(w, 500, "Не удалось загрузить ленту")
		return
	}
	var next int64
	if len(posts) > 20 {
		posts = posts[:20]
		next = posts[19].ID
	}
	socialJSON(w, 200, map[string]any{"posts": posts, "next_cursor": next})
}

// Parsing completes before saving; a failed database write removes its uploaded file.
func (s *Server) socialInput(w http.ResponseWriter, r *http.Request) (string, bool, bool) {
	if strings.HasPrefix(r.Header.Get("Content-Type"), "multipart/form-data") {
		r.Body = http.MaxBytesReader(w, r.Body, 52<<20)
		if e := r.ParseMultipartForm(5 << 20); e != nil {
			if r.MultipartForm != nil {
				_ = r.MultipartForm.RemoveAll()
			}
			socialError(w, 400, "Файл слишком большой или повреждён")
			return "", false, false
		}
		count := 0
		for key, files := range r.MultipartForm.File {
			if key != "photo" && key != "video" {
				socialError(w, 400, "Неизвестный тип вложения")
				return "", false, false
			}
			count += len(files)
		}
		if count > 1 {
			socialError(w, 400, "Прикрепите один файл")
			return "", false, false
		}
		return strings.TrimSpace(r.FormValue("body")), r.FormValue("as_company") == "true", true
	}
	var input struct {
		Body      string `json:"body"`
		AsCompany bool   `json:"as_company"`
	}
	if json.NewDecoder(http.MaxBytesReader(w, r.Body, 32<<10)).Decode(&input) != nil {
		socialError(w, 400, "Проверьте текст публикации")
		return "", false, false
	}
	return strings.TrimSpace(input.Body), input.AsCompany, true
}
func (s *Server) socialUpload(r *http.Request, prefix string) (string, string, error) {
	if r.MultipartForm == nil {
		return "", "", nil
	}
	if len(r.MultipartForm.File["video"]) > 0 {
		url, e := saveUploadedCompanyVideo(r, prefix)
		return url, "video", e
	}
	if len(r.MultipartForm.File["photo"]) > 0 {
		url, e := s.saveUploadedPhoto(r, prefix)
		return url, "image", e
	}
	return "", "", nil
}
func (s *Server) createSocialPostHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	defer func() {
		if r.MultipartForm != nil {
			_ = r.MultipartForm.RemoveAll()
		}
	}()
	body, asCompany, ok := s.socialInput(w, r)
	if !ok {
		return
	}
	if !utf8.ValidString(body) || utf8.RuneCountInString(body) > 5000 {
		socialError(w, 400, "Максимум 5000 символов")
		return
	}
	var company *int
	if asCompany {
		id := socialOwnCompany(u)
		if id == 0 {
			socialError(w, 403, "Публиковать от компании может только её представитель")
			return
		}
		company = &id
	}
	url, kind, e := s.socialUpload(r, fmt.Sprintf("post_%d", u.ID))
	if e != nil {
		socialError(w, 400, e.Error())
		return
	}
	if body == "" && url == "" {
		socialError(w, 400, "Добавьте текст или вложение")
		return
	}
	var id int64
	e = s.pool.QueryRow(r.Context(), `INSERT INTO social_posts(user_id,company_id,body,media_url,media_type) VALUES($1,$2,$3,$4,$5) RETURNING id`, u.ID, company, body, url, kind).Scan(&id)
	if e != nil {
		if url != "" {
			removeCompanyUpload(url)
		}
		socialError(w, 500, "Не удалось опубликовать пост")
		return
	}
	socialJSON(w, 201, map[string]any{"id": id})
}
func (s *Server) deleteSocialPostHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	var url string
	e := s.pool.QueryRow(r.Context(), `DELETE FROM social_posts WHERE id=$1 AND (user_id=$2 OR company_id=$3) RETURNING media_url`, id, u.ID, socialOwnCompany(u)).Scan(&url)
	if errors.Is(e, pgx.ErrNoRows) {
		socialError(w, 404, "Публикация не найдена или недоступна")
		return
	}
	if e != nil {
		socialError(w, 500, "Не удалось удалить пост")
		return
	}
	if url != "" {
		removeCompanyUpload(url)
	}
	w.WriteHeader(204)
}
func (s *Server) socialLikeHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	var exists bool
	if e := s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM social_posts WHERE id=$1)`, id).Scan(&exists); e != nil {
		socialError(w, 500, "Не удалось обновить лайк")
		return
	}
	if !exists {
		socialError(w, 404, "Пост не найден")
		return
	}
	var e error
	if r.Method == http.MethodPut {
		_, e = s.pool.Exec(r.Context(), `INSERT INTO social_likes(post_id,user_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, id, u.ID)
	} else {
		_, e = s.pool.Exec(r.Context(), `DELETE FROM social_likes WHERE post_id=$1 AND user_id=$2`, id, u.ID)
	}
	if e != nil {
		socialError(w, 500, "Не удалось обновить лайк")
		return
	}
	var count int
	if e = s.pool.QueryRow(r.Context(), `SELECT COUNT(*) FROM social_likes WHERE post_id=$1`, id).Scan(&count); e != nil {
		socialError(w, 500, "Не удалось загрузить лайки")
		return
	}
	socialJSON(w, 200, map[string]any{"likes": count, "liked": r.Method == http.MethodPut})
}
func (s *Server) socialCommentsHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	var exists bool
	if e := s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM social_posts WHERE id=$1)`, id).Scan(&exists); e != nil {
		socialError(w, 500, "Не удалось загрузить комментарии")
		return
	}
	if !exists {
		socialError(w, 404, "Пост не найден")
		return
	}
	if r.Method == http.MethodPost {
		var input struct {
			Body string `json:"body"`
		}
		if json.NewDecoder(http.MaxBytesReader(w, r.Body, 16<<10)).Decode(&input) != nil {
			socialError(w, 400, "Проверьте комментарий")
			return
		}
		input.Body = strings.TrimSpace(input.Body)
		if input.Body == "" || !utf8.ValidString(input.Body) || utf8.RuneCountInString(input.Body) > 2000 {
			socialError(w, 400, "Комментарий должен содержать от 1 до 2000 символов")
			return
		}
		var commentID int64
		if e := s.pool.QueryRow(r.Context(), `INSERT INTO social_comments(post_id,user_id,body) VALUES($1,$2,$3) RETURNING id`, id, u.ID, input.Body).Scan(&commentID); e != nil {
			socialError(w, 500, "Не удалось отправить комментарий")
			return
		}
		socialJSON(w, 201, map[string]any{"id": commentID})
		return
	}
	before, ok := socialCursor(w, r)
	if !ok {
		return
	}
	rows, e := s.pool.Query(r.Context(), `SELECT x.id,x.user_id,COALESCE(NULLIF((SELECT full_name FROM resumes WHERE user_id=u.id LIMIT 1),''),split_part(u.email,'@',1)),COALESCE(u.photo_url,''),x.body,x.created_at,x.user_id=$2 FROM social_comments x JOIN users u ON u.id=x.user_id WHERE x.post_id=$1 AND ($3::bigint=0 OR x.id<$3) ORDER BY x.id DESC LIMIT 51`, id, u.ID, before)
	if e != nil {
		socialError(w, 500, "Не удалось загрузить комментарии")
		return
	}
	defer rows.Close()
	items := []SocialComment{}
	for rows.Next() {
		var x SocialComment
		if e = rows.Scan(&x.ID, &x.UserID, &x.AuthorName, &x.AuthorPhoto, &x.Body, &x.CreatedAt, &x.CanDelete); e != nil {
			socialError(w, 500, "Не удалось загрузить комментарии")
			return
		}
		items = append(items, x)
	}
	if rows.Err() != nil {
		socialError(w, 500, "Не удалось загрузить комментарии")
		return
	}
	var next int64
	if len(items) > 50 {
		items = items[:50]
		next = items[49].ID
	}
	socialJSON(w, 200, map[string]any{"comments": items, "next_cursor": next})
}
func (s *Server) deleteSocialCommentHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	result, e := s.pool.Exec(r.Context(), `DELETE FROM social_comments WHERE id=$1 AND user_id=$2`, id, u.ID)
	if e != nil {
		socialError(w, 500, "Не удалось удалить комментарий")
		return
	}
	if result.RowsAffected() == 0 {
		socialError(w, 404, "Комментарий не найден или недоступен")
		return
	}
	w.WriteHeader(204)
}

func (s *Server) socialStoriesHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	company := 0
	if raw := r.URL.Query().Get("company_id"); raw != "" {
		id, e := strconv.Atoi(raw)
		if e != nil || id <= 0 {
			socialError(w, 400, "Некорректная компания")
			return
		}
		company = id
	}
	rows, e := s.pool.Query(r.Context(), `SELECT x.id,x.company_id,c.name,COALESCE(c.logo_url,''),x.caption,x.media_type,x.created_at,x.expires_at,x.company_id=$2 FROM company_stories x JOIN companies c ON c.id=x.company_id WHERE x.expires_at>NOW() AND (($3::int>0 AND x.company_id=$3) OR ($3=0 AND (x.company_id=$2 OR EXISTS(SELECT 1 FROM company_subscriptions f WHERE f.user_id=$1 AND f.company_id=x.company_id)))) ORDER BY x.id DESC LIMIT 200`, u.ID, socialOwnCompany(u), company)
	if e != nil {
		socialError(w, 500, "Не удалось загрузить истории")
		return
	}
	defer rows.Close()
	items := []TimedStory{}
	for rows.Next() {
		var x TimedStory
		if e = rows.Scan(&x.ID, &x.CompanyID, &x.CompanyName, &x.CompanyLogo, &x.Caption, &x.MediaType, &x.CreatedAt, &x.ExpiresAt, &x.CanDelete); e != nil {
			socialError(w, 500, "Не удалось загрузить истории")
			return
		}
		x.MediaURL = fmt.Sprintf("/api/stories/%d/media", x.ID)
		items = append(items, x)
	}
	if rows.Err() != nil {
		socialError(w, 500, "Не удалось загрузить истории")
		return
	}
	socialJSON(w, 200, items)
}
func (s *Server) createSocialStoryHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	company := socialOwnCompany(u)
	if company == 0 {
		socialError(w, 403, "Истории публикует представитель компании")
		return
	}
	defer func() {
		if r.MultipartForm != nil {
			_ = r.MultipartForm.RemoveAll()
		}
	}()
	body, _, ok := s.socialInput(w, r)
	if !ok {
		return
	}
	if !utf8.ValidString(body) || utf8.RuneCountInString(body) > 500 {
		socialError(w, 400, "Подпись — до 500 символов")
		return
	}
	url, kind, e := s.socialUpload(r, fmt.Sprintf("story_%d", company))
	if e != nil {
		socialError(w, 400, e.Error())
		return
	}
	if url == "" {
		socialError(w, 400, "Добавьте фото или видео истории")
		return
	}
	privateDir := filepath.Join("uploads", ".stories")
	if e = os.MkdirAll(privateDir, 0755); e == nil {
		e = os.Rename(filepath.Join("uploads", filepath.Base(url)), filepath.Join(privateDir, filepath.Base(url)))
	}
	if e != nil {
		removeCompanyUpload(url)
		socialError(w, 500, "Не удалось сохранить историю")
		return
	}
	committed := false
	defer func() {
		if !committed {
			_ = os.Remove(filepath.Join(privateDir, filepath.Base(url)))
		}
	}()
	tx, e := s.pool.Begin(r.Context())
	if e != nil {
		socialError(w, 500, "Не удалось опубликовать историю")
		return
	}
	defer tx.Rollback(r.Context())
	var count int
	if e = tx.QueryRow(r.Context(), `SELECT id FROM companies WHERE id=$1 FOR UPDATE`, company).Scan(new(int)); e == nil {
		e = tx.QueryRow(r.Context(), `SELECT COUNT(*) FROM company_stories WHERE company_id=$1 AND expires_at>NOW()`, company).Scan(&count)
	}
	if e != nil {
		socialError(w, 500, "Не удалось опубликовать историю")
		return
	}
	if count >= 20 {
		socialError(w, 409, "У компании уже 20 активных историй")
		return
	}
	var id int64
	e = tx.QueryRow(r.Context(), `INSERT INTO company_stories(company_id,user_id,caption,media_url,media_type) VALUES($1,$2,$3,$4,$5) RETURNING id`, company, u.ID, body, url, kind).Scan(&id)
	if e == nil {
		e = tx.Commit(r.Context())
	}
	if e != nil {
		socialError(w, 500, "Не удалось опубликовать историю")
		return
	}
	committed = true
	socialJSON(w, 201, map[string]any{"id": id})
}
func (s *Server) deleteSocialStoryHandler(w http.ResponseWriter, r *http.Request) {
	u, ok := s.socialUser(w, r)
	if !ok {
		return
	}
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	var url string
	e := s.pool.QueryRow(r.Context(), `DELETE FROM company_stories WHERE id=$1 AND company_id=$2 RETURNING media_url`, id, socialOwnCompany(u)).Scan(&url)
	if errors.Is(e, pgx.ErrNoRows) {
		socialError(w, 404, "История не найдена или недоступна")
		return
	}
	if e != nil {
		socialError(w, 500, "Не удалось удалить историю")
		return
	}
	_ = os.Remove(filepath.Join("uploads", ".stories", filepath.Base(url)))
	w.WriteHeader(204)
}
func (s *Server) socialStoryMediaHandler(w http.ResponseWriter, r *http.Request) {
	id, ok := socialID(w, r)
	if !ok {
		return
	}
	var url string
	e := s.pool.QueryRow(r.Context(), `SELECT media_url FROM company_stories WHERE id=$1 AND expires_at>NOW()`, id).Scan(&url)
	if errors.Is(e, pgx.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if e != nil {
		socialError(w, 500, "Не удалось загрузить историю")
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	http.ServeFile(w, r, filepath.Join("uploads", ".stories", filepath.Base(url)))
}
func (s *Server) runStoryCleanup(ctx context.Context) {
	ticker := time.NewTicker(time.Hour)
	defer ticker.Stop()
	for {
		s.cleanupStories(ctx)
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}
func (s *Server) cleanupStories(ctx context.Context) {
	rows, e := s.pool.Query(ctx, `DELETE FROM company_stories WHERE expires_at<=NOW() RETURNING media_url`)
	if e != nil {
		return
	}
	defer rows.Close()
	for rows.Next() {
		var url string
		if rows.Scan(&url) == nil {
			_ = os.Remove(filepath.Join("uploads", ".stories", filepath.Base(url)))
		}
	}
}
