import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import { AuthProvider } from './contexts/AuthContext.jsx'
import { BrandProvider } from './contexts/BrandContext.jsx'
import './styles/index.css'

function StartupApp(){
    const [attempt,setAttempt]=React.useState(0)
    React.useEffect(()=>{const retry=()=>setAttempt(value=>value+1);window.addEventListener('trudomix-startup-retry',retry);return()=>window.removeEventListener('trudomix-startup-retry',retry)},[])
    return <App key={attempt}/>
}

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrandProvider><AuthProvider>
            <StartupApp />
        </AuthProvider></BrandProvider>
    </React.StrictMode>,
)
