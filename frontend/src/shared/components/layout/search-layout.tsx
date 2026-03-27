import type { ReactNode } from 'react'
import type { ButtonProps, InputProps } from '@chakra-ui/react'
import { Flex, Input, InputGroup, InputLeftElement, Button } from '@chakra-ui/react'
import { SearchIcon } from '@chakra-ui/icons'
import { FiDownload } from 'react-icons/fi'

interface SearchLayoutProps {
  placeholder?: string
  actionLabel?: string
  onActionClick?: () => void
  inputProps?: InputProps
  actionProps?: ButtonProps
  rightSlot?: ReactNode
}

export function SearchLayout({
  placeholder = 'Search by state/UT, district, block, gram panchayat, village',
  actionLabel = 'Download Report',
  onActionClick,
  inputProps,
  actionProps,
  rightSlot,
}: SearchLayoutProps) {
  return (
    <Flex
      as="section"
      w="full"
      align="center"
      borderRadius="12px"
      p="16px"
      bg="neutral.25"
      gap={4}
      border="0.5px solid"
      borderColor="neutral.200"
      direction={{ base: 'column', sm: 'row' }}
      alignItems={{ base: 'stretch', sm: 'center' }}
    >
      <InputGroup flex="1">
        <InputLeftElement pointerEvents="none" p="12px" w="auto" h="32px" alignItems="center">
          <SearchIcon mr="4px" color="neutral.300" />
        </InputLeftElement>
        <Input
          px="12px"
          pl="32px"
          placeholder={placeholder}
          fontSize="14px"
          h="32px"
          borderColor="neutral.300"
          _placeholder={{ color: 'neutral.300' }}
          {...inputProps}
        />
      </InputGroup>
      {rightSlot ?? (
        <Button
          onClick={onActionClick}
          h="32px"
          w={{ base: 'full', sm: '160px' }}
          fontSize="14px"
          variant="primary"
          leftIcon={<FiDownload size="16" />}
          {...actionProps}
        >
          {actionLabel}
        </Button>
      )}
    </Flex>
  )
}
