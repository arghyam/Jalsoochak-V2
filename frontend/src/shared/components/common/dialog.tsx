import { type ReactNode } from 'react'
import { Box, Flex, Text, IconButton } from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl'
}

const maxWidthMap = {
  sm: '384px',
  md: '448px',
  lg: '512px',
  xl: '576px',
}

export function Dialog({ open, onClose, title, children, maxWidth = 'md' }: DialogProps) {
  if (!open) return null

  return (
    <Box position="fixed" inset={0} zIndex={50}>
      {/* Backdrop */}
      <Box
        position="absolute"
        inset={0}
        bg="blackAlpha.500"
        transition="opacity 0.2s"
        onClick={onClose}
      />

      {/* Dialog */}
      <Flex align="center" justify="center" minH="100vh" p={4}>
        <Box
          position="relative"
          w="full"
          maxW={maxWidthMap[maxWidth]}
          maxH="90vh"
          overflowY="auto"
          bg="white"
          borderRadius="lg"
          boxShadow="xl"
          p={6}
        >
          {/* Header */}
          <Flex align="center" justify="space-between" mb={4}>
            <Text fontSize="xl" fontWeight="semibold" color="gray.900">
              {title}
            </Text>
            <IconButton aria-label="Close dialog" size="sm" variant="ghost" onClick={onClose}>
              <CloseIcon />
            </IconButton>
          </Flex>

          {/* Content */}
          <Box>{children}</Box>
        </Box>
      </Flex>
    </Box>
  )
}
