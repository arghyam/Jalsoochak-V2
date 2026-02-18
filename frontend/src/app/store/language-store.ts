import { create } from 'zustand'
import type { LanguageCode } from '@/app/i18n'
import i18n, { SUPPORTED_LANGUAGES, DEFAULT_LANGUAGE } from '@/app/i18n'

export interface LanguageState {
  currentLanguage: LanguageCode
  setLanguage: (lang: LanguageCode) => void
  getSupportedLanguages: () => typeof SUPPORTED_LANGUAGES
}

export const useLanguageStore = create<LanguageState>()((set) => ({
  currentLanguage: (i18n.language?.split('-')[0] as LanguageCode) || DEFAULT_LANGUAGE,

  setLanguage: (lang: LanguageCode) => {
    i18n.changeLanguage(lang)
    set({ currentLanguage: lang })
  },

  getSupportedLanguages: () => SUPPORTED_LANGUAGES,
}))
