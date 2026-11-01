package main

import (
	"context"
	"github.com/sinedfq/go-hh/internal/push"
	"log"
	"net/http"
	"os"
	"time"
)

func main() {
	var sender push.Transport
	if project := os.Getenv("FIREBASE_PROJECT_ID"); project != "" {
		var err error
		sender, err = push.New(context.Background(), project)
		if err != nil {
			log.Fatal("Firebase initialization failed")
		}
	}
	key := os.Getenv("PUSH_SERVICE_TOKEN")
	if key == "" {
		log.Fatal("PUSH_SERVICE_TOKEN is required")
	}
	address := os.Getenv("HTTP_ADDR")
	if address == "" {
		address = ":8082"
	}
	server := http.Server{Addr: address, Handler: push.Handler(sender, key), ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 15 * time.Second, WriteTimeout: 15 * time.Second, IdleTimeout: 60 * time.Second}
	log.Printf("Notifications service listening on %s; Firebase enabled: %t", address, sender != nil)
	log.Fatal(server.ListenAndServe())
}
