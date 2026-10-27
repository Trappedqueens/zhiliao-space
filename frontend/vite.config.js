import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = {
    '/api': {
      target: env.BACKEND_URL || 'http://127.0.0.1:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
      timeout: 300000,
      proxyTimeout: 300000,
    },
  }
  return {
    plugins: [vue()],
    server: { port: 5173, strictPort: true, proxy },
    preview: { proxy },
  }
})
