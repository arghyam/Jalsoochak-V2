/**
 * Runtime configuration
 *
 * This module provides type-safe access to runtime configuration loaded from /config.js.
 * The config.js file is loaded before the app bundle and can be replaced at deployment time
 * without rebuilding the application.
 */

interface AppConfig {
  API_BASE_URL: string
}

// Extend Window interface to include APP_CONFIG
declare global {
  interface Window {
    APP_CONFIG?: AppConfig
  }
}

/**
 * Get runtime configuration with fallback values
 * @returns Runtime configuration object
 */
export const getRuntimeConfig = (): AppConfig => {
  return {
    API_BASE_URL: window.APP_CONFIG?.API_BASE_URL || 'https://jalsoochak.beehyv.com',
  }
}

/**
 * Get the API base URL from runtime configuration
 * @returns API base URL string
 */
export const getApiBaseUrl = (): string => {
  return getRuntimeConfig().API_BASE_URL
}
