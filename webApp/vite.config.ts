import { defineConfig } from 'vite';
import { fileURLToPath } from 'node:url';

const jodaCore = fileURLToPath(
  new URL('./node_modules/@js-joda/core/dist/js-joda.js', import.meta.url),
);

// The Kotlin/JS distribution lives outside this project, so Rollup cannot walk
// up to webApp/node_modules to find transitive dependencies, and Vite's dev
// server refuses to serve files from outside the project root. Both paths must
// be allowed explicitly.
const sharedDist = fileURLToPath(
  new URL('../shared/build/dist/js/developmentLibrary', import.meta.url),
);

export default defineConfig({
  root: '.',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    // Skiko's wasm is fetched relative to the module URL; keeping it in assets
    // means the built bundle resolves it correctly.
    assetsInclude: ['**/*.wasm'],
  },
  server: {
    port: 8080,
    // WebUSB and Web Bluetooth require a secure context. Vite serves over
    // http://localhost, which browsers treat as secure, so no extra config is
    // needed for local development.
    fs: {
      allow: ['.', sharedDist],
      // 8.6 MB of skiko.wasm needs a longer read timeout than the default.
      strict: false,
    },
  },
  resolve: {
    alias: {
      '@js-joda/core': jodaCore,
    },
  },
  optimizeDeps: {
    exclude: ['skiko'],
  },
});