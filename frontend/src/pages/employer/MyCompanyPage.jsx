import { useState } from 'react'
import PhotoUpload from '../../components/common/PhotoUpload'
import ITAccreditationBadge from '../../components/common/ITAccreditationBadge'
import { userError } from '../../utils/userError'
import './EmployerPages.css'

function MyCompanyPage({ myCompany, onCreateCompany, onUploadLogo, loadingCompany, onUpdateCompany, onUploadPhoto, onRemovePhoto }) {
    const [editing,setEditing]=useState(false)
    const [form, setForm] = useState({
        name: '',
        description: '',
        industry: '',
        size: '',
        city: '',
        website: '',
        it_accredited: false
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
            const data={...form,benefits:Array.isArray(form.benefits)?form.benefits:(form.benefits||'').split('\n').map(value=>value.trim()).filter(Boolean)}
            if(myCompany?.id)await onUpdateCompany(data);else await onCreateCompany(data)
            setEditing(false)
        } catch (err) {
            setError(userError(err))
        } finally {
            setLoading(false)
        }
    }

    if (loadingCompany && !myCompany) return <div className="loading"><div className="spinner" /></div>

    // Если компания уже есть — показываем информацию
    if (myCompany && myCompany.id && !editing) {
        return (
            <div className="employer-page">
                <div className="employer-header">
                    <div>
                        <h1>Моя компания</h1>
                        <p className="employer-subtitle">Информация о вашей компании</p>
                    </div>
                </div>

                <div className="company-info-card">
                    <button className="btn btn-secondary" onClick={()=>{setForm({...myCompany,benefits:(myCompany.benefits||[]).join('\n')});setEditing(true)}}>Редактировать страницу компании</button>
                    <div className="company-avatar-editor">
                        <PhotoUpload currentPhoto={myCompany.logo_url} onUpload={onUploadLogo} label="Изменить аватарку компании" size="large" fallback={myCompany.name} />
                        <div><h2 className="company-name-with-badge">{myCompany.name} <ITAccreditationBadge accredited={myCompany.it_accredited}/></h2><p>Нажми на аватарку, чтобы загрузить логотип.</p><p>JPG, PNG или WebP · до 4 МБ</p></div>
                    </div>
                    {myCompany.team_story&&<section><h3>Наша команда</h3><p className="preserve-text">{myCompany.team_story}</p></section>}
                    {myCompany.benefits?.length>0&&<section><h3>Преимущества</h3><ul>{myCompany.benefits.map((benefit,index)=><li key={index}>{benefit}</li>)}</ul></section>}
                    {myCompany.hiring_process&&<section><h3>Как проходит найм</h3><p className="preserve-text">{myCompany.hiring_process}</p></section>}
                    <section><h3>Фотографии компании</h3><p>До 6 фотографий · JPG, PNG, WebP до 4 МБ</p><div className="company-gallery">{myCompany.photos?.map(photo=><figure key={photo}><img src={photo} alt="Фотография компании"/><button className="btn btn-secondary" onClick={async()=>{try{await onRemovePhoto(photo)}catch(failure){setError(userError(failure))}}}>Убрать</button></figure>)}{(myCompany.photos?.length||0)<6&&<PhotoUpload onUpload={onUploadPhoto} label="Добавить фотографию компании" fallback="+" size="large"/>}</div></section>
                    {error&&<p role="alert">{error}</p>}
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
                    <h1>{editing?'Редактировать компанию':'Создать компанию'}</h1>
                    <p className="employer-subtitle">Заполните информацию о вашей компании, чтобы начать публиковать вакансии</p>
                </div>
            </div>

            <form className="company-form" onSubmit={handleSubmit}>
                <label className="company-accreditation-option"><input type="checkbox" checked={Boolean(form.it_accredited)} onChange={event=>handleChange('it_accredited',event.target.checked)}/><span><strong>IT-аккредитация</strong><small>У компании есть действующая IT-аккредитация. Статус указывается компанией.</small></span><ITAccreditationBadge accredited/></label>
                <div className="form-field"><label htmlFor="company-team">Наша команда</label><textarea id="company-team" maxLength={10000} value={form.team_story||''} onChange={event=>handleChange('team_story',event.target.value)} rows={4}/></div>
                <div className="form-field"><label htmlFor="company-benefits">Преимущества — по одному на строке</label><textarea id="company-benefits" value={form.benefits||''} onChange={event=>handleChange('benefits',event.target.value)} rows={4}/></div>
                <div className="form-field"><label htmlFor="company-hiring">Как проходит найм</label><textarea id="company-hiring" maxLength={10000} value={form.hiring_process||''} onChange={event=>handleChange('hiring_process',event.target.value)} rows={4}/></div>
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
                        {loading ? 'Сохранение…' : editing?'Сохранить изменения':'Создать компанию'}
                    </button>
                    {editing&&<button type="button" className="btn btn-secondary" disabled={loading} onClick={()=>setEditing(false)}>Отмена</button>}
                </div>
            </form>
        </div>
    )
}

export default MyCompanyPage
