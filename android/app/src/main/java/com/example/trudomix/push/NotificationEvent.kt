package com.example.trudomix.push

enum class NotificationEvent(val title:String,val description:String){
    MESSAGES("Сообщения","Новые сообщения в чатах"),
    RESUME_VIEWS("Просмотры резюме","Когда работодатель посмотрел резюме"),
    NEW_APPLICATIONS("Новые отклики","Отклики соискателей на ваши вакансии"),
    INVITATIONS("Приглашения","Приглашения на собеседование"),
    APPLICATION_UPDATES("Статусы и детали откликов","Просмотр, отказ и изменения истории отклика"),
    OTHER("Другие события","Прочие уведомления Trudomix")
}

fun notificationEvent(data:Map<String,String>):NotificationEvent=when{
    data["conversation_id"]?.toIntOrNull()?.let{it>0}==true||data["type"]=="new_message"->NotificationEvent.MESSAGES
    data["type"]=="resume_viewed"->NotificationEvent.RESUME_VIEWS
    data["type"]=="new_application"->NotificationEvent.NEW_APPLICATIONS
    data["type"]=="application_accepted"->NotificationEvent.INVITATIONS
    data["type"]?.startsWith("application_")==true->NotificationEvent.APPLICATION_UPDATES
    else->NotificationEvent.OTHER
}
