import { useState, useEffect } from 'react'
import axios from 'axios'
import CompanyDetails from '../../components/company/CompanyDetails'
import './CompaniesPage.css'

function CompaniesPage({ onSelectVacancy }) {
    const [companies, setCompanies] = useState([])
    const [loading, setLoading] = useState(true)
    const [selectedCompanyId, setSelectedCompanyId] = useState(null)

    useEffect(() => {
        loadCompanies()
    }, [])
    
    useEffect(() => {
        const handler = (e) => {
            setSelectedCompanyId(e.detail)
        }
        window.addEventListener('open-company', handler)
        return () => window.removeEventListener('open-company', handler)
    }, [])

    const loadCompanies = async () => {
        try {
            setLoading(true)
            const res = await axios.get('/api/companies')
            setCompanies(res.data || [])
        } catch (err) {
            console.error('Ошибка загрузки компаний:', err)
        } finally {
            setLoading(false)
        }
    }

    // Если выбрана компания — показываем её страницу
    if (selectedCompanyId !== null) {
        return (
            <CompanyDetails
                companyId={selectedCompanyId}
                onBack={() => setSelectedCompanyId(null)}
                onSelectVacancy={onSelectVacancy}
            />
        )
    }

    if (loading) {
        return (
            <div className="companies-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка компаний...</p>
                </div>
            </div>
        )
    }

    if (companies.length === 0) {
        return (
            <div className="companies-page">
                <div className="empty-state">
                    <h2>Компаний пока нет</h2>
                    <p>Компании появятся когда будут созданы вакансии</p>
                </div>
            </div>
        )
    }

    return (
        <div className="companies-page animate-in">
            <div className="companies-grid">
                {companies.map((company, index) => (
                    <div
                        key={company.id}
                        className="company-card animate-in"
                        style={{ animationDelay: `${index * 50}ms` }}
                        onClick={() => setSelectedCompanyId(company.id)}
                    >
                        <div className="company-logo">
                            {company.logo_url ? (
                                <img src={company.logo_url} alt={company.name} />
                            ) : (
                                <span>{company.name.charAt(0).toUpperCase()}</span>
                            )}
                        </div>
                        <div className="company-card-info">
                            <h3>{company.name}</h3>
                            {company.industry && (
                                <p className="company-industry">{company.industry}</p>
                            )}
                            {company.city && (
                                <p className="company-city">{company.city}</p>
                            )}
                        </div>
                        <div className="company-card-stats">
                            <span className="vacancies-count">
                                {company.vacancies_count} {getVacancyWord(company.vacancies_count)}
                            </span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}

// Склонение слова "вакансия"
function getVacancyWord(count) {
    const lastTwo = count % 100
    const lastOne = count % 10
    if (lastTwo >= 11 && lastTwo <= 14) return 'вакансий'
    if (lastOne === 1) return 'вакансия'
    if (lastOne >= 2 && lastOne <= 4) return 'вакансии'
    return 'вакансий'
}

export default CompaniesPage