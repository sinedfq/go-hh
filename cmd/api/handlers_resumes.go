package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
)

func (s *Server) resumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	idParam := r.URL.Query().Get("id")

	if idParam == "" {
		resumes, err := s.storage.GetAllResumes(ctx)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{"error": "internal error"})
			return
		}
		json.NewEncoder(w).Encode(resumes)
		return
	}

	id, err := strconv.Atoi(idParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	resume, err := s.storage.GetResumeByID(ctx, id)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	json.NewEncoder(w).Encode(resume)
}

func (s *Server) createResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	_, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err == nil {
		w.WriteHeader(http.StatusConflict)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume already exists"})
		return
	}

	var req CreateResumeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.FullName == "" || req.DesiredPosition == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "full_name and desired_position are required"})
		return
	}

	if req.Experience == "" {
		req.Experience = "Junior"
	}

	if req.Skills == nil {
		req.Skills = []string{}
	}

	resumeID, err := s.storage.CreateResume(ctx, claims.UserID, req)
	if err != nil {
		log.Printf("CreateResume error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to create resume",
			"details": err.Error(),
		})
		return
	}

	resume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	resume.ID = resumeID

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(resume)
}

func (s *Server) getMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		log.Printf("getMyResume error for user %d: %v", claims.UserID, err)
		if err.Error() == "resume not found" {
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
			return
		}
		s.recCache.Invalidate(claims.UserID)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to get resume",
			"details": err.Error(),
		})
		return
	}

	json.NewEncoder(w).Encode(resume)
}

func (s *Server) updateMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	currentResume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	var req CreateResumeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.FullName == "" || req.DesiredPosition == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "full_name and desired_position are required"})
		return
	}

	if req.Experience == "" {
		req.Experience = "Junior"
	}

	if req.Skills == nil {
		req.Skills = []string{}
	}

	if err := s.storage.UpdateResume(ctx, currentResume.ID, claims.UserID, req); err != nil {
		log.Printf("UpdateResume error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to update resume",
			"details": err.Error(),
		})
		return
	}

	s.recCache.Invalidate(claims.UserID)

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) deleteMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	if err := s.storage.DeleteResume(ctx, resume.ID, claims.UserID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to delete resume"})
		return
	}
	s.recCache.Invalidate(claims.UserID)

	json.NewEncoder(w).Encode(map[string]string{"status": "deleted"})
}

func (s *Server) viewResumeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	resumeID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	claims := getUserFromContext(ctx)
	if claims != nil {
		resume, err := s.storage.GetResumeByID(ctx, resumeID)
		if err == nil && resume.UserID == claims.UserID {
			json.NewEncoder(w).Encode(map[string]string{"status": "skipped", "reason": "self-view"})
			return
		}
	}

	if err := s.storage.IncrementResumeViews(ctx, resumeID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to increment views"})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func (s *Server) addWorkExperienceHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	var req CreateWorkExperienceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Printf("Decode error in addWorkExperience: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid json"})
		return
	}

	if req.Company == "" || req.Position == "" || req.StartDate == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "company, position and start_date are required"})
		return
	}

	_, err = s.storage.CreateWorkExperience(ctx, resume.ID, req)
	if err != nil {
		log.Printf("CreateWorkExperience error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "failed to create work experience",
			"details": err.Error(),
		})
		return
	}

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	s.recCache.Invalidate(claims.UserID)
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) deleteWorkExperienceHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	expID, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid id"})
		return
	}

	if err := s.storage.DeleteWorkExperience(ctx, expID); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to delete"})
		return
	}

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	s.recCache.Invalidate(claims.UserID)
	json.NewEncoder(w).Encode(updatedResume)
}
