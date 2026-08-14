package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"time"
)

func (s *Server) uploadUserPhotoHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		log.Printf("❌ uploadUserPhoto: unauthorized")
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	log.Printf("📤 uploadUserPhoto: user_id=%d", claims.UserID)

	photoURL, err := s.saveUploadedPhoto(r, fmt.Sprintf("user_%d", claims.UserID))
	if err != nil {
		log.Printf("❌ saveUploadedPhoto error: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	log.Printf("✅ Photo saved: %s", photoURL)

	if err := s.storage.UpdateUserPhoto(ctx, claims.UserID, photoURL); err != nil {
		log.Printf("❌ UpdateUserPhoto error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update user"})
		return
	}

	user, _ := s.storage.GetUserByID(ctx, claims.UserID)
	log.Printf("✅ User updated, returning: %+v", user.PhotoURL)
	json.NewEncoder(w).Encode(user)
}

func (s *Server) uploadResumePhotoHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	ctx := r.Context()

	claims := getUserFromContext(ctx)
	if claims == nil {
		log.Printf("❌ uploadResumePhoto: unauthorized")
		w.WriteHeader(http.StatusUnauthorized)
		json.NewEncoder(w).Encode(map[string]string{"error": "unauthorized"})
		return
	}

	log.Printf("📤 uploadResumePhoto: user_id=%d", claims.UserID)

	resume, err := s.storage.GetResumeByUserID(ctx, claims.UserID)
	if err != nil {
		log.Printf("❌ GetResumeByUserID error: %v", err)
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "resume not found"})
		return
	}

	photoURL, err := s.saveUploadedPhoto(r, fmt.Sprintf("resume_%d", resume.ID))
	if err != nil {
		log.Printf("❌ saveUploadedPhoto error: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	log.Printf("✅ Photo saved: %s", photoURL)

	if err := s.storage.UpdateResumePhoto(ctx, resume.ID, claims.UserID, photoURL); err != nil {
		log.Printf("❌ UpdateResumePhoto error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "failed to update resume"})
		return
	}

	updatedResume, _ := s.storage.GetResumeByUserID(ctx, claims.UserID)
	log.Printf("✅ Resume updated, returning: %+v", updatedResume.PhotoURL)
	json.NewEncoder(w).Encode(updatedResume)
}

func (s *Server) saveUploadedPhoto(r *http.Request, prefix string) (string, error) {
	r.Body = http.MaxBytesReader(nil, r.Body, 5<<20)

	if err := r.ParseMultipartForm(5 << 20); err != nil {
		log.Printf("❌ ParseMultipartForm error: %v", err)
		return "", fmt.Errorf("file too large or invalid format")
	}

	file, header, err := r.FormFile("photo")
	if err != nil {
		log.Printf("❌ FormFile error: %v", err)
		return "", fmt.Errorf("photo field required")
	}
	defer file.Close()

	log.Printf("📎 Received file: %s, size=%d, content-type=%s",
		header.Filename, header.Size, header.Header.Get("Content-Type"))

	contentType := header.Header.Get("Content-Type")
	allowedTypes := map[string]string{
		"image/jpeg": ".jpg",
		"image/png":  ".png",
		"image/webp": ".webp",
	}
	ext, ok := allowedTypes[contentType]
	if !ok {
		log.Printf("❌ Unsupported content type: %s", contentType)
		return "", fmt.Errorf("only jpg, png, webp allowed (got: %s)", contentType)
	}

	filename := fmt.Sprintf("%s_%d%s", prefix, time.Now().UnixNano(), ext)

	if err := os.MkdirAll("uploads", 0755); err != nil {
		log.Printf("❌ MkdirAll error: %v", err)
		return "", fmt.Errorf("failed to create uploads dir")
	}

	filePath := filepath.Join("uploads", filename)
	dst, err := os.Create(filePath)
	if err != nil {
		log.Printf("❌ Create file error: %v", err)
		return "", fmt.Errorf("failed to create file")
	}
	defer dst.Close()

	written, err := io.Copy(dst, file)
	if err != nil {
		log.Printf("❌ Copy error: %v", err)
		return "", fmt.Errorf("failed to write file")
	}

	log.Printf("✅ File saved to %s (%d bytes)", filePath, written)

	return "/uploads/" + filename, nil
}
