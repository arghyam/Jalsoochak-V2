import { useEffect } from 'react'
import { Box, Flex, Text } from '@chakra-ui/react'
import { CheckIcon, CloseIcon, InfoIcon, WarningIcon } from '@chakra-ui/icons'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

interface ToastProps {
  id: string
  message: string
  type: ToastType
  onClose: (id: string) => void
  duration?: number
}

export function Toast({ id, message, type, onClose, duration = 3000 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose(id)
    }, duration)

    return () => clearTimeout(timer)
  }, [id, onClose, duration])

  const typeStyles = {
    success: { bg: 'success.500', color: 'white' },
    error: { bg: 'error.500', color: 'white' },
    info: { bg: 'blue.500', color: 'white' },
    warning: { bg: 'warning.500', color: 'white' },
  }

  const icons = {
    success: CheckIcon,
    error: CloseIcon,
    info: InfoIcon,
    warning: WarningIcon,
  }

  const IconComponent = icons[type]
  const styles = typeStyles[type]

  return (
    <Flex
      align="center"
      gap="8px"
      w="200px"
      h="36px"
      bg={styles.bg}
      color={styles.color}
      borderRadius="4px"
      pt="8px"
      pr="12px"
      pb="8px"
      pl="12px"
      boxShadow="lg"
    >
      <Box flexShrink={0}>
        <IconComponent boxSize={4} />
      </Box>
      <Text flex={1} fontSize="sm" fontWeight="medium" noOfLines={1}>
        {message}
      </Text>
    </Flex>
  )
}
