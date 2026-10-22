// push-check sends a diagnostic event to an explicitly supplied developer device.
package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/sinedfq/go-hh/internal/push"
)

func main() {
	tokenFile := flag.String("token-file", "", "File containing the FCM token of your own test device")
	flag.Parse()
	if *tokenFile == "" || os.Getenv("FIREBASE_PROJECT_ID") == "" {
		fmt.Fprintln(os.Stderr, "Set FIREBASE_PROJECT_ID, GOOGLE_APPLICATION_CREDENTIALS and -token-file for your own test device")
		os.Exit(1)
	}
	contents, err := os.ReadFile(*tokenFile)
	if err != nil {
		fmt.Fprintln(os.Stderr, "Cannot read the device token file")
		os.Exit(1)
	}
	token := strings.TrimSpace(string(contents))
	if token == "" || len(token) > 4096 {
		fmt.Fprintln(os.Stderr, "Invalid device token file")
		os.Exit(1)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	sender, err := push.New(ctx, os.Getenv("FIREBASE_PROJECT_ID"))
	if err == nil {
		err = sender.Send(ctx, token, "Проверка Firebase Trudomix", "Тестовое сообщение пришло с Go-сервера через Firebase.", map[string]string{"type": "push_test"})
	}
	if err != nil {
		fmt.Fprintln(os.Stderr, "Firebase diagnostic failed:", err)
		os.Exit(1)
	}
	fmt.Println("Firebase accepted the diagnostic message. Check the test device.")
}
