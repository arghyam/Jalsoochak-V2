import type { ReactNode } from 'react'
import { Outlet } from 'react-router-dom'
import {
  Box,
  Flex,
  IconButton,
  Drawer,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  useDisclosure,
  Show,
  Hide,
} from '@chakra-ui/react'
import { HiMenu } from 'react-icons/hi'
import { useTranslation } from 'react-i18next'
import { Sidebar } from './sidebar'
import { LanguageSwitcher } from '@/shared/components/common'

interface MainLayoutProps {
  children?: ReactNode
}

export function MainLayout({ children }: MainLayoutProps) {
  const { t } = useTranslation('common')
  const { isOpen, onOpen, onClose } = useDisclosure()

  return (
    <Flex h="100vh" w="full" overflow="hidden" bg="neutral.50">
      {/* Skip Link - First focusable element */}
      <Box
        as="a"
        href="#main-content"
        position="absolute"
        left="-9999px"
        top="auto"
        width="1px"
        height="1px"
        overflow="hidden"
        zIndex={100}
        _focus={{
          position: 'fixed',
          top: 4,
          left: 4,
          width: 'auto',
          height: 'auto',
          padding: 4,
          bg: 'primary.500',
          color: 'white',
          borderRadius: 'md',
          fontWeight: 'semibold',
        }}
      >
        {t('sidebar.skipToMain', 'Skip to main content')}
      </Box>

      {/* Desktop Sidebar */}
      <Show above="lg">
        <Sidebar />
      </Show>

      {/* Mobile Drawer */}
      <Hide above="lg">
        <Drawer isOpen={isOpen} onClose={onClose} placement="left">
          <DrawerOverlay />
          <DrawerContent maxW="224px">
            <DrawerCloseButton
              top={4}
              right={4}
              aria-label={t('sidebar.closeMenu', 'Close menu')}
            />
            <Sidebar onNavClick={onClose} />
          </DrawerContent>
        </Drawer>
      </Hide>

      {/* Main Content Area */}
      <Box
        flex={1}
        minW={0}
        ml={{ base: 0, lg: '224px' }}
        display="flex"
        flexDirection="column"
        minH="100vh"
        transition="margin-left 0.2s"
      >
        {/* Mobile Header */}
        <Hide above="lg">
          <Flex
            as="header"
            role="banner"
            position="sticky"
            top={0}
            zIndex={40}
            h="56px"
            px={4}
            align="center"
            justify="space-between"
            bg="white"
            borderBottom="1px"
            borderColor="neutral.100"
          >
            <IconButton
              icon={<HiMenu size={24} />}
              aria-label={t('sidebar.openMenu', 'Open menu')}
              variant="ghost"
              onClick={onOpen}
              minW="44px"
              minH="44px"
            />
            <LanguageSwitcher isMobileHeader />
          </Flex>
        </Hide>

        {/* Desktop Language Switcher */}
        <Show above="lg">
          <LanguageSwitcher />
        </Show>

        {/* Main Content */}
        <Box
          as="main"
          id="main-content"
          role="main"
          tabIndex={-1}
          flex={1}
          minW={0}
          overflow="auto"
          px={{ base: 3, sm: 4, md: 6, lg: 8 }}
          py={{ base: 4, md: 8, lg: 10 }}
          _focus={{ outline: 'none' }}
        >
          {children || <Outlet />}
        </Box>
      </Box>
    </Flex>
  )
}
