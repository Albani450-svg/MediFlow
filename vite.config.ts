import { defineConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';
import { API_PORT, pastikanApi } from './scripts/start-api.mjs';

// https://vitejs.dev/config/
export default defineConfig({
  base: "/",
  server: {
    proxy: {
      '/api': {
        target: `http://127.0.0.1:${API_PORT}`,
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: true,
    assetsDir: "code",
    target: ["esnext"],
    cssMinify: true,
    lib: false
  },
  plugins: [
    {
      name: 'mediflow-mysql',
      configureServer(server) {
        if (server.config.server.middlewareMode) return;
        void pastikanApi();
      },
    },
    VitePWA({
      strategies: "injectManifest",
      injectManifest: {
        swSrc: 'public/sw.js',
        swDest: 'dist/sw.js',
        globDirectory: 'dist',
        globPatterns: [
          '**/*.{html,js,css,json,png}',
        ],
      },
      injectRegister: false,
      manifest: false,
      devOptions: {
        enabled: true
      }
    })
  ]
})
