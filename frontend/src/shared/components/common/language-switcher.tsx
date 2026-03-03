import {
  Box,
  Button,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  Icon,
  Text,
  Flex,
} from '@chakra-ui/react'
import { IoGlobeOutline } from 'react-icons/io5'
import { useLanguageStore } from '@/app/store'
import type { LanguageCode } from '@/app/i18n'

interface LanguageSwitcherProps {
  isMobileHeader?: boolean
}

export function LanguageSwitcher({ isMobileHeader = false }: LanguageSwitcherProps) {
  const { currentLanguage, setLanguage, getSupportedLanguages } = useLanguageStore()
  const supportedLanguages = getSupportedLanguages()

  const currentLangDisplay = currentLanguage.toUpperCase()

  const handleLanguageChange = (langCode: LanguageCode) => {
    setLanguage(langCode)
  }

  return (
    <Box
      position={isMobileHeader ? 'relative' : 'fixed'}
      top={isMobileHeader ? 'auto' : 2}
      right={isMobileHeader ? 'auto' : { base: 4, lg: 12 }}
      zIndex={50}
    >
      <Menu placement="bottom-end">
        <MenuButton
          as={Button}
          variant="ghost"
          size="sm"
          aria-label="Change language"
          px={2}
          py={1}
          h="36px"
          bg="white"
          borderWidth="1px"
          borderColor="neutral.200"
          borderRadius="8px"
          boxShadow="sm"
          _hover={{ bg: 'neutral.50', borderColor: 'neutral.300' }}
          _active={{ bg: 'neutral.100' }}
        >
          <Flex align="center" gap={1.5}>
            <Icon as={IoGlobeOutline} boxSize={5} color="neutral.600" />
            <Text fontSize="13px" fontWeight="500" color="neutral.700">
              {currentLangDisplay}
            </Text>
          </Flex>
        </MenuButton>
        <MenuList minW="140px" py={1} borderRadius="8px" boxShadow="md" borderColor="neutral.200">
          {supportedLanguages.map((lang) => (
            <MenuItem
              key={lang.code}
              onClick={() => handleLanguageChange(lang.code)}
              bg={currentLanguage === lang.code ? 'primary.50' : 'transparent'}
              color={currentLanguage === lang.code ? 'primary.600' : 'neutral.700'}
              _hover={{ bg: currentLanguage === lang.code ? 'primary.100' : 'neutral.50' }}
              fontSize="14px"
              py={2}
            >
              <Flex justify="space-between" align="center" w="full">
                <Text>{lang.name}</Text>
                <Text fontSize="12px" color="neutral.500">
                  {lang.nativeName}
                </Text>
              </Flex>
            </MenuItem>
          ))}
        </MenuList>
      </Menu>
    </Box>
  )
}
