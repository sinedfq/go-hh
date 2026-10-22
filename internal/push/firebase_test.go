package push

import "testing"

func TestNotificationMessageKeepsChatRouteAndUsesAppRenderer(t *testing.T) {
	data := map[string]string{"type": "message", "conversation_id": "42"}
	message := notificationMessage("test-device", "Новое сообщение", "Вам пришло новое сообщение", data)
	if message.Token != "test-device" || message.Data["conversation_id"] != "42" || message.Data["body"] != "Вам пришло новое сообщение" || message.Data["title"] != "Новое сообщение" {
		t.Fatal("notification lost routing or text")
	}
	if message.Notification != nil || message.Android.Priority != "high" {
		t.Fatal("message must use the app renderer in foreground and background")
	}
	if _, ok := data["body"]; ok {
		t.Fatal("input data mutated")
	}
}
