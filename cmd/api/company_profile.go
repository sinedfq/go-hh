package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
)

type CompanyProfile struct {
	ITAccredited  bool     `json:"it_accredited"`
	TeamStory     string   `json:"team_story"`
	Benefits      []string `json:"benefits"`
	HiringProcess string   `json:"hiring_process"`
	Photos        []string `json:"photos"`
	Videos        []string `json:"videos"`
}

func (s *Server) updateCompanyProfileHandler(w http.ResponseWriter, r *http.Request) {
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
	if user.CompanyID == nil {
		http.Error(w, "company not found", 404)
		return
	}
	if user.Role != "employer" && user.Role != "admin" {
		http.Error(w, "forbidden", 403)
		return
	}
	var req CreateCompanyRequest
	if err = json.NewDecoder(http.MaxBytesReader(w, r.Body, 64<<10)).Decode(&req); err != nil {
		http.Error(w, "invalid json", 400)
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	if req.Name == "" || len(req.Name) > 200 || len(req.Description) > 10000 || len(req.TeamStory) > 10000 || len(req.HiringProcess) > 10000 || len(req.Benefits) > 20 {
		http.Error(w, "invalid company profile", 400)
		return
	}
	for _, benefit := range req.Benefits {
		if len(benefit) > 250 {
			http.Error(w, "benefit too long", 400)
			return
		}
	}
	if req.Website != "" {
		u, e := url.Parse(req.Website)
		if e != nil || u.Host == "" || (u.Scheme != "http" && u.Scheme != "https") {
			http.Error(w, "invalid website", 400)
			return
		}
	}
	// Uploaded photos are managed separately; editing text cannot replace the gallery.
	data, _ := json.Marshal(map[string]interface{}{"team_story": req.TeamStory, "benefits": req.Benefits, "hiring_process": req.HiringProcess, "it_accredited": req.ITAccredited})
	tx, err := s.pool.Begin(r.Context())
	if err != nil {
		http.Error(w, "failed to update company", 500)
		return
	}
	defer tx.Rollback(r.Context())
	result, err := tx.Exec(r.Context(), `UPDATE companies SET name=$1,description=$2,industry=$3,size=$4,city=$5,website=$6,profile=profile||$7::jsonb WHERE id=$8`, req.Name, req.Description, req.Industry, req.Size, req.City, req.Website, data, *user.CompanyID)
	if err != nil || result.RowsAffected() != 1 {
		http.Error(w, "failed to update company", 500)
		return
	}
	if _, err = tx.Exec(r.Context(), `UPDATE vacancies SET company=$1 WHERE company_id=$2`, req.Name, *user.CompanyID); err != nil {
		http.Error(w, "failed to update company", 500)
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		http.Error(w, "failed to update company", 500)
		return
	}
	company, err := s.storage.GetCompanyByID(r.Context(), *user.CompanyID)
	if err != nil {
		http.Error(w, "failed to get company", 500)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(company)
}
func (s *Server) companyGalleryHandler(w http.ResponseWriter, r *http.Request) {
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
	if user.CompanyID == nil {
		http.Error(w, "company not found", 404)
		return
	}
	if user.Role != "employer" && user.Role != "admin" {
		http.Error(w, "forbidden", 403)
		return
	}
	if r.Method == http.MethodDelete {
		var req struct {
			URL string `json:"url"`
		}
		if json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&req) != nil {
			http.Error(w, "invalid json", 400)
			return
		}
		_, err = s.pool.Exec(r.Context(), `UPDATE companies SET profile=jsonb_set(jsonb_set(profile,'{photos}',COALESCE((SELECT jsonb_agg(item) FROM jsonb_array_elements(COALESCE(profile->'photos','[]')) item WHERE item<>to_jsonb($1::text)),'[]')),'{videos}',COALESCE((SELECT jsonb_agg(item) FROM jsonb_array_elements(COALESCE(profile->'videos','[]')) item WHERE item<>to_jsonb($1::text)),'[]')) WHERE id=$2`, req.URL, *user.CompanyID)
	} else {
		r.Body = http.MaxBytesReader(w, r.Body, 52<<20)
		if err := r.ParseMultipartForm(5 << 20); err != nil {
			http.Error(w, "media too large or invalid multipart", 400)
			return
		}
		defer r.MultipartForm.RemoveAll()
		video := len(r.MultipartForm.File["video"]) > 0
		company, e := s.storage.GetCompanyByID(r.Context(), *user.CompanyID)
		if e != nil {
			http.Error(w, "company not found", 404)
			return
		}
		if (!video && len(company.Photos) >= 6) || (video && len(company.Videos) >= 4) {
			http.Error(w, "gallery limit reached", 409)
			return
		}
		var photo string
		if video {
			photo, e = saveUploadedCompanyVideo(r, fmt.Sprintf("company_%d_gallery", *user.CompanyID))
		} else {
			photo, e = s.saveUploadedPhoto(r, fmt.Sprintf("company_%d_gallery", *user.CompanyID))
		}
		if e != nil {
			http.Error(w, e.Error(), 400)
			return
		}
		key, limit := "photos", 6
		if video {
			key, limit = "videos", 4
		}
		result, e := s.pool.Exec(r.Context(), `UPDATE companies SET profile=jsonb_set(profile,ARRAY[$3::text],COALESCE(profile->$3::text,'[]')||jsonb_build_array($1::text)) WHERE id=$2 AND jsonb_array_length(COALESCE(profile->$3::text,'[]'))<$4`, photo, *user.CompanyID, key, limit)
		if e != nil || result.RowsAffected() != 1 {
			removeCompanyUpload(photo)
			http.Error(w, "failed to add photo", 409)
			return
		}
	}
	if err != nil {
		http.Error(w, "failed to update gallery", 500)
		return
	}
	company, err := s.storage.GetCompanyByID(r.Context(), *user.CompanyID)
	if err != nil {
		http.Error(w, "failed to get company", 500)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(company)
}
