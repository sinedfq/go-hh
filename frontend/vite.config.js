import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import {readFileSync} from 'node:fs'

export default defineConfig({
  plugins: [react(),{
    name:'critical-splash-style',
    transformIndexHtml(html){
      return html.replace('<link rel="stylesheet" href="/splash.css" />',()=>`<style>${readFileSync(new URL('./public/splash.css',import.meta.url),'utf8')}</style>`)
    }
  }],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/health': {target:'http://127.0.0.1:8080',changeOrigin:true},
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      // ← ДОБАВЛЕНО: прокси для загруженных файлов
      '/uploads': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      }
    }
  }
})
