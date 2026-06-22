import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  preview: {
    allowedHosts: ["rubina-solutions.tech"]
  },
  resolve: {
    alias: {
      vue: "vue/dist/vue.esm-bundler.js"
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:9095",
        changeOrigin: true
      },
      "/uploads": {
        target: "http://localhost:9095",
        changeOrigin: true
      }
    }
  }
});
