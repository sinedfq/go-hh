package com.example.trudomix

import com.example.trudomix.data.models.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class VacancyTermsTest {
    private val json=Json { ignoreUnknownKeys=true }
    @Test fun salaryAndAccreditationMatchServerContract() {
        val v=json.decodeFromString<Vacancy>("""{"id":3,"salary_from":120000,"salary_to":180000,"currency":"RUB","salary_gross":true,"employment":"full_time","schedule":"flexible","company_it_accredited":true,"remote":true}""")
        val text=vacancySalary(v).replace('\u00a0',' ').replace('\u202f',' ')
        assertEquals("120 000 – 180 000 ₽ · до вычета налогов",text)
        assertEquals("Полная занятость · Гибкий график",vacancyConditions(v))
        assertTrue(v.company_it_accredited)
        assertTrue(v.remote)
        assertTrue(json.decodeFromString<Company>("""{"it_accredited":true}""").it_accredited)
    }
    @Test fun missingAndOpenEndedSalariesRemainReadable() {
        assertEquals("Зарплата не указана",vacancySalary(Vacancy()))
        assertTrue(vacancySalary(Vacancy(salary_to=90000,currency="USD")).startsWith("до "))
        assertTrue(vacancySalary(Vacancy(salary_from=1000,currency="EUR")).contains("€"))
        assertEquals("По договорённости",vacancySalary(Vacancy(salary="По договорённости")))
        assertEquals("",vacancyConditions(Vacancy(employment="future_value")))
    }
}
