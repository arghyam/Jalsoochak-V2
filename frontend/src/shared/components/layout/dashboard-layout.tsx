import type { ReactNode } from 'react'
import { Flex, Box } from '@chakra-ui/react'
import { Header } from './header'

interface DashboardLayoutProps {
  children: ReactNode
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  return (
    <Flex minH="100vh" direction="column" bg="neutral.50">
      <Header />

      <Box as="main" flex="1" overflowY="auto" px={{ base: '40px', md: '80px' }}>
        {children}
      </Box>
    </Flex>
  )
}
