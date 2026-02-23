import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

// Import translation files
import enCommon from '@/locales/en/common.json'
import enSuperAdmin from '@/locales/en/super-admin.json'
import enStateAdmin from '@/locales/en/state-admin.json'
import hiCommon from '@/locales/hi/common.json'
import hiSuperAdmin from '@/locales/hi/super-admin.json'
import hiStateAdmin from '@/locales/hi/state-admin.json'

export const SUPPORTED_LANGUAGES = [
  { code: 'en', name: 'English', nativeName: 'English' },
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी' },
] as const

export type LanguageCode = (typeof SUPPORTED_LANGUAGES)[number]['code']

export const DEFAULT_LANGUAGE: LanguageCode = 'en'

const resources = {
  en: {
    common: enCommon,
    'super-admin': enSuperAdmin,
    'state-admin': enStateAdmin,
  },
  hi: {
    common: hiCommon,
    'super-admin': hiSuperAdmin,
    'state-admin': hiStateAdmin,
  },
}

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: DEFAULT_LANGUAGE,
    defaultNS: 'common',
    ns: ['common', 'super-admin', 'state-admin'],

    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'i18nextLng',
      caches: ['localStorage'],
    },

    interpolation: {
      escapeValue: false, // React already handles XSS
    },

    react: {
      useSuspense: false,
    },
  })

export default i18n
