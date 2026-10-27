package phoneotp

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/url"
	"time"
)

type Sender interface {
	SendCode(context.Context, string, string, time.Duration) error
}

// HTTPSender targets an explicitly configured SMS gateway adapter, not a vendor-specific API.
type HTTPSender struct {
	endpoint, token string
	client          *http.Client
}

func NewHTTPSender(endpoint, token string, allowLocalHTTP bool) (*HTTPSender, error) {
	u, err := url.Parse(endpoint)
	if err != nil || u.Host == "" || u.User != nil || u.RawQuery != "" || u.Fragment != "" {
		return nil, errors.New("invalid SMS gateway URL")
	}
	local := u.Hostname() == "localhost" || u.Hostname() == "127.0.0.1" || u.Hostname() == "::1"
	if u.Scheme != "https" && !(allowLocalHTTP && local && u.Scheme == "http") {
		return nil, errors.New("SMS gateway requires HTTPS")
	}
	return &HTTPSender{endpoint: endpoint, token: token, client: &http.Client{Timeout: 8 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}}, nil
}
func (s *HTTPSender) SendCode(ctx context.Context, phone, code string, ttl time.Duration) error {
	body, _ := json.Marshal(struct {
		Phone   string `json:"phone"`
		Code    string `json:"code"`
		Expires int    `json:"expires_in"`
	}{phone, code, int(ttl.Seconds())})
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, s.endpoint, bytes.NewReader(body))
	if err != nil {
		return errors.New("SMS delivery failed")
	}
	req.Header.Set("Content-Type", "application/json")
	if s.token != "" {
		req.Header.Set("Authorization", "Bearer "+s.token)
	}
	response, err := s.client.Do(req)
	if err != nil {
		return errors.New("SMS delivery failed")
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return errors.New("SMS delivery failed")
	}
	return nil
}

type TestSender struct{ allowed map[string]bool }

func NewTestSender(phones []string) *TestSender {
	m := make(map[string]bool)
	for _, p := range phones {
		m[p] = true
	}
	return &TestSender{allowed: m}
}
func (s *TestSender) SendCode(_ context.Context, phone, _ string, _ time.Duration) error {
	if !s.allowed[phone] {
		return ErrTestPhone
	}
	return nil
}
