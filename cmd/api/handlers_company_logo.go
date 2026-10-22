package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
)

// The target company is taken from the authenticated user, never from the request.
func removeCompanyUpload(path string) { _ = os.Remove(filepath.Join("uploads", filepath.Base(path))) }
func (s *Server) uploadCompanyLogoHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	fail := func(status int, message string) {
		w.WriteHeader(status)
		_ = json.NewEncoder(w).Encode(map[string]string{"error": message})
	}
	claims := getUserFromContext(r.Context())
	if claims == nil {
		fail(http.StatusUnauthorized, "unauthorized")
		return
	}
	user, err := s.storage.GetUserByID(r.Context(), claims.UserID)
	if err != nil {
		fail(http.StatusInternalServerError, "failed to get user")
		return
	}
	if user.Role != "employer" && user.Role != "admin" {
		fail(http.StatusForbidden, "employer required")
		return
	}
	if user.CompanyID == nil {
		fail(http.StatusNotFound, "company not found")
		return
	}
	photoURL, err := s.saveUploadedPhoto(r, fmt.Sprintf("company_%d", *user.CompanyID))
	if r.MultipartForm != nil {
		defer r.MultipartForm.RemoveAll()
	}
	if err != nil {
		fail(http.StatusBadRequest, err.Error())
		return
	}
	if err := s.storage.UpdateCompanyLogo(r.Context(), *user.CompanyID, user.ID, photoURL); err != nil {
		_ = os.Remove(filepath.Join("uploads", filepath.Base(photoURL)))
		fail(http.StatusInternalServerError, "failed to update company")
		return
	}
	company, err := s.storage.GetCompanyByID(r.Context(), *user.CompanyID)
	if err != nil {
		fail(http.StatusInternalServerError, "failed to get company")
		return
	}
	_ = json.NewEncoder(w).Encode(company)
}
