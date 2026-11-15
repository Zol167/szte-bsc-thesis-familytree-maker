import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // Fejlesztés közben a /api kéréseket a Spring Boot backend kapja, így nincs szükség CORS-ra
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
