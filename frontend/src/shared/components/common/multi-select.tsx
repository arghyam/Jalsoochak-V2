import { useState, useRef, useEffect } from 'react'
import { Box, Flex, Text, IconButton } from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'

interface MultiSelectProps {
  options: string[]
  value: string[]
  onChange: (value: string[]) => void
  placeholder?: string
}

export function MultiSelect({ options, value, onChange, placeholder }: MultiSelectProps) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

  const toggleOption = (option: string) => {
    if (value.includes(option)) {
      onChange(value.filter((v) => v !== option))
    } else {
      onChange([...value, option])
    }
  }

  const removeOption = (option: string) => {
    onChange(value.filter((v) => v !== option))
  }

  return (
    <Box ref={containerRef} position="relative">
      <Box
        minH="42px"
        w="full"
        borderRadius="md"
        borderWidth="1px"
        borderColor={isOpen ? 'primary.500' : 'gray.300'}
        bg="white"
        px={3}
        py={2}
        cursor="pointer"
        transition="border-color 0.2s"
        _hover={{ borderColor: 'gray.400' }}
        onClick={() => setIsOpen(!isOpen)}
      >
        {value.length === 0 ? (
          <Text color="gray.400">{placeholder || 'Select options...'}</Text>
        ) : (
          <Flex flexWrap="wrap" gap={1}>
            {value.map((option) => (
              <Flex
                key={option}
                align="center"
                gap={1}
                bg="primary.100"
                color="primary.700"
                borderRadius="md"
                px={2}
                py={1}
                fontSize="sm"
              >
                <Text>{option}</Text>
                <IconButton
                  aria-label={`Remove ${option}`}
                  size="xs"
                  variant="ghost"
                  color="primary.600"
                  onClick={(e) => {
                    e.stopPropagation()
                    removeOption(option)
                  }}
                  _hover={{ color: 'primary.800' }}
                >
                  <CloseIcon boxSize={2} />
                </IconButton>
              </Flex>
            ))}
          </Flex>
        )}
      </Box>

      {isOpen && (
        <Box
          position="absolute"
          zIndex={10}
          mt={1}
          maxH="60"
          w="full"
          overflowY="auto"
          borderRadius="md"
          borderWidth="1px"
          borderColor="gray.300"
          bg="white"
          boxShadow="lg"
        >
          {options.map((option) => (
            <Flex
              key={option}
              align="center"
              gap={2}
              px={3}
              py={2}
              cursor="pointer"
              transition="background 0.2s"
              bg={value.includes(option) ? 'primary.50' : 'transparent'}
              _hover={{ bg: value.includes(option) ? 'primary.100' : 'gray.100' }}
              onClick={() => toggleOption(option)}
            >
              <input
                type="checkbox"
                checked={value.includes(option)}
                onChange={(e) => {
                  e.stopPropagation()
                  toggleOption(option)
                }}
                style={{
                  height: '16px',
                  width: '16px',
                  borderRadius: '4px',
                  borderColor: '#D1D5DB',
                }}
              />
              <Text
                fontSize="sm"
                fontWeight={value.includes(option) ? 'medium' : 'normal'}
                color={value.includes(option) ? 'primary.700' : 'inherit'}
              >
                {option}
              </Text>
            </Flex>
          ))}
        </Box>
      )}
    </Box>
  )
}
