package push

import (
	"bytes"
	"context"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type Transport interface {
	Send(context.Context, string, string, string, map[string]string) error
}
type payload struct {
	Token string            `json:"token"`
	Title string            `json:"title"`
	Body  string            `json:"body"`
	Data  map[string]string `json:"data"`
}

var errInvalidRemoteToken = errors.New("unregistered push token")

// Remote has a bounded timeout and never includes device tokens in error messages.
type Remote struct {
	endpoint, key string
	client        *http.Client
}

func NewRemote(endpoint, key string) (*Remote, error) {
	u, err := url.Parse(endpoint)
	if err != nil || u.Host == "" || (u.Scheme != "http" && u.Scheme != "https") || u.User != nil || u.RawQuery != "" || key == "" {
		return nil, errors.New("PUSH_SERVICE_URL and PUSH_SERVICE_TOKEN must identify a trusted internal service")
	}
	return &Remote{strings.TrimRight(endpoint, "/") + "/v1/send", key, &http.Client{Timeout: 12 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}}, nil
}
func (s *Remote) Send(ctx context.Context, token, title, body string, data map[string]string) error {
	encoded, err := json.Marshal(payload{token, title, body, data})
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, s.endpoint, bytes.NewReader(encoded))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+s.key)
	req.Header.Set("Content-Type", "application/json")
	response, err := s.client.Do(req)
	if err != nil {
		return errors.New("push service unavailable")
	}
	defer response.Body.Close()
	if response.StatusCode == http.StatusGone {
		return errInvalidRemoteToken
	}
	if response.StatusCode != http.StatusNoContent {
		return fmt.Errorf("push service returned %d", response.StatusCode)
	}
	return nil
}

// Handler has no database access. Only the API may request Firebase delivery.
func Handler(sender Transport, key string) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]any{"status": "ok", "push_enabled": sender != nil})
	})
	mux.HandleFunc("POST /v1/send", func(w http.ResponseWriter, r *http.Request) {
		if key == "" || subtle.ConstantTimeCompare([]byte(r.Header.Get("Authorization")), []byte("Bearer "+key)) != 1 {
			http.Error(w, "unauthorized", 401)
			return
		}
		if sender == nil {
			http.Error(w, "push not configured", 503)
			return
		}
		r.Body = http.MaxBytesReader(w, r.Body, 64<<10)
		var input payload
		if json.NewDecoder(r.Body).Decode(&input) != nil || input.Token == "" || len(input.Token) > 4096 {
			http.Error(w, "invalid request", 400)
			return
		}
		ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
		defer cancel()
		if err := sender.Send(ctx, input.Token, input.Title, input.Body, input.Data); err != nil {
			if InvalidToken(err) {
				http.Error(w, "unregistered device", 410)
			} else {
				http.Error(w, "delivery unavailable", 503)
			}
			return
		}
		w.WriteHeader(http.StatusNoContent)
	})
	return mux
}
