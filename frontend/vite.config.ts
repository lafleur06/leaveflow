import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// During development the Spring Boot API runs on :8080. Proxying /api keeps the browser on
// one origin (:5173), so the backend needs no CORS configuration for local work.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
