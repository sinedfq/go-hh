package main

import (
	"bytes"
	"fmt"
	"golang.org/x/image/draw"
	_ "golang.org/x/image/webp"
	"image"
	_ "image/jpeg"
	"image/png"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"unicode"

	"github.com/signintech/gopdf"
	"golang.org/x/image/font/gofont/gobold"
	"golang.org/x/image/font/gofont/goregular"
)

// Only the signed-in owner can download their resume, including private contacts.
func (s *Server) downloadMyResumeHandler(w http.ResponseWriter, r *http.Request) {
	claims := getUserFromContext(r.Context())
	if claims == nil {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}
	resume, err := s.storage.GetResumeByUserID(r.Context(), claims.UserID)
	if err != nil {
		if err.Error() == "resume not found" {
			http.Error(w, "resume not found", 404)
		} else {
			http.Error(w, "failed to load resume", 500)
		}
		return
	}
	data, err := buildResumePDF(resume)
	if err != nil {
		http.Error(w, "failed to export resume", 500)
		return
	}
	w.Header().Set("Content-Type", "application/pdf")
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="trudomix-resume-%d.pdf"`, resume.ID))
	w.Header().Set("Cache-Control", "private, no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Write(data)
}

// Embed open Go fonts so Cyrillic does not depend on installed system fonts.
func buildResumePDF(resume Resume) ([]byte, error) {
	var pdf gopdf.GoPdf
	pdf.Start(gopdf.Config{PageSize: *gopdf.PageSizeA4})
	if err := pdf.AddTTFFontByReader("regular", bytes.NewReader(goregular.TTF)); err != nil {
		return nil, err
	}
	if err := pdf.AddTTFFontByReader("bold", bytes.NewReader(gobold.TTF)); err != nil {
		return nil, err
	}
	const left, width, bottom = 204.0, 347.0, 787.0
	y := 48.0
	page := 0
	resume.summarizeExperience()
	photo := resumePDFPhoto(resume.PhotoURL)
	newPage := func() {
		pdf.AddPage()
		page++
		y = 48
		pdf.SetFillColor(236, 242, 250)
		pdf.RectFromUpperLeftWithStyle(0, 0, 174, 842, "F")
		pdf.SetFillColor(0, 102, 204)
		pdf.RectFromUpperLeftWithStyle(0, 0, 6, 842, "F")
		pdf.SetFont("bold", "", 12)
		pdf.SetTextColor(0, 102, 204)
		pdf.SetXY(28, 32)
		pdf.Cell(nil, "TRUDOMIX")
		if page == 1 {
			if len(photo) > 0 {
				holder, e := gopdf.ImageHolderByBytes(photo)
				if e == nil {
					pdf.ImageByHolder(holder, 28, 72, &gopdf.Rect{W: 118, H: 118})
				}
			}
			if len(photo) == 0 {
				pdf.SetFillColor(214, 227, 245)
				pdf.RectFromUpperLeftWithStyle(28, 72, 118, 118, "F")
				initials := ""
				for _, part := range strings.Fields(resume.FullName) {
					if len([]rune(initials)) < 2 {
						initials += string([]rune(part)[0])
					}
				}
				pdf.SetFont("bold", "", 30)
				pdf.SetXY(58, 112)
				pdf.Cell(nil, initials)
			}
			pdf.SetFont("regular", "", 9)
			pdf.SetTextColor(83, 103, 130)
			pdf.SetXY(28, 210)
			pdf.Cell(nil, "ПРОФЕССИОНАЛЬНЫЙ")
			pdf.SetXY(28, 224)
			pdf.Cell(nil, "ПРОФИЛЬ")
		}
		pdf.SetFillColor(0, 102, 204)
		pdf.RectFromUpperLeftWithStyle(left, 28, 42, 3, "F")
		pdf.SetFont("regular", "", 9)
		pdf.SetTextColor(100, 110, 128)
		pdf.SetXY(28, 810)
		pdf.Cell(nil, fmt.Sprintf("Резюме · %d", page))
	}
	newPage()
	var renderErr error
	write := func(text string, size int, bold bool) {
		if strings.TrimSpace(text) == "" || renderErr != nil {
			return
		}
		font := "regular"
		if bold {
			font = "bold"
		}
		if err := pdf.SetFont(font, "", size); err != nil {
			renderErr = err
			return
		}
		pdf.SetTextColor(30, 42, 62)
		height := float64(size) * 1.5
		// Wrap by actual glyph width, including a single long URL or skill name.
		for _, paragraph := range strings.Split(strings.ReplaceAll(text, "\r\n", "\n"), "\n") {
			line := []rune{}
			flush := func() {
				if y+height > bottom {
					newPage()
					pdf.SetFont(font, "", size)
					pdf.SetTextColor(30, 42, 62)
				}
				pdf.SetXY(left, y)
				if err := pdf.Cell(nil, string(line)); err != nil {
					renderErr = err
				}
				y += height
			}
			for _, ch := range paragraph {
				if unicode.IsControl(ch) {
					ch = ' '
				}
				candidate := append(append([]rune{}, line...), ch)
				measure, err := pdf.MeasureTextWidth(string(candidate))
				if err != nil {
					renderErr = err
					return
				}
				if measure > width && len(line) > 0 {
					cut := -1
					for i := len(line) - 1; i >= 0; i-- {
						if unicode.IsSpace(line[i]) {
							cut = i
							break
						}
					}
					if cut > 0 {
						rest := append([]rune{}, line[cut+1:]...)
						line = line[:cut]
						flush()
						line = append(rest, ch)
					} else {
						flush()
						line = []rune{ch}
					}
				} else {
					line = candidate
				}
			}
			flush()
		}
		y += 7
	}
	section := func(title, text string) {
		if strings.TrimSpace(text) != "" {
			if y+70 > bottom {
				newPage()
			}
			write(title, 13, true)
			write(text, 11, false)
		}
	}
	write(resume.FullName, 22, true)
	write(resume.DesiredPosition, 15, true)
	place := resume.City
	if resume.Remote {
		if place != "" {
			place += " · "
		}
		place += "Готов к удалённой работе"
	}
	write(place, 11, false)
	section("Опыт / грейд", resume.Experience)
	section("Общий опыт работы", resume.TotalExperience)
	contacts := []string{}
	for _, entry := range []struct{ label, value string }{{"Телефон", resume.Phone}, {"Telegram", resume.Telegram}, {"GitHub", resume.GitHub}, {"LinkedIn", resume.LinkedIn}} {
		if strings.TrimSpace(entry.value) != "" {
			contacts = append(contacts, entry.label+": "+entry.value)
		}
	}
	section("Контакты", strings.Join(contacts, "\n"))
	section("Навыки", strings.Join(resume.Skills, " · "))
	section("О себе", resume.About)
	if len(resume.WorkExperience) > 0 {
		write("Опыт работы", 15, true)
	}
	for _, job := range resume.WorkExperience {
		if y+90 > bottom {
			newPage()
		}
		write(job.Position+" · "+job.Company, 13, true)
		end := "по настоящее время"
		if job.EndDate != nil {
			end = job.EndDate.Format("01.2006")
		}
		write(job.StartDate.Format("01.2006")+" — "+end, 10, false)
		write(job.Description, 11, false)
	}
	if renderErr != nil {
		return nil, renderErr
	}
	return pdf.GetBytesPdfReturnErr()
}

// Read only locally uploaded avatars, never arbitrary paths or remote URLs.
func resumePDFPhoto(raw string) []byte {
	u, err := url.Parse(raw)
	if err != nil || u.Host != "" || !strings.HasPrefix(u.Path, "/uploads/") {
		return nil
	}
	name := strings.TrimPrefix(u.Path, "/uploads/")
	if name == "" || filepath.Base(name) != name || strings.ContainsAny(name, "/\\") {
		return nil
	}
	data, err := os.ReadFile(filepath.Join("uploads", name))
	if err != nil || len(data) > 4<<20 {
		return nil
	}
	config, _, err := image.DecodeConfig(bytes.NewReader(data))
	if err != nil || config.Width < 1 || config.Height < 1 || int64(config.Width)*int64(config.Height) > 40000000 {
		return nil
	}
	source, _, err := image.Decode(bytes.NewReader(data))
	if err != nil {
		return nil
	}
	b := source.Bounds()
	side := b.Dx()
	if b.Dy() < side {
		side = b.Dy()
	}
	crop := image.Rect(b.Min.X+(b.Dx()-side)/2, b.Min.Y+(b.Dy()-side)/2, b.Min.X+(b.Dx()+side)/2, b.Min.Y+(b.Dy()+side)/2)
	avatar := image.NewRGBA(image.Rect(0, 0, 320, 320))
	draw.ApproxBiLinear.Scale(avatar, avatar.Bounds(), source, crop, draw.Src, nil)
	var out bytes.Buffer
	if png.Encode(&out, avatar) != nil {
		return nil
	}
	return out.Bytes()
}
