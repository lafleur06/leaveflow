import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// During development the Spring Boot API runs on :8080. Proxying /api keeps the browser on
// one origin (:5173), so the backend needs no CORS configuration for local work.
// LEAVEFLOW_API points the proxy elsewhere, e.g. at the demo container: LEAVEFLOW_API=http://localhost:9090
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.LEAVEFLOW_API ?? 'http://localhost:8080',
    },
  },
})
