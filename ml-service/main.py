from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
import httpx
import json
import logging
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Job Matching ML Service")

logger.info("Loading fallback model...")
local_model = SentenceTransformer('all-MiniLM-L6-v2')

OLLAMA_URL = "http://localhost:11434"
OLLAMA_MODEL = "llama3.2:latest"

# Минимальный score для возврата в рекомендациях
MIN_SCORE_THRESHOLD = 0.3


class Vacancy(BaseModel):
    id: int
    title: str
    company: str
    city: str
    experience: str
    remote: bool
    skills: List[str] = []
    description: str = ""


class Resume(BaseModel):
    id: int
    full_name: str
    desired_position: str
    experience: str
    skills: List[str] = []
    about: str = ""
    city: str = ""
    remote: bool = False


class MatchRequest(BaseModel):
    resume: Resume
    vacancies: List[Vacancy]


class MatchResult(BaseModel):
    vacancy_id: int
    score: float
    reasoning: Optional[str] = None


class MatchResponse(BaseModel):
    resume_id: int
    matches: List[MatchResult]
    model_used: str


def experience_to_years(exp: str) -> int:
    """Конвертирует строковый уровень опыта в годы"""
    exp_lower = exp.lower()
    if 'junior' in exp_lower:
        return 1
    elif 'middle' in exp_lower:
        return 3
    elif 'senior' in exp_lower:
        return 6
    elif 'lead' in exp_lower or 'architect' in exp_lower:
        return 8
    return 1


def normalize(text: str) -> str:
    """Нормализация текста для сравнения"""
    return text.lower().strip()


def has_role_match(resume_position: str, vacancy_title: str) -> bool:
    """Проверяет совпадение роли (мягкое)"""
    rp = normalize(resume_position)
    vt = normalize(vacancy_title)

    # Ключевые слова-маркеры для ролей
    role_keywords = {
        'go': ['go developer', 'golang', 'go backend', 'go engineer'],
        'python': ['python developer', 'python backend', 'python engineer'],
        'java': ['java developer', 'java backend', 'java engineer'],
        'frontend': ['frontend', 'react', 'vue', 'angular', 'js developer'],
        'backend': ['backend', 'server-side'],
        'fullstack': ['fullstack', 'full-stack', 'full stack'],
        'data': ['data engineer', 'data scientist', 'data analyst', 'ml engineer', 'etl'],
        'devops': ['devops', 'sre', 'infrastructure', 'platform engineer'],
        'mobile': ['ios', 'android', 'mobile', 'swift', 'kotlin developer'],
        'qa': ['qa', 'test', 'quality assurance'],
        'manager': ['manager', 'lead', 'head of'],
    }

    # Определяем роль резюме
    resume_roles = set()
    for role, keywords in role_keywords.items():
        if any(kw in rp for kw in keywords):
            resume_roles.add(role)

    # Если не определили роль — считаем backend по умолчанию
    if not resume_roles:
        resume_roles.add('backend')

    # Определяем роль вакансии
    vacancy_roles = set()
    for role, keywords in role_keywords.items():
        if any(kw in vt for kw in keywords):
            vacancy_roles.add(role)

    # Если не определили роль вакансии — ищем в описании
    if not vacancy_roles:
        return True  # Не знаем роль — не штрафуем

    # Проверяем пересечение
    return bool(resume_roles & vacancy_roles)


def count_skill_overlap(resume_skills: List[str], vacancy_skills: List[str]) -> int:
    """Считает количество совпадающих навыков (case-insensitive)"""
    rs = set(normalize(s) for s in resume_skills)
    vs = set(normalize(s) for s in vacancy_skills)
    return len(rs & vs)


def apply_hard_penalties(score: float, resume: Resume, vacancy: Vacancy, reasoning: str) -> (float, str):
    """
    Применяет жёсткие штрафы программно, даже если LLM их не применила.
    Это страховка от завышенных оценок.
    """
    penalties = []

    # Штраф 1: Роль не совпадает — максимум 0.35
    if not has_role_match(resume.desired_position, vacancy.title):
        if score > 0.35:
            penalties.append(f"Штраф: должность '{vacancy.title}' не соответствует '{resume.desired_position}'")
            score = min(score, 0.35)

    # Штраф 2: Нет ни одного общего навыка — максимум 0.2
    overlap = count_skill_overlap(resume.skills, vacancy.skills)
    if overlap == 0 and len(resume.skills) > 0 and len(vacancy.skills) > 0:
        if score > 0.2:
            penalties.append("Штраф: нет общих навыков")
            score = min(score, 0.2)

    # Штраф 3: Мало общих навыков (< 2) — максимум 0.5
    elif overlap < 2 and len(vacancy.skills) >= 3:
        if score > 0.55:
            penalties.append(f"Штраф: всего {overlap} общих навыков из {len(vacancy.skills)}")
            score = min(score, 0.55)

    # Штраф 4: Remote preference mismatch
    if resume.remote and not vacancy.remote:
        if score > 0.7:
            penalties.append("Штраф: кандидат ищет удалёнку, а вакансия офисная")
            score = min(score, 0.7)

    # Бонус: много общих навыков
    if overlap >= 3 and len(vacancy.skills) > 0:
        bonus_ratio = overlap / len(vacancy.skills)
        if bonus_ratio >= 0.5 and score < 0.75:
            score = min(0.95, score + 0.1)

    if penalties:
        reasoning = reasoning.rstrip('.') + '. ' + '. '.join(penalties)

    return round(score, 3), reasoning


async def match_with_ollama(resume: Resume, vacancies: List[Vacancy]) -> List[MatchResult]:
    """Используем локальную LLM через Ollama с жёсткими правилами"""

    vacancies_text = ""
    for v in vacancies:
        skills_str = ", ".join(v.skills) if v.skills else "Не указаны"
        vacancies_text += f"""
{v.id}. {v.title} в компании {v.company}
   Город: {v.city}, Удалёнка: {"Да" if v.remote else "Нет"}
   Опыт: {v.experience}
   Требуемые навыки: {skills_str}
   Описание: {v.description[:250] if v.description else "Нет описания"}
"""

    resume_skills = ", ".join(resume.skills) if resume.skills else "Не указаны"

    prompt = f"""Ты — эксперт по подбору IT-специалистов. Твоя задача — оценить насколько хорошо кандидат подходит на каждую вакансию.

КРИТИЧЕСКИ ВАЖНЫЕ ПРАВИЛА ОЦЕНКИ:
1. СОВПАДЕНИЕ ДОЛЖНОСТИ (самое важное):
   - Если желаемая должность кандидата НЕ СОВПАДАЕТ с вакансией (например, кандидат хочет "Go Developer", а вакансия "Data Engineer") — score ДОЛЖЕН быть НЕ БОЛЕЕ 0.35.
   - Совпадение должно быть по сути роли, а не по словам. "Backend Developer" и "Go Engineer" — это совпадение. "Go Developer" и "Frontend Developer" — это НЕ совпадение.

2. СОВПАДЕНИЕ НАВЫКОВ:
   - Если у кандидата нет НИ ОДНОГО общего навыка с вакансией — score НЕ БОЛЕЕ 0.2.
   - Если общих навыков 1 из 4+ — score НЕ БОЛЕЕ 0.5.
   - Если совпадает 3+ навыков из требуемых — это хороший знак, можно ставить 0.7-0.9.

3. УРОВЕНЬ ОПЫТА:
   - Junior кандидат на Senior вакансию — штраф, score не более 0.4.
   - Senior кандидат на Junior вакансию — допустимо, но score не более 0.7.
   - Совпадение уровней — хорошо.

4. УДАЛЁНКА:
   - Если кандидат хочет удалёнку, а вакансия только офисная — штраф 0.1-0.15.

ШКАЛА ОЦЕНОК:
- 0.9-1.0: Идеальное совпадение — та же роль, почти все навыки совпадают
- 0.7-0.89: Хорошее совпадение — роль та же, большинство навыков совпадают
- 0.5-0.69: Частичное совпадение — роль похожа, но часть навыков не хватает
- 0.3-0.49: Слабое совпадение — роль не та, но есть пересечение по навыкам
- 0.0-0.29: Не подходит — совсем другая роль и нет общих навыков

КАНДИДАТ:
Имя: {resume.full_name}
Желаемая должность: {resume.desired_position}
Уровень опыта: {resume.experience} ({experience_to_years(resume.experience)} лет)
Навыки: {resume_skills}
Город: {resume.city or "Любой"}
Готов к удалёнке: {"Да" if resume.remote else "Нет"}
О себе: {resume.about or "Не указано"}

ВАКАНСИИ:
{vacancies_text}

Оцени каждую вакансию по шкале от 0.0 до 1.0.
Дай краткое объяснение на русском (1-2 предложения).
Ответь ТОЛЬКО валидным JSON в формате:
{{"matches": [{{"vacancy_id": 1, "score": 0.85, "reasoning": "краткое объяснение"}}]}}
"""

    async with httpx.AsyncClient(timeout=90.0) as client:
        response = await client.post(
            f"{OLLAMA_URL}/api/generate",
            json={
                "model": OLLAMA_MODEL,
                "prompt": prompt,
                "stream": False,
                "options": {
                    "temperature": 0.2,
                    "num_predict": 2000,
                }
            }
        )

        if response.status_code != 200:
            raise Exception(f"Ollama error: {response.text}")

        result = response.json()
        content = result["response"]

        start = content.find("{")
        end = content.rfind("}") + 1
        if start == -1 or end == 0:
            raise Exception(f"No JSON found in model response. Raw: {content[:200]}")

        parsed = json.loads(content[start:end])

        return [
            MatchResult(
                vacancy_id=m["vacancy_id"],
                score=float(m["score"]),
                reasoning=m.get("reasoning")
            )
            for m in parsed["matches"]
        ]


def match_with_local_model(resume: Resume, vacancies: List[Vacancy]) -> List[MatchResult]:
    """Fallback: sentence-transformers + жёсткие штрафы"""

    resume_skills_str = ", ".join(resume.skills) if resume.skills else ""
    resume_text = f"""
Position: {resume.desired_position}
Skills: {resume_skills_str}
Experience: {resume.experience} ({experience_to_years(resume.experience)} years)
City: {resume.city or "Any"}
Remote: {"Yes" if resume.remote else "No"}
About: {resume.about}
"""

    vacancy_texts = []
    for v in vacancies:
        skills_str = ", ".join(v.skills) if v.skills else ""
        text = f"""
Position: {v.title}
Company: {v.company}
Experience: {v.experience}
Skills: {skills_str}
City: {v.city}
Remote: {"Yes" if v.remote else "No"}
Description: {v.description}
"""
        vacancy_texts.append(text)

    resume_embedding = local_model.encode([resume_text])
    vacancy_embeddings = local_model.encode(vacancy_texts)

    similarities = cosine_similarity(resume_embedding, vacancy_embeddings)[0]

    matches = []
    for vacancy, sim in zip(vacancies, similarities):
        # Базовый score из embeddings
        score = float(max(0, min(1, (sim + 1) / 2)))

        # Усиливаем сигнал от навыков (embeddings его плохо ловят)
        overlap = count_skill_overlap(resume.skills, vacancy.skills)
        if len(vacancy.skills) > 0:
            skill_ratio = overlap / len(vacancy.skills)
            # Смешиваем: 40% embeddings + 60% skills overlap
            score = score * 0.4 + skill_ratio * 0.6

        reasoning = f"Совпадение навыков: {overlap}/{len(vacancy.skills)}"

        # Применяем штрафы
        score, reasoning = apply_hard_penalties(score, resume, vacancy, reasoning)

        matches.append(MatchResult(
            vacancy_id=vacancy.id,
            score=round(score, 3),
            reasoning=reasoning
        ))

    return matches


@app.post("/match", response_model=MatchResponse)
async def match(request: MatchRequest):
    """Главный эндпоинт: Ollama → fallback на embeddings + жёсткие штрафы"""

    model_used = "local-embeddings"

    try:
        logger.info("Trying Ollama LLM...")
        matches = await match_with_ollama(request.resume, request.vacancies)
        model_used = f"ollama-{OLLAMA_MODEL}"
        logger.info("Ollama match successful")
    except Exception as e:
        logger.error(f"Ollama failed: {e}, falling back to embeddings")
        matches = match_with_local_model(request.resume, request.vacancies)
        model_used = "local-embeddings-fallback"

    # Пост-обработка: применяем жёсткие штрафы даже к LLM-результатам
    vacancy_map = {v.id: v for v in request.vacancies}
    processed_matches = []
    for m in matches:
        vacancy = vacancy_map.get(m.vacancy_id)
        if not vacancy:
            continue

        # Применяем штрафы как страховку
        final_score, final_reasoning = apply_hard_penalties(
            m.score,
            request.resume,
            vacancy,
            m.reasoning or ""
        )

        # Фильтруем по минимальному порогу
        if final_score >= MIN_SCORE_THRESHOLD:
            processed_matches.append(MatchResult(
                vacancy_id=m.vacancy_id,
                score=final_score,
                reasoning=final_reasoning
            ))

    processed_matches.sort(key=lambda x: x.score, reverse=True)

    logger.info(
        f"Returning {len(processed_matches)} matches out of {len(request.vacancies)} candidates "
        f"(filtered < {MIN_SCORE_THRESHOLD})"
    )

    return MatchResponse(
        resume_id=request.resume.id,
        matches=processed_matches,
        model_used=model_used
    )


@app.get("/health")
def health():
    return {
        "status": "ok",
        "ollama_model": OLLAMA_MODEL,
        "fallback": "sentence-transformers",
        "min_score_threshold": MIN_SCORE_THRESHOLD
    }


if __name__ == "__main__":
    import uvicorn
    print("\n" + "="*50)
    print("Starting ML service on http://127.0.0.1:8000")
    print("Swagger UI: http://127.0.0.1:8000/docs")
    print("="*50 + "\n")
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)