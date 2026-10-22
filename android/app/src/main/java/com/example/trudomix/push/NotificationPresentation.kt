package com.example.trudomix.push

data class NotificationPresentation(val channel: String, val label: String, val action: String)

fun notificationPresentation(data: Map<String, String>): NotificationPresentation = when {
    data["conversation_id"]?.toIntOrNull()?.let { it > 0 } == true -> NotificationPresentation(AppNotifications.MESSAGES_CHANNEL, "Новое сообщение", "Открыть чат")
    data["type"] == "resume_viewed" -> NotificationPresentation(AppNotifications.RESUMES_CHANNEL, "Просмотр резюме", "Моё резюме")
    data["type"] == "new_application" || data["type"]?.startsWith("application_") == true -> NotificationPresentation(AppNotifications.APPLICATIONS_CHANNEL, "Отклики и приглашения", "Посмотреть отклик")
    else -> NotificationPresentation(AppNotifications.CHANNEL, "Событие Trudomix", "Открыть приложение")
}

fun notificationTarget(data: Map<String, String>): String? {
    fun id(key: String) = data[key]?.toIntOrNull()?.takeIf { it > 0 }
    id("conversation_id")?.let { return "chat_detail/$it" }
    if (data["type"] == "resume_viewed") return "resume"
    if (data["type"] == "new_application") return "employer"
    if (id("application_id") != null || data["type"]?.startsWith("application_") == true) return "applications"
    id("vacancy_id")?.let { return "vacancy_detail/$it" }
    return null
}

fun notificationKey(data: Map<String, String>, messageId: String?): String {
    data["conversation_id"]?.toIntOrNull()?.takeIf { it > 0 }?.let { return "conversation-$it" }
    if (data["type"] == "resume_viewed") return "resume-${data["resume_id"]}-${data["viewer_user_id"]}"
    data["application_id"]?.let { return "application-$it" }
    return messageId ?: data.toSortedMap().toString()
}
