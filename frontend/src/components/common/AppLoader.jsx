import BrandMark from './BrandMark'
import './PageLoader.css'

export default function AppLoader({inline=false,label='Загружаем…'}){
    return <span className={`app-loader ${inline?'app-loader-inline':''}`} role="status" aria-label={label}>
        <span className="app-loader-sign"><BrandMark/><span className="app-loader-orbit" aria-hidden="true"/></span>
        {!inline&&<span className="app-loader-label">{label}</span>}
    </span>
}
