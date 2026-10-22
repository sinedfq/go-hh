package main

import (
	"bytes"
	"mime/multipart"
	"net/http/httptest"
	"os"
	"testing"
)

func TestCompanyVideoRejectsOversizedHeader(t *testing.T) {
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	part, _ := writer.CreateFormFile("video", "oversized.mp4")
	part.Write([]byte("small placeholder"))
	writer.Close()
	request := httptest.NewRequest("POST", "/", &body)
	request.Header.Set("Content-Type", writer.FormDataContentType())
	if err := request.ParseMultipartForm(1024); err != nil {
		t.Fatal(err)
	}
	defer request.MultipartForm.RemoveAll()
	request.MultipartForm.File["video"][0].Size = (50 << 20) + 1
	t.Chdir(t.TempDir())
	if _, err := saveUploadedCompanyVideo(request, "test"); err == nil {
		t.Fatal("oversized video accepted")
	}
	if _, err := os.Stat("uploads"); !os.IsNotExist(err) {
		t.Fatal("rejected video created storage")
	}
}
