import '../../styles/workspace.css'

export function WorkspaceIcon({ name, size = 22 }) {
    const paths = {
        briefcase: <><rect x="3" y="7" width="18" height="14" rx="3"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12c5 4 13 4 18 0M10 13h4"/></>,
        eye: <><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></>,
        chat: <path d="M21 11a8 8 0 0 1-8 8H7l-4 3V7a4 4 0 0 1 4-4h6a8 8 0 0 1 8 8Z"/>,
        check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></>,
        archive: <><rect x="3" y="3" width="18" height="5" rx="1"/><path d="M5 8v12h14V8M9 12h6"/></>,
        plus: <path d="M12 5v14M5 12h14"/>,
        arrow: <path d="M7 17 17 7M7 7h10v10"/>,
        search: <><circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/></>
    }
    return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name] || paths.briefcase}</svg>
}

export default function WorkspaceHeader({ eyebrow, title, description, children }) {
    return <header className="workspace-header"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{children && <div className="workspace-header-actions">{children}</div>}</header>
}

export function WorkspaceMetric({ icon, value, label, hint }) {
    return <div className="workspace-metric"><span className="workspace-metric-icon"><WorkspaceIcon name={icon}/></span><div><span className="workspace-metric-label">{label}</span><strong>{value}</strong>{hint && <small>{hint}</small>}</div></div>
}
