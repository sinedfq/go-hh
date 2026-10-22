package main

import (
	"bytes"
	"image"
	"image/color"
	"image/png"
	"os"
	"strings"
	"testing"
	"time"
)

func TestResumePDFUnicodeAndPagination(t *testing.T) {
	t.Chdir(t.TempDir())
	if err := os.Mkdir("uploads", 0700); err != nil {
		t.Fatal(err)
	}
	avatar := image.NewRGBA(image.Rect(0, 0, 64, 96))
	for y := 0; y < 96; y++ {
		for x := 0; x < 64; x++ {
			avatar.Set(x, y, color.RGBA{uint8(x * 3), 100, uint8(120 + y), 255})
		}
	}
	file, err := os.Create("uploads/avatar.png")
	if err != nil {
		t.Fatal(err)
	}
	if err = png.Encode(file, avatar); err != nil {
		t.Fatal(err)
	}
	file.Close()
	end := time.Date(2025, 10, 1, 0, 0, 0, 0, time.UTC)
	resume := Resume{ID: 42, FullName: "Александр Иванов", DesiredPosition: "Разработчик Go", City: "Новосибирск", Remote: true, Phone: "+7 999 123-45-67", Telegram: "@alex", Skills: []string{"Go", "PostgreSQL"}, About: strings.Repeat("Разрабатываю надёжные сервисы. ", 500), WorkExperience: []WorkExperience{{Company: "Команда", Position: "Инженер", StartDate: end, Description: "Работаю над API и мобильным приложением."}}}
	resume.PhotoURL = "/uploads/avatar.png"
	data, err := buildResumePDF(resume)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.HasPrefix(data, []byte("%PDF-")) || !bytes.Contains(data, []byte("%%EOF")) {
		t.Fatal("invalid PDF")
	}
	if bytes.Count(data, []byte("/Type /Page\n")) < 2 {
		t.Fatal("long resume should have multiple pages")
	}
	if !bytes.Contains(data, []byte("/Subtype /Image")) {
		t.Fatal("avatar missing from PDF")
	}
	if len(resumePDFPhoto("/uploads/../../private.png")) != 0 || len(resumePDFPhoto("https://example.test/avatar.png")) != 0 {
		t.Fatal("PDF must not load arbitrary images")
	}
	// Optional artifact for validating text extraction with a real PDF reader.
	if path := os.Getenv("TRUDOMIX_TEST_PDF_PATH"); path != "" {
		if err := os.WriteFile(path, data, 0600); err != nil {
			t.Fatal(err)
		}
	}
}
