import './ConfirmModal.css'
import ModalPortal from './ModalPortal'

function ConfirmModal({ 
    title = 'Подтверждение',
    message,
    confirmText = 'Подтвердить',
    cancelText = 'Отмена',
    danger = false,
    onConfirm,
    onCancel
}) {
    return (
        <ModalPortal>
            <div className="confirm-modal-overlay" onClick={onCancel}>
                <div className="confirm-modal" onClick={(e) => e.stopPropagation()}>
                    <div className="confirm-modal-icon">
                        {danger ? (
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10" />
                                <line x1="12" y1="8" x2="12" y2="12" />
                                <line x1="12" y1="16" x2="12.01" y2="16" />
                            </svg>
                        ) : (
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M9 11l3 3L22 4" />
                                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                            </svg>
                        )}
                    </div>
                    
                    <h3 className="confirm-modal-title">{title}</h3>
                    <p className="confirm-modal-message">{message}</p>
                    
                    <div className="confirm-modal-actions">
                        <button 
                            className="btn btn-secondary"
                            onClick={onCancel}
                        >
                            {cancelText}
                        </button>
                        <button 
                            className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
                            onClick={() => {
                                onConfirm()
                                onCancel()
                            }}
                        >
                            {confirmText}
                        </button>
                    </div>
                </div>
            </div>
        </ModalPortal>
    )
}

export default ConfirmModal