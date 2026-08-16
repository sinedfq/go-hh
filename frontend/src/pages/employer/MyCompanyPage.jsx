import { useState } from 'react'
import './EmployerPages.css'

function MyCompanyPage({ myCompany, onCreateCompany }) {
    const [form, setForm] = useState({
        name: '',
        description: '',
        industry: '',
        size: '',
        city: '',
        website: ''
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleChange = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }))
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        setError('')

        if (!form.name.trim()) {
            setError('Укажите название компании')
            return
        }

        setLoading(true)
        try {
            await onCreateCompany(form)
        } catch (err) {
            setError(err.response?.data?.error || 'Ошибка при создании компании')
        } finally {
            setLoading(false)
        }
    }

    // Если компания уже есть — показываем информацию
    if (myCompany && myCompany.id) {
        return (
            <div className="employer-page">
                <div className="employer-header">
                    <div>
                        <h1>Моя компания</h1>
                        <p className="employer-subtitle">Информация о вашей компании</p>
                    </div>
                </div>

                <div className="company-info-card">
                    <div className="company-info-row">
                        <span className="company-info-label">Название</span>
                        <span className="company-info-value">{myCompany.name}</span>
                    </div>
                    {myCompany.description && (
                        <div className="company-info-row">
                            <span className="company-info-label">Описание</span>
                            <span className="company-info-value">{myCompany.description}</span>
                        </div>
                    )}
                    {myCompany.industry && (
                        <div className="company-info-row">
                            <span className="company-info-label">Сфера</span>
                            <span className="company-info-value">{myCompany.industry}</span>
                        </div>
                    )}
                    {myCompany.size && (
                        <div className="company-info-row">
                            <span className="company-info-label">Размер</span>
                            <span className="company-info-value">{myCompany.size}</span>
                        </div>
                    )}
                    {myCompany.city && (
                        <div className="company-info-row">
                            <span className="company-info-label">Город</span>
                            <span className="company-info-value">{myCompany.city}</span>
                        </div>
                    )}
                    {myCompany.website && (
                        <div className="company-info-row">
                            <span className="company-info-label">Сайт</span>
                            <a href={myCompany.website} target="_blank" rel="noreferrer" className="company-info-link">
                                {myCompany.website}
                            </a>
                        </div>
                    )}
                </div>
            </div>
        )
    }

    // Форма создания компании
    return (
        <div className="employer-page">
            <div className="employer-header">
                <div>
                    <h1>Создать компанию</h1>
                    <p className="employer-subtitle">Заполните информацию о вашей компании, чтобы начать публиковать вакансии</p>
                </div>
            </div>

            <form className="company-form" onSubmit={handleSubmit}>
                {error && <div className="employer-error">{error}</div>}

                <div className="form-field">
                    <label>Название компании *</label>
                    <input
                        type="text"
                        value={form.name}
                        onChange={(e) => handleChange('name', e.target.value)}
                        placeholder="ООО 'Ромашка'"
                        required
                    />
                </div>

                <div className="form-field">
                    <label>Описание</label>
                    <textarea
                        value={form.description}
                        onChange={(e) => handleChange('description', e.target.value)}
                        placeholder="Расскажите о компании..."
                        rows={3}
                    />
                </div>

                <div className="form-row-2">
                    <div className="form-field">
                        <label>Сфера деятельности</label>
                        <input
                            type="text"
                            value={form.industry}
                            onChange={(e) => handleChange('industry', e.target.value)}
                            placeholder="IT, производство..."
                        />
                    </div>

                    <div className="form-field">
                        <label>Размер компании</label>
                        <select
                            value={form.size}
                            onChange={(e) => handleChange('size', e.target.value)}
                        >
                            <option value="">Не указано</option>
                            <option value="1-10">1-10 человек</option>
                            <option value="11-50">11-50 человек</option>
                            <option value="51-200">51-200 человек</option>
                            <option value="201-500">201-500 человек</option>
                            <option value="500+">500+ человек</option>
                        </select>
                    </div>
                </div>

                <div className="form-row-2">
                    <div className="form-field">
                        <label>Город</label>
                        <input
                            type="text"
                            value={form.city}
                            onChange={(e) => handleChange('city', e.target.value)}
                            placeholder="Москва"
                        />
                    </div>

                    <div className="form-field">
                        <label>Сайт</label>
                        <input
                            type="url"
                            value={form.website}
                            onChange={(e) => handleChange('website', e.target.value)}
                            placeholder="https://company.com"
                        />
                    </div>
                </div>

                <div className="form-actions">
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? 'Создание...' : 'Создать компанию'}
                    </button>
                </div>
            </form>
        </div>
    )
}

export default MyCompanyPage