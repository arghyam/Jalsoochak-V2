import { useState, useRef, useId } from 'react'
import {
  Box,
  Input,
  Text,
  VStack,
  InputGroup,
  InputLeftElement,
  useOutsideClick,
  Flex,
} from '@chakra-ui/react'
import type { ResponsiveValue } from '@chakra-ui/react'
import type { Property } from 'csstype'
import { SearchIcon, ChevronDownIcon } from '@chakra-ui/icons'

export interface SearchableSelectOption {
  value: string
  label: string
}

export interface SearchableSelectProps {
  options: SearchableSelectOption[]
  value: string
  onChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
  width?: ResponsiveValue<Property.Width>
  fontSize?: string
  textColor?: string
  height?: string
  borderRadius?: string
  borderColor?: string
  textStyle?: string
  required?: boolean
  isFilter?: boolean
  id?: string
  'aria-labelledby'?: string
  ariaLabel?: string
  placeholderColor?: string
}

export function SearchableSelect({
  options,
  value,
  onChange,
  placeholder = 'Select',
  disabled = false,
  width = '486px',
  fontSize = 'sm',
  textColor,
  height = '36px',
  borderRadius = '6px',
  borderColor = 'neutral.300',
  textStyle = 'h10',
  required = false,
  isFilter = false,
  id,
  'aria-labelledby': ariaLabelledBy,
  ariaLabel,
  placeholderColor = 'neutral.500',
}: SearchableSelectProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const containerRef = useRef<HTMLDivElement>(null)
  const listboxId = useId()

  useOutsideClick({
    ref: containerRef,
    handler: () => {
      setIsOpen(false)
      setSearchQuery('')
    },
  })

  const selectedOption = options.find((opt) => opt.value === value)

  const filteredOptions = options.filter((option) =>
    option.label.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleSelect = (optionValue: string) => {
    onChange(optionValue)
    setIsOpen(false)
    setSearchQuery('')
  }

  const handleToggle = () => {
    if (!disabled) {
      if (isOpen) {
        setIsOpen(false)
        setSearchQuery('')
      } else {
        setIsOpen(true)
      }
    }
  }

  const displayColor = isFilter
    ? selectedOption
      ? 'primary.500'
      : textColor || placeholderColor
    : textColor || (selectedOption ? 'neutral.950' : placeholderColor)
  const displayBorderColor = isFilter ? (selectedOption ? 'primary.500' : borderColor) : borderColor

  return (
    <Box position="relative" ref={containerRef} w={width}>
      {/* Select Input */}
      <Flex
        as="button"
        type="button"
        id={id}
        role="combobox"
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-controls={listboxId}
        aria-labelledby={ariaLabelledBy}
        aria-label={ariaLabel}
        aria-disabled={disabled}
        disabled={disabled}
        w="full"
        h={height}
        px="12px"
        py="6px"
        bg="white"
        borderWidth="1px"
        borderColor={displayBorderColor}
        borderRadius={borderRadius}
        align="center"
        justify="space-between"
        cursor={disabled ? 'not-allowed' : 'pointer'}
        opacity={disabled ? 0.6 : 1}
        onClick={handleToggle}
        _hover={!disabled ? { borderColor: 'neutral.400' } : undefined}
        _focus={{ borderColor: 'primary.500', outline: 'none' }}
        _disabled={{
          cursor: 'not-allowed',
          opacity: 0.6,
          pointerEvents: 'none',
        }}
      >
        <Text
          fontSize={fontSize}
          color={displayColor}
          textStyle={textStyle}
          fontWeight={isFilter ? 'semibold' : '400'}
          noOfLines={1}
        >
          {selectedOption ? (
            selectedOption.label
          ) : (
            <>
              {placeholder}
              {required && (
                <Text as="span" color="#D92D20">
                  {' '}
                  *
                </Text>
              )}
            </>
          )}
        </Text>
        <ChevronDownIcon
          boxSize={5}
          color="neutral.500"
          transform={isOpen ? 'rotate(180deg)' : 'rotate(0deg)'}
          transition="transform 0.2s"
        />
      </Flex>

      {/* Dropdown Menu */}
      {isOpen && (
        <Box
          position="absolute"
          top="calc(100% + 4px)"
          left={0}
          zIndex={1000}
          bg="white"
          borderWidth="1px"
          borderColor="neutral.100"
          borderRadius="8px"
          boxShadow="0px 4px 6px -2px rgba(10, 13, 18, 0.03)"
          w="full"
          maxH="240px"
          overflow="hidden"
        >
          {/* Search Input */}
          <Box p="8px" borderBottomWidth="1px" borderBottomColor="neutral.100">
            <InputGroup size="sm" borderRadius="4px">
              <InputLeftElement pointerEvents="none">
                <SearchIcon color="neutral.400" boxSize={4} />
              </InputLeftElement>
              <Input
                placeholder="Search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                borderColor={borderColor}
                borderRadius="4px"
                _hover={{ borderColor: 'neutral.400' }}
                _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                fontSize="sm"
                autoFocus
              />
            </InputGroup>
          </Box>

          {/* Options List */}
          <VStack
            id={listboxId}
            role="listbox"
            align="stretch"
            spacing={0}
            maxH="233px"
            overflowY="auto"
            sx={{
              '&::-webkit-scrollbar': {
                width: '4px',
              },
              '&::-webkit-scrollbar-track': {
                bg: 'neutral.50',
              },
              '&::-webkit-scrollbar-thumb': {
                bg: 'neutral.300',
                borderRadius: '2px',
              },
            }}
          >
            {filteredOptions.length > 0 ? (
              filteredOptions.map((option) => (
                <Box
                  key={option.value}
                  role="option"
                  aria-selected={value === option.value}
                  px="12px"
                  py="10px"
                  cursor="pointer"
                  bg={value === option.value ? 'primary.50' : 'white'}
                  _hover={{ bg: value === option.value ? 'primary.50' : 'neutral.50' }}
                  onClick={() => handleSelect(option.value)}
                >
                  <Text
                    fontSize="sm"
                    color={value === option.value ? 'primary.600' : 'neutral.950'}
                    fontWeight={value === option.value ? 'medium' : 'normal'}
                  >
                    {option.label}
                  </Text>
                </Box>
              ))
            ) : (
              <Box px="12px" py="10px" role="status">
                <Text fontSize="sm" color="neutral.500">
                  No results found
                </Text>
              </Box>
            )}
          </VStack>
        </Box>
      )}
    </Box>
  )
}
