package main

import (
	"context"
	"encoding/json"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestDevicePushStatusDisabledAndValidation(t *testing.T) {
	for _, tc := range []struct {
		name, body    string
		authenticated bool
		status        int
	}{
		{"anonymous", `{}`, false, 401},
		{"invalid JSON", `{`, true, 400},
		{"oversized token", `{"token":"` + strings.Repeat("x", 4097) + `"}`, true, 400},
		{"not configured", `{"token":""}`, true, 200},
	} {
		t.Run(tc.name, func(t *testing.T) {
			r := httptest.NewRequest("POST", "/api/devices/status", strings.NewReader(tc.body))
			if tc.authenticated {
				r = r.WithContext(context.WithValue(r.Context(), userContextKey, &Claims{UserID: 7}))
			}
			w := httptest.NewRecorder()
			(&Server{}).devicePushStatusHandler(w, r)
			if w.Code != tc.status {
				t.Fatalf("status %d, want %d", w.Code, tc.status)
			}
			if tc.status == 200 {
				var result map[string]bool
				if err := json.Unmarshal(w.Body.Bytes(), &result); err != nil {
					t.Fatal(err)
				}
				if len(result) != 2 || result["server_enabled"] || result["registered"] {
					t.Fatalf("unexpected readiness: %v", result)
				}
			}
		})
	}
}

func TestDevicePushDiagnosticValidation(t *testing.T) {
	for _, tc := range []struct {
		name, body    string
		authenticated bool
		status        int
	}{
		{"anonymous", `{}`, false, 401},
		{"invalid JSON", `{`, true, 400},
		{"empty token", `{"token":""}`, true, 400},
		{"oversized token", `{"token":"` + strings.Repeat("x", 4097) + `"}`, true, 400},
		{"not configured", `{"token":"owned-device"}`, true, 503},
	} {
		t.Run(tc.name, func(t *testing.T) {
			r := httptest.NewRequest("POST", "/api/devices/test", strings.NewReader(tc.body))
			if tc.authenticated {
				r = r.WithContext(context.WithValue(r.Context(), userContextKey, &Claims{UserID: 7}))
			}
			w := httptest.NewRecorder()
			(&Server{}).testDevicePushHandler(w, r)
			if w.Code != tc.status {
				t.Fatalf("status %d, want %d", w.Code, tc.status)
			}
		})
	}
}
