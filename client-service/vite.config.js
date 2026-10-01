import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/patients': 'http://localhost:8080',
      '/appointments': 'http://localhost:8080'
    }
  }
});
