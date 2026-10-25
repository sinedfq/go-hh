import { BrandLogo, useBrand } from '../../contexts/BrandContext'
import './PageLoader.css'

/** The supplied loader artwork, with indeterminate progress until data is ready. */
export default function PageLoader({fullscreen=false,compact=false,label='Подбираем возможности для вас'}){
    const brand=useBrand()
    return <div className={`trudomix-loader ${fullscreen?'trudomix-loader-fullscreen':''} ${compact?'trudomix-loader-compact':''}`} style={{'--loader-accent':brand?.accent||'#8651f7'}} role="status" aria-live="polite" aria-label={label}>
        <div className="trudomix-loader-stage">
            <div className="trudomix-loader-decor" aria-hidden="true"><svg viewBox="0 0 700 430" preserveAspectRatio="xMidYMid slice"><path d="M-50 310 C110 180 185 365 350 240 S560 85 760 165"/><path d="M-30 345 C145 235 220 400 405 280 S590 125 735 210"/><circle cx="112" cy="135" r="5"/><circle cx="588" cy="295" r="4"/><polygon points="565,92 569,104 581,108 569,112 565,124 561,112 549,108 561,104"/></svg></div>
            <div className="trudomix-loader-content"><BrandLogo className="trudomix-loader-logo" alt="Логотип Трудомикс"/><strong className="trudomix-loader-name">Трудомикс</strong><span className="trudomix-loader-label">{label}</span><div className="trudomix-loader-progress" aria-hidden="true"><span/></div></div>
        </div>
    </div>
}
