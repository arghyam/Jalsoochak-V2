type TextStyle = {
  fontSize?: string | number
  lineHeight?: string | number
  fontWeight?: string | number
  color?: string
}

type ThemeLike = {
  textStyles?: {
    bodyText5?: TextStyle
    bodyText6?: TextStyle
    bodyText7?: TextStyle
  }
  colors?: Record<string, Record<string, string>>
}

const toNumber = (value: string | number | undefined, fallback: number) => {
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = Number.parseFloat(value)
    return Number.isNaN(parsed) ? fallback : parsed
  }
  return fallback
}

const resolveColor = (theme: ThemeLike | undefined, token: string | undefined) => {
  if (!token || !token.includes('.')) return token
  const [scale, shade] = token.split('.')
  return theme?.colors?.[scale]?.[shade] ?? token
}

const getTextStyle = (
  theme: ThemeLike | undefined | unknown,
  key: 'bodyText5' | 'bodyText6' | 'bodyText7',
  defaults: { fontSize: number; lineHeight: number; fontWeight: number | string }
) => {
  const safeTheme = theme as ThemeLike | undefined
  const style = safeTheme?.textStyles?.[key] ?? {}

  return {
    fontSize: toNumber(style.fontSize, defaults.fontSize),
    lineHeight: toNumber(style.lineHeight, defaults.lineHeight),
    fontWeight: style.fontWeight ?? defaults.fontWeight,
    color: resolveColor(safeTheme, style.color),
  }
}

export const getBodyText5Style = (theme: ThemeLike | undefined | unknown) =>
  getTextStyle(theme, 'bodyText5', { fontSize: 14, lineHeight: 21, fontWeight: 400 })

export const getBodyText6Style = (theme: ThemeLike | undefined | unknown) =>
  getTextStyle(theme, 'bodyText6', { fontSize: 14, lineHeight: 21, fontWeight: 'medium' })

export const getBodyText7Style = (theme: ThemeLike | undefined | unknown) =>
  getTextStyle(theme, 'bodyText7', { fontSize: 12, lineHeight: 16, fontWeight: 400 })
