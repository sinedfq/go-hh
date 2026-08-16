import { useState } from 'react'
import './ApplicationModal.css'

function ApplicationModal({ vacancy, onClose, onSuccess }) {
    const [coverLetter, setCoverLetter] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleSubmit = async (e) => {
        e.preventDefault()
        setError('')
        setLoading(true)

        try {
            const response = await fetch(`/api/vacancies/${vacancy.id}/apply`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify({ cover_letter: coverLetter })
            })

            const data = await response.json()

            if (!response.ok) {
                const errorMessages = {
                    'cannot apply to your own vacancy': 'Нельзя откликнуться на свою вакансию',
                    'cannot apply to your company\'s vacancy': 'Нельзя откликнуться на вакансию своей компании',
                    'already applied': 'Вы уже откликнулись на эту вакансию'
                }
                setError(errorMessages[data.error] || data.error || 'Ошибка при отправке')
                return
            }

            onSuccess(data)
        } catch (err) {
            setError('Ошибка сети: ' + err.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="application-modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="application-modal">
                <button className="application-modal-close" onClick={onClose}>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                </button>

                <div className="application-modal-header">
                    <h2>Отклик на вакансию</h2>
                    <p className="application-vacancy-title">{vacancy.title}</p>
                    <p className="application-vacancy-company">{vacancy.company}</p>
                </div>

                <form onSubmit={handleSubmit}>
                    {error && (
                        <div className="application-error">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10" />
                                <line x1="12" y1="8" x2="12" y2="12" />
                                <line x1="12" y1="16" x2="12.01" y2="16" />
                            </svg>
                            {error}
                        </div>
                    )}

                    <div className="application-field">
                        <label>Сопроводительное письмо</label>
                        <textarea
                            value={coverLetter}
                            onChange={(e) => setCoverLetter(e.target.value)}
                            placeholder="Расскажите, почему вы подходите на эту должность..."
                            rows={6}
                            autoFocus
                        />
                        <span className="application-counter">{coverLetter.length} / 1000</span>
                    </div>

                    <div className="application-actions">
                        <button type="button" className="btn btn-secondary" onClick={onClose}>
                            Отмена
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={loading || coverLetter.length > 1000}>
                            {loading ? 'Отправка...' : 'Отправить отклик'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default ApplicationModal