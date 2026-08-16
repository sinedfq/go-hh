import ModalPortal from '../common/ModalPortal'
import ResumeForm from './ResumeForm'
import './ResumeModal.css'

function ResumeModal({ onClose, onSuccess, resume }) {
    const isEditMode = !!resume

    return (
        <ModalPortal>
            <div className="resume-modal-overlay" onClick={onClose}>
                {/* Кнопка закрытия ВЫНЕ модалки */}
                <button 
                    className="resume-modal-close-btn" 
                    onClick={onClose} 
                    aria-label="Закрыть"
                >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                </button>
                
                <div className="resume-modal" onClick={(e) => e.stopPropagation()}>
                    <ResumeForm
                        onSuccess={onSuccess}
                        onCancel={onClose}
                        initialData={resume}
                        isEditMode={isEditMode}
                    />
                </div>
            </div>
        </ModalPortal>
    )
}

export default ResumeModal