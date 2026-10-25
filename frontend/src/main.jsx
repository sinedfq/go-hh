import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import { AuthProvider } from './contexts/AuthContext.jsx'
import { BrandProvider } from './contexts/BrandContext.jsx'
import './styles/index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrandProvider><AuthProvider>
            <App />
        </AuthProvider></BrandProvider>
    </React.StrictMode>,
)
