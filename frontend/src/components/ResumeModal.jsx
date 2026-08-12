import { useEffect, useState } from 'react'
import ModalPortal from './ModalPortal'
import ResumeForm from './ResumeForm'
import './ResumeModal.css'

function ResumeModal({ onClose, onSuccess, resume }) {
  const isEditMode = !!resume

  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [onClose])

  return (
    <ModalPortal>
      <div 
        className="resume-modal-overlay"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose()
        }}
      >
        <button className="modal-close-btn" onClick={onClose} aria-label="Закрыть">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
        <div className="resume-modal">
          <div className="resume-modal-content">
            <ResumeForm 
              onSuccess={onSuccess} 
              onCancel={onClose} 
              initialData={resume}
              isEditMode={isEditMode}
            />
          </div>
        </div>
      </div>
    </ModalPortal>
  )
}

export default ResumeModal