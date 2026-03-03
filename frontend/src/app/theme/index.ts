import { extendTheme } from '@chakra-ui/react'

const theme = extendTheme({
  config: {
    initialColorMode: 'light',
    useSystemColorMode: false,
  },
  breakpoints: {
    sm: '30em', // 480px
    md: '48em', // 768px - tablet
    lg: '62em', // 992px
    xl: '80em', // 1280px
    '2xl': '90em', // 1440px - desktop
  },
  fonts: {
    heading: 'Geist, sans-serif',
    body: 'Geist, sans-serif',
  },

  //Titles-Headlines
  textStyles: {
    h5: {
      fontSize: '32px',
      lineHeight: '48px',
      fontWeight: 'semibold',
      color: 'neutral.950',
    },
    h6: {
      fontSize: '24px',
      lineHeight: '36px',
      fontWeight: 'semibold',
      color: 'neutral.950',
    },
    h7: {
      fontSize: '20px',
      lineHeight: '30px',
      fontWeight: 'semibold',
      color: 'neutral.950',
    },

    h8: {
      fontSize: '20px',
      lineHeight: '30px',
      fontWeight: '400',
      color: 'neutral.950',
    },

    h9: {
      fontSize: '24px',
      lineHeight: '36px',
      fontWeight: '400',
      color: 'neutral.950',
    },

    h10: {
      fontSize: '14px',
      lineHeight: '20px',
      fontWeight: '500',
      color: 'neutral.950',
    },

    //Body text

    bodyText2: {
      fontSize: '24px',
      lineHeight: '36px',
      fontWeight: '400',
      letterSpacing: '0%',
      color: 'neutral.950',
    },
    bodyText3: {
      fontSize: '20px',
      lineHeight: '30px',
      fontWeight: '400',
      letterSpacing: '0%',
      color: 'neutral.950',
    },
    bodyText4: {
      fontSize: '16px',
      lineHeight: '24px',
      color: 'neutral.500',
    },
    bodyText5: {
      fontSize: '14px',
      lineHeight: '21px',
      color: 'neutral.500',
    },
    bodyText6: {
      fontWeight: 'medium',
      fontSize: '14px',
      lineHeight: '21px',
      color: 'neutral.950',
    },
    bodyText7: {
      fontSize: '12px',
      lineHeight: '16px',
      fontWeight: '400',
      color: 'neutral.950',
    },
  },

  colors: {
    // Primary Brand Color
    primary: {
      25: '#EBF4FA',
      50: '#D6E9F6',
      100: '#C2DEF1',
      200: '#ADD3ED',
      300: '#84BDE3',
      400: '#5BA7DA',
      500: '#3291D1', // Main brand color
      600: '#2874A7',
      700: '#1E577D',
      800: '#143A54',
      900: '#0F2C3F',
      950: '#050F15',
    },
    // Secondary Color
    secondary: {
      25: '#FFF6E6',
      50: '#FFECCC',
      100: '#FFE3B3',
      200: '#FFD999',
      300: '#FFC766',
      400: '#FFB433',
      500: '#FFA100', // Main secondary color
      600: '#CC8100',
      700: '#996100',
      800: '#664000',
      900: '#4D3000',
      950: '#332000',
    },
    // Neutral Colors
    neutral: {
      25: '#FFFFFF',
      50: '#FAFAFA',
      100: '#F4F4F5',
      200: '#E4E4E7',
      300: '#D1D1D6',
      400: '#A0A0AB',
      500: '#70707B',
      600: '#51525C',
      700: '#3F3F46',
      800: '#26272B',
      900: '#1A1A1E',
      950: '#1C1C1C',
    },
    // System Colors
    success: {
      50: '#E6FFF4',
      100: '#D1FADF',
      500: '#079455',
      600: '#067647',
      700: '#05603A',
    },
    warning: {
      50: '#FFFAEB',
      100: '#FEF0C7',
      500: '#F79009',
      600: '#DC6803',
      700: '#B54708',
    },
    error: {
      50: '#FFEDED',
      100: '#FEE4E2',
      500: '#D92D20',
      600: '#B42318',
      700: '#912018',
    },
  },
  shadows: {
    default: '0px 1px 2px 0px rgba(16, 24, 40, 0.05)',
    active: '0px 0px 0px 4px rgba(50, 145, 209, 0.12)',
  },
  components: {
    Heading: {
      baseStyle: {
        fontFamily: 'heading',
        color: 'neutral.950',
      },
      sizes: {
        h1: {
          fontSize: '32px',
          lineHeight: '48px',
          fontWeight: '600',
        },
        h2: {
          fontSize: '24px',
          lineHeight: '36px',
          fontWeight: '600',
        },
        h3: {
          fontSize: '20px',
          lineHeight: '30px',
          fontWeight: '600',
        },
        h4: {
          fontSize: '14px',
          lineHeight: '20px',
          fontWeight: '500',
        },
      },
      defaultProps: {
        size: 'h2',
      },
    },
    Input: {
      baseStyle: {
        field: {
          _focus: {
            borderColor: 'primary.500',
            boxShadow: '0 0 0 1px var(--chakra-colors-primary-500)',
          },
          _autofill: {
            boxShadow: '0 0 0 1000px #ffffff inset',
            WebkitTextFillColor: 'var(--chakra-colors-neutral-950)',
          },
        },
      },
    },
    Button: {
      baseStyle: {
        fontWeight: 'medium',
        fontFamily: 'Geist, sans-serif',
        _focus: {
          boxShadow: 'none',
        },
      },
      sizes: {
        sm: {
          fontSize: '14px',
          h: '32px',
          minW: '32px',
          px: '12px',
          borderRadius: '4px',
        },
        md: {
          fontSize: '16px',
          h: '40px',
          minW: '40px',
          px: '16px',
          borderRadius: '6px',
        },
        lg: {
          fontSize: '18px',
          h: '44px',
          minW: '44px',
          px: '24px',
          borderRadius: '8px',
        },
      },
      variants: {
        primary: {
          bg: 'primary.500',
          color: 'white',
          _hover: {
            bg: 'primary.600',
            _disabled: {
              bg: 'neutral.200',
            },
          },
          _active: {
            bg: 'primary.700',
          },
          _disabled: {
            bg: 'neutral.300',
            color: 'neutral.25',
            opacity: 1,
            cursor: 'not-allowed',
          },
        },
        secondary: {
          bg: 'transparent',
          color: 'primary.500',
          borderWidth: '1px',
          borderColor: 'primary.500',
          _hover: {
            bg: 'primary.50',
            borderColor: 'primary.600',
            color: 'primary.600',
            _disabled: {
              bg: 'transparent',
              borderColor: 'neutral.200',
              color: 'neutral.400',
            },
          },
          _active: {
            bg: 'primary.100',
            borderColor: 'primary.700',
            color: 'primary.700',
          },
          _disabled: {
            bg: 'transparent',
            borderColor: 'neutral.200',
            color: 'neutral.400',
            opacity: 1,
            cursor: 'not-allowed',
          },
        },
      },
      defaultProps: {
        size: 'md',
        variant: 'primary',
      },
    },
  },
  styles: {
    global: {
      body: {
        bg: 'neutral.50',
        color: 'neutral.950',
        fontSize: '16px',
        lineHeight: '24px',
        fontFamily: 'Geist, sans-serif',
      },
      'input:-webkit-autofill, textarea:-webkit-autofill, select:-webkit-autofill': {
        boxShadow: '0 0 0 1000px #ffffff inset',
        WebkitTextFillColor: 'var(--chakra-colors-neutral-950)',
      },
      'input:-webkit-autofill:focus, textarea:-webkit-autofill:focus, select:-webkit-autofill:focus':
        {
          boxShadow: '0 0 0 1000px #ffffff inset',
          WebkitTextFillColor: 'var(--chakra-colors-neutral-950)',
        },
    },
  },
})

export default theme
