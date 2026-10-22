package main

import (
	"encoding/json"
	"fmt"
	"net/http"
)

// Upload before saving a draft; its Markdown stores the returned relative URL.
func (s *Server) uploadVacancyImageHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", 401)
		return
	}
	user, err := s.storage.GetUserByID(r.Context(), claims.UserID)
	if err != nil {
		http.Error(w, "failed to get user", 500)
		return
	}
	if user.CompanyID == nil || (user.Role != "employer" && user.Role != "admin") {
		http.Error(w, "forbidden", 403)
		return
	}
	path, err := s.saveUploadedPhoto(r, fmt.Sprintf("vacancy_company_%d", *user.CompanyID))
	if r.MultipartForm != nil {
		defer r.MultipartForm.RemoveAll()
	}
	if err != nil {
		http.Error(w, err.Error(), 400)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(201)
	json.NewEncoder(w).Encode(map[string]string{"url": path})
}
