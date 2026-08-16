import { useState, useEffect } from 'react'
import axios from 'axios'
import { CircularProgress, DonutChart, HorizontalBars } from '../../components/common/Charts'
import './EmployerPages.css'

function EmployerStatsPage() {
    const [stats, setStats] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        const loadStats = async () => {
            try {
                setLoading(true)
                const res = await axios.get('/api/employer/stats')
                console.log('Employer stats:', res.data)
                setStats(res.data)
            } catch (err) {
                console.error('Ошибка загрузки статистики:', err)
                setError('Не удалось загрузить статистику')
            } finally {
                setLoading(false)
            }
        }
        loadStats()
    }, [])

    if (loading) {
        return (
            <div className="employer-page">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>Загрузка статистики...</p>
                </div>
            </div>
        )
    }

    if (error || !stats) {
        return (
            <div className="employer-page">
                <div className="employer-empty">
                    <p>{error || 'Не удалось загрузить статистику'}</p>
                </div>
            </div>
        )
    }

    // ====== БЕЗОПАСНЫЕ ВЫЧИСЛЕНИЯ (защита от undefined/NaN) ======
    const s = {
        vacancies: stats.total_vacancies || 0,
        vacancyViews: stats.total_vacancy_views || 0,
        apps: stats.total_applications || 0,
        new: stats.new_applications || 0,
        viewed: stats.viewed_applications || 0,
        accepted: stats.accepted_applications || 0,
        rejected: stats.rejected_applications || 0,
        resumeViews: stats.total_resume_views || 0,
    }

    // Конверсия (просмотры → отклики)
    const conversionRate = s.vacancyViews > 0
        ? (s.apps / s.vacancyViews) * 100
        : 0

    // Процент принятых среди решённых
    const decided = s.accepted + s.rejected
    const acceptRate = decided > 0 ? (s.accepted / decided) * 100 : 0

    // Откликов на вакансию (среднее)
    const avgPerVacancy = s.vacancies > 0 ? s.apps / s.vacancies : 0

    // Просмотров на отклик (ИСПРАВЛЕНО: защита от NaN)
    const viewsPerApp = s.apps > 0 ? s.vacancyViews / s.apps : 0

    // Данные для donut chart
    const applicationsData = [
        { label: 'Новые', value: s.new, color: '#0066cc' },
        { label: 'Просмотрено', value: s.viewed, color: '#d97706' },
        { label: 'Принято', value: s.accepted, color: '#2ea043' },
        { label: 'Отклонено', value: s.rejected, color: '#dc3545' },
    ]

    // Данные для горизонтальных баров
    const barItems = [
        { label: 'Просмотров вакансий', value: s.vacancyViews, color: '#2ea043' },
        { label: 'Всего откликов', value: s.apps, color: '#d97706' },
        { label: 'Принятых', value: s.accepted, color: '#0066cc' },
        { label: 'Просмотров резюме', value: s.resumeViews, color: '#9c27b0' },
    ]

    return (
        <div className="employer-page">
            <div className="employer-header">
                <div>
                    <h1>Аналитика</h1>
                    <p className="employer-subtitle">Статистика вашей компании</p>
                </div>
            </div>

            {/* ====== ОСНОВНЫЕ МЕТРИКИ ====== */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-card-icon" style={{ background: 'rgba(0, 102, 204, 0.1)', color: '#0066cc' }}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                            <polyline points="14 2 14 8 20 8" />
                        </svg>
                    </div>
                    <div className="stat-card-content">
                        <div className="stat-card-number">{s.vacancies}</div>
                        <div className="stat-card-label">Активных вакансий</div>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-card-icon" style={{ background: 'rgba(46, 160, 67, 0.1)', color: '#2ea043' }}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                            <circle cx="12" cy="12" r="3" />
                        </svg>
                    </div>
                    <div className="stat-card-content">
                        <div className="stat-card-number">{s.vacancyViews}</div>
                        <div className="stat-card-label">Просмотров вакансий</div>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-card-icon" style={{ background: 'rgba(255, 193, 7, 0.1)', color: '#d97706' }}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
                        </svg>
                    </div>
                    <div className="stat-card-content">
                        <div className="stat-card-number">{s.apps}</div>
                        <div className="stat-card-label">Всего откликов</div>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-card-icon" style={{ background: 'rgba(156, 39, 176, 0.1)', color: '#9c27b0' }}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                            <polyline points="14 2 14 8 20 8" />
                            <line x1="16" y1="13" x2="8" y2="13" />
                            <line x1="16" y1="17" x2="8" y2="17" />
                        </svg>
                    </div>
                    <div className="stat-card-content">
                        <div className="stat-card-number">{s.resumeViews}</div>
                        <div className="stat-card-label">Просмотров резюме</div>
                    </div>
                </div>
            </div>

            {/* ====== ГРАФИКИ: КОНВЕРСИЯ + РАСПРЕДЕЛЕНИЕ ОТКЛИКОВ ====== */}
            <div className="chart-grid-2">
                {/* Круговой прогресс конверсии */}
                <div className="chart-container" style={{ textAlign: 'center' }}>
                    <h3>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                        </svg>
                        Конверсия в отклик
                    </h3>
                    <CircularProgress
                        value={conversionRate}
                        max={100}
                        size={180}
                        strokeWidth={16}
                        label="просмотр → отклик"
                    />
                    <p style={{
                        color: 'var(--text-muted)',
                        fontSize: '0.875rem',
                        margin: '1rem 0 0',
                        lineHeight: 1.5
                    }}>
                        Из {s.vacancyViews} просмотров вакансий получено {s.apps} откликов
                    </p>
                </div>

                {/* Donut chart статусов */}
                <div className="chart-container">
                    <h3>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M21.21 15.89A10 10 0 1 1 8 2.83" />
                            <path d="M22 12A10 10 0 0 0 12 2v10z" />
                        </svg>
                        Статусы откликов
                    </h3>
                    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
                        <DonutChart data={applicationsData} size={180} strokeWidth={24} />
                    </div>
                    <div className="donut-legend">
                        {applicationsData.map((item, i) => {
                            const percent = s.apps > 0 ? ((item.value / s.apps) * 100).toFixed(1) : '0.0'
                            return (
                                <div key={i} className="donut-legend-item">
                                    <div className="donut-legend-left">
                                        <div className="donut-legend-color" style={{ background: item.color }} />
                                        <span className="donut-legend-label">{item.label}</span>
                                    </div>
                                    <div className="donut-legend-right">
                                        <span className="donut-legend-value">{item.value}</span>
                                        <span className="donut-legend-percent">{percent}%</span>
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                </div>
            </div>

            {/* ====== ГОРИЗОНТАЛЬНЫЕ БАРЫ ====== */}
            <div className="chart-container">
                <h3>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="18" y1="20" x2="18" y2="10" />
                        <line x1="12" y1="20" x2="12" y2="4" />
                        <line x1="6" y1="20" x2="6" y2="14" />
                    </svg>
                    Сравнение показателей
                </h3>
                <HorizontalBars items={barItems} />
            </div>

            {/* ====== КЛЮЧЕВЫЕ ПОКАЗАТЕЛИ (без эмодзи) ====== */}
            <div className="chart-container">
                <h3>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                    </svg>
                    Ключевые показатели
                </h3>

                <div className="kpi-grid">
                    <div className="kpi-item">
                        <div className="kpi-label">Среднее на вакансию</div>
                        <div className="kpi-value">
                            {avgPerVacancy.toFixed(1)}
                            <span className="kpi-unit">отклика</span>
                        </div>
                    </div>

                    <div className="kpi-item">
                        <div className="kpi-label">Доля принятых</div>
                        <div className="kpi-value">
                            {acceptRate.toFixed(0)}
                            <span className="kpi-unit">%</span>
                        </div>
                    </div>

                    <div className="kpi-item">
                        <div className="kpi-label">Просмотров на отклик</div>
                        <div className="kpi-value">
                            {viewsPerApp.toFixed(1)}
                            <span className="kpi-unit">просмотров</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default EmployerStatsPage