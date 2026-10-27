import './CompactRefreshButton.css'
import AppLoader from './AppLoader'

export default function CompactRefreshButton({loading, onClick, label = 'Обновить', ariaLabel = label}) {
    return <button type="button" className="btn btn-secondary compact-refresh-button" onClick={onClick} disabled={loading} aria-busy={loading} aria-label={ariaLabel}>
        {loading?<AppLoader inline/>:<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"/><path d="M6.1 6.1a8 8 0 0 1 13 2.7L20 12M4 12l.9 3.2a8 8 0 0 0 13 2.7"/></svg>}
        <span>{label}</span>
    </button>
}
