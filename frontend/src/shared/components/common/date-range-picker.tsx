import { useMemo, useRef, useState } from 'react'
import {
  Box,
  Button,
  Flex,
  Input,
  Popover,
  PopoverBody,
  PopoverContent,
  PopoverTrigger,
  Text,
  VStack,
} from '@chakra-ui/react'
import type { ResponsiveValue } from '@chakra-ui/react'
import type { Property } from 'csstype'
import { MdOutlineCalendarToday } from 'react-icons/md'

export type DateRange = {
  startDate: string
  endDate: string
  preset?: string
}

type PresetDefinition = {
  id: string
  label: string
  getRange: (baseDate: Date) => { startDate: string; endDate: string }
}

export interface DateRangePickerProps {
  value: DateRange | null
  onChange: (value: DateRange | null) => void
  placeholder?: string
  disabled?: boolean
  width?: ResponsiveValue<Property.Width>
  fontSize?: string
  textColor?: string
  height?: string
  borderRadius?: string
  borderColor?: string
  textStyle?: string
  isFilter?: boolean
  placeholderColor?: string
}

const formatISODate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const toDisplayValue = (value: string) => {
  const [year, month, day] = value.split('-')
  if (!year || !month || !day) return value
  return `${day}/${month}/${year}`
}

const toDisplayRange = (startDate: string, endDate: string) =>
  `${toDisplayValue(startDate)} - ${toDisplayValue(endDate)}`

const toIsoValue = (value: string) => {
  const [day, month, year] = value.split('/')
  if (!year || !month || !day) return ''
  return `${year}-${month}-${day}`
}

const isValidDisplayDate = (value: string) => {
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(value)) return false
  const [day, month, year] = value.split('/').map((part) => Number(part))
  if (!day || !month || !year) return false
  if (month < 1 || month > 12) return false
  const maxDay = new Date(year, month, 0).getDate()
  return day >= 1 && day <= maxDay
}

const startOfWeek = (date: Date) => {
  const day = date.getDay()
  const diff = day === 0 ? -6 : 1 - day
  const start = new Date(date)
  start.setDate(date.getDate() + diff)
  return start
}

const endOfWeek = (date: Date) => {
  const start = startOfWeek(date)
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  return end
}

const startOfMonth = (date: Date) => new Date(date.getFullYear(), date.getMonth(), 1)

const endOfMonth = (date: Date) => new Date(date.getFullYear(), date.getMonth() + 1, 0)

export function DateRangePicker({
  value,
  onChange,
  placeholder = 'Duration',
  disabled = false,
  width = '162px',
  fontSize = 'sm',
  textColor,
  height = '32px',
  borderRadius = '4px',
  borderColor = 'neutral.400',
  textStyle = 'h10',
  isFilter = false,
  placeholderColor = 'neutral.500',
}: DateRangePickerProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [draft, setDraft] = useState<DateRange | null>(value)
  const [draftIso, setDraftIso] = useState<{ startDate: string; endDate: string } | null>(
    value
      ? {
          startDate: toIsoValue(value.startDate),
          endDate: toIsoValue(value.endDate),
        }
      : null
  )
  const startDateInputRef = useRef<HTMLInputElement | null>(null)
  const endDateInputRef = useRef<HTMLInputElement | null>(null)

  const syncDraftFromValue = (nextValue: DateRange | null) => {
    setDraft(nextValue)
    setDraftIso(
      nextValue
        ? {
            startDate: toIsoValue(nextValue.startDate),
            endDate: toIsoValue(nextValue.endDate),
          }
        : null
    )
  }

  const presets = useMemo<PresetDefinition[]>(
    () => [
      {
        id: 'today',
        label: 'Today',
        getRange: (baseDate) => ({
          startDate: formatISODate(baseDate),
          endDate: formatISODate(baseDate),
        }),
      },
      {
        id: 'yesterday',
        label: 'Yesterday',
        getRange: (baseDate) => {
          const yesterday = new Date(baseDate)
          yesterday.setDate(baseDate.getDate() - 1)
          return {
            startDate: formatISODate(yesterday),
            endDate: formatISODate(yesterday),
          }
        },
      },
      {
        id: 'this-week',
        label: 'This week',
        getRange: (baseDate) => ({
          startDate: formatISODate(startOfWeek(baseDate)),
          endDate: formatISODate(endOfWeek(baseDate)),
        }),
      },
      {
        id: 'last-week',
        label: 'Last week',
        getRange: (baseDate) => {
          const lastWeek = new Date(baseDate)
          lastWeek.setDate(baseDate.getDate() - 7)
          return {
            startDate: formatISODate(startOfWeek(lastWeek)),
            endDate: formatISODate(endOfWeek(lastWeek)),
          }
        },
      },
      {
        id: 'this-month',
        label: 'This month',
        getRange: (baseDate) => ({
          startDate: formatISODate(startOfMonth(baseDate)),
          endDate: formatISODate(endOfMonth(baseDate)),
        }),
      },
      {
        id: 'last-month',
        label: 'Last month',
        getRange: (baseDate) => {
          const lastMonth = new Date(baseDate.getFullYear(), baseDate.getMonth() - 1, 1)
          return {
            startDate: formatISODate(startOfMonth(lastMonth)),
            endDate: formatISODate(endOfMonth(lastMonth)),
          }
        },
      },
    ],
    []
  )

  const displayLabel = value
    ? value.preset || toDisplayRange(toIsoValue(value.startDate), toIsoValue(value.endDate))
    : placeholder

  const displayColor = isFilter
    ? value
      ? 'primary.500'
      : textColor || placeholderColor
    : textColor || (value ? 'neutral.950' : placeholderColor)

  const displayBorderColor = isFilter ? (value ? 'primary.500' : borderColor) : borderColor

  const handleOpen = () => {
    if (!disabled) {
      syncDraftFromValue(value)
      setIsOpen(true)
    }
  }

  const handleClose = () => {
    setIsOpen(false)
  }

  const handlePreset = (preset: PresetDefinition) => {
    const range = preset.getRange(new Date())
    setDraft({
      startDate: toDisplayValue(range.startDate),
      endDate: toDisplayValue(range.endDate),
      preset: preset.label,
    })
    setDraftIso({ startDate: range.startDate, endDate: range.endDate })
  }

  const handleApply = () => {
    if (!draft?.startDate || !draft?.endDate) return
    if (!isValidDisplayDate(draft.startDate) || !isValidDisplayDate(draft.endDate)) return
    const start = toIsoValue(draft.startDate)
    const end = toIsoValue(draft.endDate)
    if (!start || !end) return
    if (start > end) {
      onChange({
        startDate: toDisplayValue(end),
        endDate: toDisplayValue(start),
        preset: draft?.preset,
      })
    } else {
      onChange({
        startDate: toDisplayValue(start),
        endDate: toDisplayValue(end),
        preset: draft?.preset,
      })
    }
    handleClose()
  }

  const handleClear = () => {
    setDraft(null)
    onChange(null)
    handleClose()
  }

  const isApplyDisabled =
    !draft?.startDate ||
    !draft?.endDate ||
    !isValidDisplayDate(draft.startDate) ||
    !isValidDisplayDate(draft.endDate) ||
    toIsoValue(draft.endDate) < toIsoValue(draft.startDate)

  const openPicker = (ref: React.RefObject<HTMLInputElement | null>) => {
    if (!ref.current) return
    if (typeof ref.current.showPicker === 'function') {
      ref.current.showPicker()
    } else {
      ref.current.focus()
      ref.current.click()
    }
  }

  return (
    <Popover
      isOpen={isOpen}
      onOpen={handleOpen}
      onClose={handleClose}
      placement="bottom-end"
      modifiers={[
        { name: 'offset', options: { offset: [260, 8] } },
        { name: 'flip', enabled: false },
        { name: 'preventOverflow', options: { mainAxis: false, altAxis: false, tether: false } },
      ]}
    >
      <PopoverTrigger>
        <Flex
          as="button"
          type="button"
          w={width}
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
          _hover={!disabled ? { borderColor: 'neutral.400' } : undefined}
          _focus={{ borderColor: 'primary.500', outline: 'none' }}
          _disabled={{ cursor: 'not-allowed', opacity: 0.6, pointerEvents: 'none' }}
        >
          <Text
            fontSize={fontSize}
            color={displayColor}
            textStyle={textStyle}
            fontWeight={isFilter ? 'semibold' : '400'}
            noOfLines={1}
          >
            {displayLabel}
          </Text>
          <Box as={MdOutlineCalendarToday} boxSize={4} color="neutral.500" />
        </Flex>
      </PopoverTrigger>
      <PopoverContent w={{ base: 'full', md: '420px' }} borderColor="neutral.100" boxShadow="md">
        <PopoverBody p="16px">
          <Flex direction={{ base: 'column', md: 'row' }} gap="16px">
            <VStack align="stretch" spacing="6px" minW={{ md: '160px' }}>
              <Text textStyle="h10" color="neutral.500">
                Quick ranges
              </Text>
              {presets.map((preset) => {
                const isSelected = draft?.preset === preset.label
                return (
                  <Button
                    key={preset.id}
                    variant="ghost"
                    justifyContent="flex-start"
                    size="sm"
                    fontWeight={isSelected ? '600' : '500'}
                    color={isSelected ? 'primary.500' : 'neutral.600'}
                    onClick={() => handlePreset(preset)}
                  >
                    {preset.label}
                  </Button>
                )
              })}
            </VStack>
            <Box flex="1">
              <Flex gap="12px" wrap="wrap">
                <Box flex="1" minW="180px">
                  <Text textStyle="h10" color="neutral.500" mb="6px">
                    Start date
                  </Text>
                  <Box position="relative">
                    <Input
                      type="text"
                      placeholder="dd/mm/yyyy"
                      value={draft?.startDate ?? ''}
                      onFocus={() => openPicker(startDateInputRef)}
                      onChange={(event) => {
                        const nextValue = event.target.value
                        setDraft((current) => {
                          const currentEnd = current?.endDate ?? ''
                          const shouldAdjustEnd =
                            isValidDisplayDate(nextValue) &&
                            isValidDisplayDate(currentEnd) &&
                            toIsoValue(currentEnd) < toIsoValue(nextValue)
                          return {
                            startDate: nextValue,
                            endDate: shouldAdjustEnd ? nextValue : currentEnd,
                            preset: undefined,
                          }
                        })
                        setDraftIso((current) => {
                          const currentEnd = current?.endDate ?? ''
                          if (!isValidDisplayDate(nextValue)) {
                            return { startDate: '', endDate: currentEnd }
                          }
                          const nextStart = toIsoValue(nextValue)
                          const shouldAdjustEnd = currentEnd && currentEnd < nextStart
                          return {
                            startDate: nextStart,
                            endDate: shouldAdjustEnd ? nextStart : currentEnd,
                          }
                        })
                      }}
                      borderColor="neutral.200"
                      _hover={{ borderColor: 'neutral.300' }}
                      _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                    />
                    <Input
                      ref={startDateInputRef}
                      type="date"
                      value={draftIso?.startDate ?? ''}
                      tabIndex={-1}
                      aria-hidden="true"
                      onChange={(event) => {
                        const nextValue = event.target.value
                        setDraftIso((current) => {
                          const currentEnd = current?.endDate ?? ''
                          const nextEnd =
                            currentEnd && currentEnd < nextValue ? nextValue : currentEnd
                          return {
                            startDate: nextValue,
                            endDate: nextEnd,
                          }
                        })
                        setDraft((current) => {
                          const currentEnd = current?.endDate ?? ''
                          const nextEnd =
                            currentEnd && toIsoValue(currentEnd) < nextValue
                              ? toDisplayValue(nextValue)
                              : currentEnd
                          return {
                            startDate: toDisplayValue(nextValue),
                            endDate: nextEnd,
                            preset: undefined,
                          }
                        })
                      }}
                      position="absolute"
                      top={0}
                      left={0}
                      w="full"
                      h="full"
                      opacity={0}
                      pointerEvents="none"
                    />
                  </Box>
                </Box>
                <Box flex="1" minW="180px">
                  <Text textStyle="h10" color="neutral.500" mb="6px">
                    End date
                  </Text>
                  <Box position="relative">
                    <Input
                      type="text"
                      placeholder="dd/mm/yyyy"
                      value={draft?.endDate ?? ''}
                      onFocus={() => openPicker(endDateInputRef)}
                      onChange={(event) => {
                        const nextValue = event.target.value
                        setDraft((current) => ({
                          startDate: current?.startDate ?? '',
                          endDate: nextValue,
                          preset: undefined,
                        }))
                        setDraftIso((current) => ({
                          startDate: current?.startDate ?? '',
                          endDate: isValidDisplayDate(nextValue) ? toIsoValue(nextValue) : '',
                        }))
                      }}
                      borderColor="neutral.200"
                      _hover={{ borderColor: 'neutral.300' }}
                      _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                    />
                    <Input
                      ref={endDateInputRef}
                      type="date"
                      min={draftIso?.startDate || undefined}
                      value={draftIso?.endDate ?? ''}
                      tabIndex={-1}
                      aria-hidden="true"
                      onChange={(event) => {
                        const nextValue = event.target.value
                        setDraftIso((current) => ({
                          startDate: current?.startDate ?? '',
                          endDate: nextValue,
                        }))
                        setDraft((current) => ({
                          startDate: current?.startDate ?? '',
                          endDate: toDisplayValue(nextValue),
                          preset: undefined,
                        }))
                      }}
                      position="absolute"
                      top={0}
                      left={0}
                      w="full"
                      h="full"
                      opacity={0}
                      pointerEvents="none"
                    />
                  </Box>
                </Box>
              </Flex>
              <Flex justify="flex-end" gap="8px" mt="16px">
                <Button variant="outline" size="sm" onClick={handleClear}>
                  Clear
                </Button>
                <Button
                  colorScheme="teal"
                  size="sm"
                  onClick={handleApply}
                  isDisabled={isApplyDisabled}
                >
                  Apply
                </Button>
              </Flex>
            </Box>
          </Flex>
        </PopoverBody>
      </PopoverContent>
    </Popover>
  )
}
