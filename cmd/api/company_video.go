package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
)

func saveUploadedCompanyVideo(r *http.Request, prefix string) (string, error) {
	file, header, err := r.FormFile("video")
	if err != nil {
		return "", fmt.Errorf("video field required")
	}
	defer file.Close()
	if header.Size > 50<<20 {
		return "", fmt.Errorf("video must be at most 50 MB")
	}
	var sample [512]byte
	n, err := file.Read(sample[:])
	if err != nil && err != io.EOF {
		return "", fmt.Errorf("failed to read video")
	}
	extension := map[string]string{"video/mp4": ".mp4", "video/webm": ".webm"}[http.DetectContentType(sample[:n])]
	if extension == "" {
		return "", fmt.Errorf("only mp4 and webm videos allowed")
	}
	if _, err = file.Seek(0, io.SeekStart); err != nil {
		return "", fmt.Errorf("failed to read video")
	}
	if err = os.MkdirAll("uploads", 0755); err != nil {
		return "", fmt.Errorf("failed to create uploads dir")
	}
	destination, err := os.CreateTemp("uploads", prefix+"_*"+extension)
	if err != nil {
		return "", fmt.Errorf("failed to save video")
	}
	name := destination.Name()
	success := false
	defer func() {
		destination.Close()
		if !success {
			os.Remove(name)
		}
	}()
	written, err := io.Copy(destination, io.LimitReader(file, (50<<20)+1))
	if err != nil || written > 50<<20 {
		return "", fmt.Errorf("failed to save video")
	}
	if err = destination.Close(); err != nil {
		return "", fmt.Errorf("failed to save video")
	}
	if err = os.Chmod(name, 0644); err != nil {
		return "", fmt.Errorf("failed to save video")
	}
	success = true
	return "/uploads/" + filepath.Base(name), nil
}
