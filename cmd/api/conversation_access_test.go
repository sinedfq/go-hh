package main

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestConversationAccess(t *testing.T) {
	for _, tc := range []struct {
		name, id      string
		user, allowed bool
		err           error
		status        int
	}{
		{"anonymous", "12", false, false, nil, 401},
		{"invalid", "oops", true, false, nil, 400},
		{"negative", "-1", true, false, nil, 400},
		{"outsider", "12", true, false, nil, 403},
		{"database failure", "12", true, false, errors.New("db"), 500},
		{"participant", "12", true, true, nil, 204},
	} {
		t.Run(tc.name, func(t *testing.T) {
			called := false
			handler := requireConversationAccess(func(_ context.Context, conversationID, userID int) (bool, error) {
				if conversationID != 12 || userID != 7 {
					t.Fatal("wrong membership arguments")
				}
				return tc.allowed, tc.err
			}, func(w http.ResponseWriter, r *http.Request) { called = true; w.WriteHeader(204) })
			r := httptest.NewRequest("GET", "/api/conversations/"+tc.id+"/messages", nil)
			r.SetPathValue("id", tc.id)
			if tc.user {
				r = r.WithContext(context.WithValue(r.Context(), userContextKey, &Claims{UserID: 7}))
			}
			w := httptest.NewRecorder()
			handler(w, r)
			if w.Code != tc.status {
				t.Fatalf("status %d, want %d", w.Code, tc.status)
			}
			if called != (tc.status == 204) {
				t.Fatal("protected handler called without access")
			}
		})
	}
}
