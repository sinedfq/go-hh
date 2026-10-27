import AppLoader from './AppLoader'

export default function PageLoadingState({label='Загружаем…',compact=false}){
    return <div className={`page-loading-state ${compact?'page-loading-compact':''}`} aria-busy="true"><AppLoader label={label}/></div>
}
