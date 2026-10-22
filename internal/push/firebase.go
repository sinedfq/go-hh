// Package push keeps Firebase transport independent of HTTP and database code.
package push

import (
	"context"
	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"fmt"
)

type Sender struct{ client *messaging.Client }

func New(ctx context.Context, projectID string) (*Sender, error) {
	app, err := firebase.NewApp(ctx, &firebase.Config{ProjectID: projectID})
	if err != nil {
		return nil, fmt.Errorf("firebase: %w", err)
	}
	client, err := app.Messaging(ctx)
	if err != nil {
		return nil, fmt.Errorf("firebase messaging: %w", err)
	}
	return &Sender{client: client}, nil
}

func (s *Sender) Send(ctx context.Context, token, title, body string, data map[string]string) error {
	_, err := s.client.Send(ctx, notificationMessage(token, title, body, data))
	return err
}

func notificationMessage(token, title, body string, data map[string]string) *messaging.Message {
	fields := make(map[string]string, len(data)+2)
	for key, value := range data {
		fields[key] = value
	}
	fields["title"], fields["body"] = title, body
	// The app renders both foreground and background messages with its selected local icon.
	return &messaging.Message{Token: token, Data: fields, Android: &messaging.AndroidConfig{Priority: "high"}}
}

func InvalidToken(err error) bool { return messaging.IsRegistrationTokenNotRegistered(err) }
