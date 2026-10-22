package com.example.trudomix.data.models

import java.text.NumberFormat
import java.util.Locale

fun vacancySalary(v: Vacancy): String {
    val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU"))
    val range = when {
        v.salary_from > 0 && v.salary_to > 0 && v.salary_from == v.salary_to -> number.format(v.salary_from)
        v.salary_from > 0 && v.salary_to > 0 -> "${number.format(v.salary_from)} – ${number.format(v.salary_to)}"
        v.salary_from > 0 -> "от ${number.format(v.salary_from)}"
        v.salary_to > 0 -> "до ${number.format(v.salary_to)}"
        else -> return v.salary?.takeIf { it.isNotBlank() } ?: "Зарплата не указана"
    }
    val currency = when(v.currency) { "RUB", "" -> "₽"; "USD" -> "$"; "EUR" -> "€"; else -> v.currency }
    return "$range $currency · ${if(v.salary_gross) "до вычета налогов" else "на руки"}"
}

fun vacancyConditions(v: Vacancy): String = listOfNotNull(
    when(v.employment) { "full_time" -> "Полная занятость"; "part_time" -> "Частичная занятость"; "project" -> "Проектная работа"; "internship" -> "Стажировка"; else -> null },
    when(v.schedule) { "full_day" -> "Полный день"; "flexible" -> "Гибкий график"; "shift" -> "Сменный график"; else -> null }
).joinToString(" · ")
