import ITAccreditationBadge from '../../components/common/ITAccreditationBadge'
import { useState, useEffect, useRef } from 'react'
import SearchBar from '../../components/vacancy/SearchBar'
import FilterPanel from '../../components/vacancy/FilterPanel'
import VacancyFilterDialog from '../../components/vacancy/VacancyFilterDialog'
import ApplicationModal from '../../components/common/ApplicationModal'
import axios from 'axios'
import VacancyTerms from '../../components/vacancy/VacancyTerms'
import { useAuth } from '../../contexts/AuthContext'
import { searchParams } from '../../utils/searchFilters'
import { userError } from '../../utils/userError'
import { emptySearchFilters } from '../../utils/searchFilters'
import './SearchResultsPage.css'

function SearchResultsPage({
    results, totalCount, loading, filters,
    onApply, onReset, onOpenVacancy, cities, companies, skills, experiences, showToast
}) {
    const [showFilters, setShowFilters] = useState(false)
    const {user}=useAuth()
    const [savingSearch,setSavingSearch]=useState(false),[searchName,setSearchName]=useState(''),[saveError,setSaveError]=useState(''),[saveBusy,setSaveBusy]=useState(false)
    async function saveSearch(event){event.preventDefault();setSaveBusy(true);setSaveError('');try{await axios.post('/api/saved-searches',{name:searchName.trim(),query:searchParams(filters,true).toString()});setSavingSearch(false);setSearchName('');showToast?.('Поиск сохранён. Уведомления о новых вакансиях включены.')}catch(failure){setSaveError(userError(failure))}finally{setSaveBusy(false)}}
    const [draft, setDraft] = useState(filters)
    const [searchHidden,setSearchHidden]=useState(false)
    const headerRef=useRef(null)
    useEffect(()=>{
        const root=headerRef.current?.closest('.content')
        if(!root)return
        let last=root.scrollTop,travel=0
        const scroll=()=>{
            const delta=root.scrollTop-last;last=root.scrollTop
            if(Math.sign(delta)!==Math.sign(travel))travel=0
            travel+=delta
            if(root.scrollTop<100||showFilters||headerRef.current?.contains(document.activeElement))setSearchHidden(false)
            else if(Math.abs(travel)>12)setSearchHidden(delta>0)
        }
        root.addEventListener('scroll',scroll,{passive:true})
        return()=>root.removeEventListener('scroll',scroll)
    },[showFilters])
    const [applyVacancy, setApplyVacancy] = useState(null)
    const [appliedVacancies, setAppliedVacancies] = useState(new Set())

    // Загружаем список откликнутых вакансий
    useEffect(() => {
        const checkApplied = async () => {
            try {
                const res = await axios.get('/api/my-applications')
                const apps = Array.isArray(res.data) ? res.data : []
                setAppliedVacancies(new Set(apps.map(a => a.vacancy_id)))
            } catch (err) {
                console.error('Ошибка проверки откликов:', err)
            }
        }
        checkApplied()
    }, [])

    useEffect(() => {
        if (results.length === 0 && !loading) {
            onApply(filters)
        }
    }, [])

    const truncate = (text, limit = 150) => {
        if (!text) return 'Без описания'
        return text.length > limit ? text.slice(0, limit) + '...' : text
    }

    const handleApplySuccess = (data) => {
        if (applyVacancy) {
            setAppliedVacancies(prev => new Set([...prev, applyVacancy.id]))
        }
        const companyName = applyVacancy?.company || 'Компания'
        setApplyVacancy(null)
        if (showToast) {
            showToast(`✨ Отклик отправлен! ${companyName} скоро свяжется с вами`)
        }
    }

    return (
        <div className="search-results-container animate-fade-in">
            <div className="search-intro"><span className="eyebrow">ВОЗМОЖНОСТИ РЯДОМ</span><h1>Найди свою команду</h1><p>Выбирай подходящий формат работы, сохраняй интересное и знакомься с работодателями.</p></div>
            <div ref={headerRef} className={`search-header ${searchHidden?'search-hidden':''}`} onFocusCapture={()=>setSearchHidden(false)}>
                <div className="search-bar-wrapper">
                    <SearchBar
                        value={filters.query || ''}
                        onChange={(query) => onApply({ ...filters, query })}
                        onSubmit={(query) => onApply({ ...filters, query })}
                        placeholder="Поиск по названию, компании или навыкам..."
                    />
                    <button
                        className={`filter-toggle-button ${showFilters ? 'active' : ''}`}
                        type="button" aria-label="Фильтры" title="Фильтры" aria-expanded={showFilters} aria-controls="vf-dialog-title" aria-haspopup="dialog"
                        onClick={() => { if(!showFilters)setDraft(filters); setShowFilters(!showFilters) }}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="4" y1="21" x2="4" y2="14" />
                            <line x1="4" y1="10" x2="4" y2="3" />
                            <line x1="12" y1="21" x2="12" y2="12" />
                            <line x1="12" y1="8" x2="12" y2="3" />
                            <line x1="20" y1="21" x2="20" y2="16" />
                            <line x1="20" y1="12" x2="20" y2="3" />
                        </svg>
                    </button>
                </div>

                {showFilters && (
                    <VacancyFilterDialog onClose={() => setShowFilters(false)}>
                        <FilterPanel
                            filters={draft}
                            onFiltersChange={setDraft}
                            onApply={() => { onApply({ ...draft, query: filters.query }); setShowFilters(false) }}
                            onReset={() => setDraft({ ...emptySearchFilters(), query: filters.query })}
                            cities={cities}
                            companies={companies}
                            skills={skills}
                            experiences={experiences}
                        />
                    </VacancyFilterDialog>
                )}
            </div>

            <div className="results-count animate-fade-in-delayed">
                {user&&user.role!=='employer'&&<button type="button" className="btn btn-secondary" onClick={()=>{setSearchName(filters.query||filters.location||'Мой поиск');setSavingSearch(value=>!value)}}>Сохранить поиск</button>}
                {loading ? (
                    <span>Ищем вакансии...</span>
                ) : (
                    <>
                        <span className="count-number">{totalCount}</span>
                        <span className="count-text">
                            {totalCount === 1 ? 'вакансия найдена' :
                                totalCount > 1 && totalCount < 5 ? 'вакансии найдены' :
                                    'вакансий найдено'}
                        </span>
                    </>
                )}
            </div>

            {savingSearch&&<form className="save-search-form" onSubmit={saveSearch}><label>Название поиска<input value={searchName} maxLength={80} onChange={event=>setSearchName(event.target.value)} required/></label><p>Новые совпадения появятся в уведомлениях сайта.</p>{saveError&&<p role="alert">{saveError}</p>}<button className="btn btn-primary" disabled={saveBusy||!searchName.trim()}>{saveBusy?'Сохранение…':'Сохранить и подписаться'}</button><button type="button" className="btn btn-secondary" disabled={saveBusy} onClick={()=>setSavingSearch(false)}>Отмена</button></form>}
            {loading ? (
                <div className="loading"><div className="spinner"></div></div>
            ) : results.length === 0 ? (
                <div className="empty-state">
                    <h2>Ничего не найдено</h2>
                    <p>Попробуйте изменить параметры поиска</p>
                    <button className="btn btn-secondary" onClick={onReset}>Сбросить фильтры</button>
                </div>
            ) : (
                <div className="search-results-grid">
                    {results.map((vacancy, index) => {
                        const hasApplied = appliedVacancies.has(vacancy.id)
                        return (
                            <div
                                key={vacancy.id}
                                className="search-result-card animate-search-result"
                                style={{ animationDelay: `${index * 100}ms` }}
                                onClick={() => onOpenVacancy(vacancy.id)}
                            >
                                <h3 className="result-card-title">{vacancy.title}</h3>
                                <VacancyTerms vacancy={vacancy}/>
                                <div className="result-card-company company-name-with-badge">{vacancy.company} <ITAccreditationBadge accredited={vacancy.company_it_accredited}/></div>

                                <div className="result-card-meta">
                                    <span className="meta-item">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                                            <circle cx="12" cy="10" r="3" />
                                        </svg>
                                        {vacancy.location || 'Не указан'}
                                    </span>
                                    <span className="meta-item">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                                        </svg>
                                        {vacancy.experience}
                                    </span>
                                </div>

                                <div className="result-card-skills">
                                    {vacancy.skills?.slice(0, 4).map((skill, i) => (
                                        <span key={i} className="skill-tag">{skill}</span>
                                    ))}
                                    {vacancy.skills?.length > 4 && (
                                        <span className="skill-tag skill-more">+{vacancy.skills.length - 4}</span>
                                    )}
                                </div>

                                {vacancy.description && (
                                    <p className="result-card-description">{truncate(vacancy.description)}</p>
                                )}

                                <div className="result-card-footer">
                                    <span className="views-count">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                            <circle cx="12" cy="12" r="3" />
                                        </svg>
                                        <span>{vacancy.views || 0}</span>
                                    </span>

                                    {hasApplied ? (
                                        <div className="applied-badge-small">
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                                <polyline points="20 6 9 17 4 12" />
                                            </svg>
                                            Отклик отправлен
                                        </div>
                                    ) : (
                                        <button
                                            className="btn btn-primary btn-small"
                                            onClick={(e) => {
                                                e.stopPropagation()
                                                setApplyVacancy(vacancy)
                                            }}
                                        >
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                                            </svg>
                                            Откликнуться
                                        </button>
                                    )}
                                </div>
                            </div>
                        )
                    })}
                </div>
            )}

            {applyVacancy && (
                <ApplicationModal
                    vacancy={applyVacancy}
                    onClose={() => setApplyVacancy(null)}
                    onSuccess={handleApplySuccess}
                />
            )}
        </div>
    )
}

export default SearchResultsPage
