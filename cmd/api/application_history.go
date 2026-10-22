package main

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
	"time"
)

type ApplicationUpdateRequest struct {
	Status      string  `json:"status"`
	Feedback    *string `json:"feedback"`
	InterviewAt *string `json:"interview_at"`
}
type ApplicationEvent struct {
	ID          int        `json:"id"`
	Status      string     `json:"status"`
	Feedback    string     `json:"feedback"`
	InterviewAt *time.Time `json:"interview_at"`
	CreatedAt   *time.Time `json:"created_at"`
}

func (s *Server) applicationHistoryHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", 401)
		return
	}
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil || id <= 0 {
		http.Error(w, "invalid id", 400)
		return
	}
	var allowed bool
	err = s.pool.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM applications a JOIN vacancies v ON v.id=a.vacancy_id WHERE a.id=$1 AND (a.candidate_user_id=$2 OR v.company_id IN(SELECT company_id FROM users WHERE id=$2 AND role IN ('employer','admin'))))`, id, claims.UserID).Scan(&allowed)
	if err != nil {
		http.Error(w, "failed to check access", 500)
		return
	}
	if !allowed {
		http.Error(w, "forbidden", 403)
		return
	}
	rows, err := s.pool.Query(r.Context(), `SELECT id,status,feedback,interview_at,created_at FROM application_events WHERE application_id=$1 ORDER BY id`, id)
	if err != nil {
		http.Error(w, "failed to load history", 500)
		return
	}
	defer rows.Close()
	items := []ApplicationEvent{}
	for rows.Next() {
		var item ApplicationEvent
		if err := rows.Scan(&item.ID, &item.Status, &item.Feedback, &item.InterviewAt, &item.CreatedAt); err != nil {
			http.Error(w, "failed to load history", 500)
			return
		}
		items = append(items, item)
	}
	if rows.Err() != nil {
		http.Error(w, "failed to load history", 500)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(items)
}
func (s *PostgresStorage) updateApplicationDetails(ctx context.Context, id int, req ApplicationUpdateRequest, interview *time.Time) (bool, error) {
	result, err := s.pool.Exec(ctx, `UPDATE applications SET status=$1,feedback=COALESCE($2,feedback),interview_at=CASE WHEN $3 THEN $4::timestamptz ELSE interview_at END,viewed_at=CASE WHEN viewed_at IS NULL THEN NOW() ELSE viewed_at END WHERE id=$5 AND (status,feedback,interview_at) IS DISTINCT FROM ($1,COALESCE($2,feedback),CASE WHEN $3 THEN $4::timestamptz ELSE interview_at END)`, req.Status, req.Feedback, req.InterviewAt != nil, interview, id)
	return result.RowsAffected() > 0, err
}
