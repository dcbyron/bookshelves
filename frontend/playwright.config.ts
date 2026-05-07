import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30000,
  workers: 1,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:15173",
    trace: "on-first-retry"
  },
  webServer: [
    {
      command: "../scripts/start_spring_e2e.sh",
      url: "http://localhost:18080/health",
      reuseExistingServer: false,
      timeout: 120000
    },
    {
      command: "VITE_API_PROXY_TARGET=http://localhost:18080 npm run dev -- --host localhost --port 15173",
      url: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:15173",
      reuseExistingServer: false,
      timeout: 30000
    }
  ]
});
