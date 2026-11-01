package push

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

type testTransport struct {
	calls int
	err   error
	token string
	data  map[string]string
}

func (s *testTransport) Send(_ context.Context, token, title, body string, data map[string]string) error {
	s.calls++
	s.token = token
	s.data = data
	return s.err
}
func TestRemoteDeliveryAndAuthentication(t *testing.T) {
	sender := &testTransport{}
	server := httptest.NewServer(Handler(sender, "private-key"))
	defer server.Close()
	client, err := NewRemote(server.URL, "private-key")
	if err != nil {
		t.Fatal(err)
	}
	if err = client.Send(context.Background(), "device", "Title", "Body", map[string]string{"type": "phone_otp"}); err != nil {
		t.Fatal(err)
	}
	if sender.calls != 1 || sender.token != "device" || sender.data["type"] != "phone_otp" {
		t.Fatal("payload was not preserved")
	}
	wrong, _ := NewRemote(server.URL, "wrong-key")
	if wrong.Send(context.Background(), "device", "", "", nil) == nil || sender.calls != 1 {
		t.Fatal("unauthorized delivery accepted")
	}
	sender.err = errInvalidRemoteToken
	if err = client.Send(context.Background(), "device", "", "", nil); !InvalidToken(err) {
		t.Fatal("invalid-device signal lost")
	}
	sender.err = errors.New("firebase unavailable")
	if err = client.Send(context.Background(), "device", "", "", nil); err == nil || InvalidToken(err) {
		t.Fatal("transient error treated as invalid token")
	}
}
func TestUnconfiguredServiceAndCredentialRedirect(t *testing.T) {
	service := httptest.NewServer(Handler(nil, "key"))
	defer service.Close()
	client, _ := NewRemote(service.URL, "key")
	if client.Send(context.Background(), "device", "", "", nil) == nil {
		t.Fatal("disabled service accepted message")
	}
	var leaked bool
	destination := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { leaked = true }))
	defer destination.Close()
	redirect := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { http.Redirect(w, r, destination.URL, 307) }))
	defer redirect.Close()
	client, _ = NewRemote(redirect.URL, "key")
	if client.Send(context.Background(), "device", "", "", nil) == nil || leaked {
		t.Fatal("redirect followed with secret payload")
	}
}
