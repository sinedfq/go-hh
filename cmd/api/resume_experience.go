package main

import (
	"fmt"
	"sort"
	"strings"
	"time"
)

// Parallel jobs describe one period of employment and must not double the total.
func resumeExperienceMonths(jobs []WorkExperience, now time.Time) (int, bool) {
	type interval struct{ start, end time.Time }
	periods := []interval{}
	for _, job := range jobs {
		end := now
		if job.EndDate != nil && job.EndDate.Before(end) {
			end = *job.EndDate
		}
		start := job.StartDate
		if start.IsZero() || !end.After(start) {
			continue
		}
		periods = append(periods, interval{start, end})
	}
	if len(periods) == 0 {
		return 0, false
	}
	sort.Slice(periods, func(i, j int) bool { return periods[i].start.Before(periods[j].start) })
	merged := []interval{periods[0]}
	for _, p := range periods[1:] {
		last := &merged[len(merged)-1]
		if !p.start.After(last.end) {
			if p.end.After(last.end) {
				last.end = p.end
			}
		} else {
			merged = append(merged, p)
		}
	}
	months := 0
	remainingDays := 0
	for _, p := range merged {
		m := (p.end.Year()-p.start.Year())*12 + int(p.end.Month()-p.start.Month())
		if p.end.Day() < p.start.Day() {
			m--
		}
		if m > 0 {
			months += m
		}
		if m < 0 {
			m = 0
		}
		remainingDays += int(p.end.Sub(p.start.AddDate(0, m, 0)).Hours() / 24)
	}
	// Short, non-overlapping jobs also contribute to the total (30 days/month).
	return months + remainingDays/30, true
}

func pluralRU(n int, one, few, many string) string {
	if n%100 >= 11 && n%100 <= 14 {
		return many
	}
	switch n % 10 {
	case 1:
		return one
	case 2, 3, 4:
		return few
	}
	return many
}
func experienceLabel(months int, hasJobs bool) string {
	if !hasJobs {
		return "Не указан"
	}
	if months == 0 {
		return "Менее месяца"
	}
	parts := []string{}
	years := months / 12
	rest := months % 12
	if years > 0 {
		parts = append(parts, fmt.Sprintf("%d %s", years, pluralRU(years, "год", "года", "лет")))
	}
	if rest > 0 {
		parts = append(parts, fmt.Sprintf("%d %s", rest, pluralRU(rest, "месяц", "месяца", "месяцев")))
	}
	return strings.Join(parts, " ")
}
func (r *Resume) summarizeExperience() {
	months, hasJobs := resumeExperienceMonths(r.WorkExperience, time.Now())
	r.TotalExperienceMonths = months
	r.TotalExperience = experienceLabel(months, hasJobs)
}
