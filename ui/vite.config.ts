import { defineConfig } from "vite";

export default defineConfig({
  build: {
    cssCodeSplit: false,
    rollupOptions: { output: { entryFileNames: "flow.js", assetFileNames: "flow.[ext]" } },
  },
});
