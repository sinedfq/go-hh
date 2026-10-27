import {Component, Suspense, useLayoutEffect} from 'react'
import PageLoader from './PageLoader'
import {beginStartupTask} from '../../utils/startupLoading'

function PendingScreen(){
    // A resolved import can still wait for a React Suspense commit. Keep the
    // startup gate closed until the real screen can start its own API effects.
    useLayoutEffect(()=>beginStartupTask(),[])
    return <PageLoader compact/>
}
export default class StartupBoundary extends Component{
    state={failed:false}
    static getDerivedStateFromError(){return {failed:true}}
    render(){
        if(this.state.failed)return <div className="profile-status" role="alert">Не удалось загрузить экран. <button className="btn btn-primary" onClick={()=>location.reload()}>Перезагрузить</button></div>
        return <Suspense fallback={<PendingScreen/>}>{this.props.children}</Suspense>
    }
}
