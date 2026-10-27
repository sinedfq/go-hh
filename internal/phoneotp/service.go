package phoneotp

import (
	"context"
	"errors"
	"strings"
	"time"
)

var (
	ErrInvalid   = errors.New("invalid or expired code")
	ErrRateLimit = errors.New("request rate exceeded")
	ErrTestPhone = errors.New("phone is not enabled for testing")
	ErrDelivery  = errors.New("SMS delivery unavailable")
)

const (
	CodeTTL     = 5 * time.Minute
	ReceiptTTL  = 10 * time.Minute
	ResendAfter = time.Minute
	MaxAttempts = 5
)

type Challenge struct {
	ID, Phone, CodeHash, IPHash, ReceiptHash string
	CreatedAt, ExpiresAt                     time.Time
	Attempts                                 int
	Delivered, Verified, Consumed            bool
}

// Reserve must atomically enforce per-phone and per-IP limits and supersede older unverified codes.
// Verify must serialize attempts; a receipt is issued once. Redeem consumes it once.
type Store interface {
	Reserve(context.Context, Challenge) error
	Delivered(context.Context, string) error
	Verify(context.Context, string, string, string, time.Time) error
	Redeem(context.Context, string, string, time.Time) (string, error)
}
type Service struct {
	store      Store
	sender     Sender
	secret     []byte
	testPhones map[string]bool
	now        func() time.Time
}

func New(store Store, sender Sender, secret string, testPhones []string) (*Service, error) {
	if store == nil || sender == nil || len(secret) < 32 {
		return nil, errors.New("OTP requires storage, a sender and a secret of at least 32 bytes")
	}
	allow := make(map[string]bool)
	for _, p := range testPhones {
		n, err := NormalizePhone(p)
		if err != nil {
			return nil, err
		}
		allow[n] = true
	}
	return &Service{store: store, sender: sender, secret: []byte(secret), testPhones: allow, now: time.Now}, nil
}

type RequestResult struct {
	ChallengeID     string `json:"challenge_id"`
	ExpiresIn       int    `json:"expires_in"`
	RetryAfter      int    `json:"retry_after"`
	DevelopmentCode string `json:"development_code,omitempty"`
}
type Verification struct {
	Verified  bool   `json:"verified"`
	Receipt   string `json:"verification_receipt"`
	ExpiresIn int    `json:"expires_in"`
}

func (s *Service) Request(ctx context.Context, phone, ip string) (RequestResult, error) {
	phone, err := NormalizePhone(phone)
	if err != nil {
		return RequestResult{}, ErrInvalid
	}
	if len(s.testPhones) > 0 && !s.testPhones[phone] {
		return RequestResult{}, ErrTestPhone
	}
	id, err := randomID()
	if err != nil {
		return RequestResult{}, err
	}
	code, err := randomCode()
	if err != nil {
		return RequestResult{}, err
	}
	now := s.now().UTC()
	challenge := Challenge{ID: id, Phone: phone, CodeHash: digest(s.secret, "code", id, code), IPHash: digest(s.secret, "ip", ip), CreatedAt: now, ExpiresAt: now.Add(CodeTTL)}
	if err = s.store.Reserve(ctx, challenge); err != nil {
		return RequestResult{}, err
	}
	if err = s.sender.SendCode(ctx, phone, code, CodeTTL); err != nil {
		return RequestResult{}, ErrDelivery
	}
	if err = s.store.Delivered(ctx, id); err != nil {
		return RequestResult{}, err
	}
	result := RequestResult{ChallengeID: id, ExpiresIn: int(CodeTTL.Seconds()), RetryAfter: int(ResendAfter.Seconds())}
	if len(s.testPhones) > 0 {
		result.DevelopmentCode = code
	}
	return result, nil
}
func (s *Service) Verify(ctx context.Context, id, code string) (Verification, error) {
	if len(id) != 64 || len(code) != 6 {
		return Verification{}, ErrInvalid
	}
	for _, c := range code {
		if c < '0' || c > '9' {
			return Verification{}, ErrInvalid
		}
	}
	if _, err := hexID(id); err != nil {
		return Verification{}, ErrInvalid
	}
	receipt, err := randomID()
	if err != nil {
		return Verification{}, err
	}
	if err = s.store.Verify(ctx, id, digest(s.secret, "code", id, code), digest(s.secret, "receipt", id, receipt), s.now().UTC()); err != nil {
		return Verification{}, err
	}
	return Verification{Verified: true, Receipt: id + "." + receipt, ExpiresIn: int(ReceiptTTL.Seconds())}, nil
}
func (s *Service) Redeem(ctx context.Context, receipt string) (string, error) {
	parts := strings.Split(receipt, ".")
	if len(parts) != 2 {
		return "", ErrInvalid
	}
	for _, p := range parts {
		if _, err := hexID(p); err != nil {
			return "", ErrInvalid
		}
	}
	return s.store.Redeem(ctx, parts[0], digest(s.secret, "receipt", parts[0], parts[1]), s.now().UTC())
}
func hexID(s string) (bool, error) {
	if len(s) != 64 {
		return false, ErrInvalid
	}
	for _, c := range s {
		if !(c >= '0' && c <= '9' || c >= 'a' && c <= 'f') {
			return false, ErrInvalid
		}
	}
	return true, nil
}
