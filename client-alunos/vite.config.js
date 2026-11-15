import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // O navegador chama /api/... (mesma origem do client, localhost:5173), então não há CORS.
      // O Vite repassa para a API em localhost:8080, tirando o prefixo /api:
      //   /api/auth/signin  ->  http://localhost:8080/auth/signin
      // Só vale no npm run dev; em produção o /api precisa ser resolvido por outro lugar (CloudFront, ALB...).
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
