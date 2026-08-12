import { useState, useRef } from 'react'
import './PhotoUpload.css'

function PhotoUpload({ 
  currentPhoto, 
  onUpload, 
  label = "Загрузить фото", 
  size = "medium",
  fallback
}) {
  const [loading, setLoading] = useState(false)
  const fileInputRef = useRef(null)

  const handleFileSelect = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    if (file.size > 5 * 1024 * 1024) {
      alert('Файл слишком большой. Максимум 5MB')
      return
    }

    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      alert('Допустимы только JPG, PNG, WebP')
      return
    }

    setLoading(true)

    try {
      const formData = new FormData()
      formData.append('photo', file)
      await onUpload(formData)
    } catch (err) {
      console.error('Ошибка загрузки фото:', err)
      alert('Не удалось загрузить фото')
    } finally {
      setLoading(false)
      e.target.value = ''
    }
  }

  // Определяем что показывать — используем currentPhoto напрямую
  const renderContent = () => {
    if (currentPhoto) {
      return (
        <img 
          src={currentPhoto} 
          alt="Фото" 
          className="photo-preview"
          key={currentPhoto}  // ← KEY для принудительного обновления при смене URL
        />
      )
    }
    
    if (fallback) {
      return (
        <div className="photo-fallback">
          {fallback.charAt(0).toUpperCase()}
        </div>
      )
    }

    return (
      <div className="photo-placeholder">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
          <circle cx="12" cy="13" r="4"/>
        </svg>
      </div>
    )
  }

  return (
    <div 
      className={`photo-upload photo-upload-${size}`} 
      onClick={() => fileInputRef.current?.click()}
      title={label}
    >
      {renderContent()}
      {loading && <div className="photo-loading-overlay">Загрузка...</div>}
      
      <div className="photo-hover-overlay">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
          <circle cx="12" cy="13" r="4"/>
        </svg>
      </div>
      
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileSelect}
        accept="image/jpeg,image/png,image/webp"
        style={{ display: 'none' }}
      />
    </div>
  )
}

export default PhotoUpload