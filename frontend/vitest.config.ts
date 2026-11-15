import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config.ts'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      // a komponenstesztekhez kell DOM; az algoritmus tesztjei tiszta függvényeket hívnak
      environment: 'jsdom',
    },
  }),
)
