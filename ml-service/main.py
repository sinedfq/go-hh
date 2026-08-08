from fastapi import FastAPI, HTTPException
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

# Локальная модель как fallback
logger.info("Loading fallback model...")
local_model = SentenceTransformer('all-MiniLM-L6-v2')

# URL Ollama (по умолчанию localhost:11434)
OLLAMA_URL = "http://localhost:11434"
OLLAMA_MODEL = "llama3.2"  # или "phi3"


class Vacancy(BaseModel):
    id: int
    title: str
    company: str
    city: str
    experience: str
    remote: bool


class Resume(BaseModel):
    id: int
    title: str
    skills: List[str]
    experience_years: int
    expected_salary: int
    about: str


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


async def match_with_ollama(resume: Resume, vacancies: List[Vacancy]) -> List[MatchResult]:
    """Используем локальную LLM через Ollama"""
    
    vacancies_text = "\n".join([
        f"{v.id}. {v.title} at {v.company} ({v.experience}, {v.city}, remote={v.remote})"
        for v in vacancies
    ])
    
    prompt = f"""You are a job matching expert. Score how well this candidate matches each vacancy.

CANDIDATE:
Title: {resume.title}
Skills: {', '.join(resume.skills)}
Experience: {resume.experience_years} years
About: {resume.about}

VACANCIES:
{vacancies_text}

For each vacancy, give a score from 0.0 to 1.0 and one sentence reasoning.
Respond ONLY with valid JSON, no other text:
{{"matches": [{{"vacancy_id": 1, "score": 0.85, "reasoning": "..."}}]}}"""

    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(
            f"{OLLAMA_URL}/api/generate",
            json={
                "model": OLLAMA_MODEL,
                "prompt": prompt,
                "stream": False,
                "options": {
                    "temperature": 0.3
                }
            }
        )
        
        if response.status_code != 200:
            raise Exception(f"Ollama error: {response.text}")
        
        result = response.json()
        content = result["response"]
        
        # Парсим JSON из ответа модели
        # Иногда модель добавляет текст вокруг JSON, ищем его
        start = content.find("{")
        end = content.rfind("}") + 1
        if start == -1 or end == 0:
            raise Exception("No JSON found in model response")
        
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
    """Fallback: sentence-transformers"""
    
    resume_text = f"{resume.title} {' '.join(resume.skills)} {resume.about}"
    vacancy_texts = [
        f"{v.title} {v.experience} {v.company}"
        for v in vacancies
    ]
    
    resume_embedding = local_model.encode([resume_text])
    vacancy_embeddings = local_model.encode(vacancy_texts)
    
    similarities = cosine_similarity(resume_embedding, vacancy_embeddings)[0]
    
    matches = []
    for vacancy, sim in zip(vacancies, similarities):
        score = float(max(0, min(1, (sim + 1) / 2)))
        matches.append(MatchResult(
            vacancy_id=vacancy.id,
            score=round(score, 3),
            reasoning="Matched using local embedding model"
        ))
    
    return matches


@app.post("/match", response_model=MatchResponse)
async def match(request: MatchRequest):
    """Главный эндпоинт: Ollama → fallback на embeddings"""
    
    matches = []
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
    
    matches.sort(key=lambda x: x.score, reverse=True)
    
    return MatchResponse(
        resume_id=request.resume.id,
        matches=matches,
        model_used=model_used
    )


@app.get("/health")
def health():
    return {
        "status": "ok",
        "ollama_model": OLLAMA_MODEL,
        "fallback": "sentence-transformers"
    }

if __name__ == "__main__":
    import uvicorn
    print("\n" + "="*50)
    print("Starting ML service on http://127.0.0.1:8000")
    print("Swagger UI: http://127.0.0.1:8000/docs")
    print("="*50 + "\n")
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)