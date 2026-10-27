package phoneotp

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"math/big"
	"strings"
)

func NormalizePhone(value string) (string, error) {
	var digits strings.Builder
	for i, r := range strings.TrimSpace(value) {
		switch {
		case r >= '0' && r <= '9':
			digits.WriteRune(r)
		case r == '+' && i == 0:
		case r == ' ' || r == '(' || r == ')' || r == '-':
		default:
			return "", errors.New("invalid phone")
		}
	}
	phone := digits.String()
	if len(phone) == 11 && phone[0] == '8' {
		phone = "7" + phone[1:]
	}
	if len(phone) < 8 || len(phone) > 15 || phone[0] == '0' {
		return "", errors.New("invalid phone")
	}
	if !strings.HasPrefix(strings.TrimSpace(value), "+") && !(len(phone) == 11 && phone[0] == '7') {
		return "", errors.New("country code required")
	}
	return "+" + phone, nil
}

func randomID() (string, error) {
	var bytes [32]byte
	if _, err := rand.Read(bytes[:]); err != nil {
		return "", err
	}
	return hex.EncodeToString(bytes[:]), nil
}
func randomCode() (string, error) {
	n, err := rand.Int(rand.Reader, big.NewInt(1_000_000))
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("%06d", n.Int64()), nil
}
func digest(secret []byte, parts ...string) string {
	m := hmac.New(sha256.New, secret)
	for _, p := range parts {
		m.Write([]byte(p))
		m.Write([]byte{0})
	}
	return hex.EncodeToString(m.Sum(nil))
}
func equalHash(first, second string) bool { return hmac.Equal([]byte(first), []byte(second)) }
