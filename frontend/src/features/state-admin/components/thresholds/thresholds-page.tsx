import { useState, useEffect } from 'react'
import {
  Box,
  Text,
  Button,
  Flex,
  HStack,
  SimpleGrid,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
  NumberIncrementStepper,
  NumberDecrementStepper,
  Heading,
  Spinner,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import {
  getMockThresholdConfiguration,
  saveMockThresholdConfiguration,
} from '../../services/mock-data'
import type { ThresholdConfiguration } from '../../types/thresholds'
import { useToast } from '@/shared/hooks/use-toast'
import { ToastContainer } from '@/shared/components/common'

export function ThresholdsPage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const [config, setConfig] = useState<ThresholdConfiguration | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  // Form state
  const [coverage, setCoverage] = useState('')
  const [continuity, setContinuity] = useState('')
  const [quantity, setQuantity] = useState('')
  const [regularity, setRegularity] = useState('')

  const toast = useToast()

  useEffect(() => {
    document.title = `${t('thresholds.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    fetchConfiguration()
  }, [])

  const fetchConfiguration = async () => {
    setIsLoading(true)
    try {
      const data = await getMockThresholdConfiguration()
      setConfig(data)
      setCoverage(data.coverage)
      setContinuity(data.continuity)
      setQuantity(data.quantity)
      setRegularity(data.regularity)
    } catch (error) {
      console.error('Failed to fetch threshold configuration:', error)
      toast.addToast(t('thresholds.messages.failedToLoad'), 'error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCancel = () => {
    if (config) {
      setCoverage(config.coverage)
      setContinuity(config.continuity)
      setQuantity(config.quantity)
      setRegularity(config.regularity)
    }
  }

  const handleSave = async () => {
    if (!coverage || !continuity || !quantity || !regularity) {
      toast.addToast(t('common:toast.fillAllFields'), 'error')
      return
    }

    setIsSaving(true)
    try {
      const savedConfig = await saveMockThresholdConfiguration({
        coverage,
        continuity,
        quantity,
        regularity,
        isConfigured: true,
      })
      setConfig(savedConfig)
      toast.addToast(t('common:toast.changesSavedShort'), 'success')
    } catch (error) {
      console.error('Failed to save threshold configuration:', error)
      toast.addToast(t('thresholds.messages.failedToSave'), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const hasChanges =
    config &&
    (coverage !== config.coverage ||
      continuity !== config.continuity ||
      quantity !== config.quantity ||
      regularity !== config.regularity)

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('thresholds.title')}
        </Heading>
        <Flex align="center" role="status" aria-live="polite" aria-busy="true">
          <Spinner size="md" color="primary.500" mr={3} />
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  return (
    <Box w="full">
      {/* Page Header */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('thresholds.title')}
        </Heading>
      </Box>

      {/* Configuration Card */}
      <Box
        as="section"
        aria-labelledby="thresholds-heading"
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.100"
        borderRadius={{ base: 'lg', md: 'xl' }}
        w="full"
        minH={{ base: 'auto', lg: 'calc(100vh - 148px)' }}
        py={{ base: 4, md: 6 }}
        px={4}
      >
        <Flex
          as="form"
          role="form"
          aria-label={t('thresholds.aria.formLabel')}
          direction="column"
          w="full"
          h="full"
          justify="space-between"
          minH={{ base: 'auto', lg: 'calc(100vh - 200px)' }}
          gap={{ base: 6, lg: 0 }}
        >
          <Flex direction="column" gap={4}>
            {/* Card Header */}
            <Heading
              as="h2"
              id="thresholds-heading"
              size="h3"
              fontWeight="400"
              fontSize={{ base: 'md', md: 'xl' }}
            >
              {t('thresholds.configurationSettings')}
            </Heading>

            {/* Form Fields Grid - 2x2 Layout */}
            <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={{ base: 4, md: 7 }} maxW="1200px">
              {/* Coverage */}
              <Box
                as="article"
                aria-label={t('thresholds.aria.coverageCard')}
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius={{ base: 'lg', md: 'xl' }}
                bg="neutral.50"
                py={{ base: 4, md: 6 }}
                px={4}
                minH={{ base: 'auto', lg: '174px' }}
              >
                <Heading
                  as="h3"
                  size="h3"
                  fontWeight="400"
                  fontSize={{ base: 'md', md: 'xl' }}
                  mb={1}
                >
                  {t('thresholds.coverage.title')}
                </Heading>
                <Text fontSize={{ base: '12px', md: '14px' }} lineHeight="20px" mb={4}>
                  {t('thresholds.coverage.description')}
                </Text>
                <NumberInput
                  value={coverage}
                  onChange={(valueString) => setCoverage(valueString)}
                  min={0}
                  w={{ base: 'full', xl: '490px' }}
                >
                  <NumberInputField
                    placeholder={t('common:enter')}
                    h="36px"
                    fontSize="md"
                    borderRadius="6px"
                    borderWidth="1px"
                    borderColor="neutral.200"
                    pr="32px"
                    pl="16px"
                    aria-label={t('thresholds.aria.enterCoverage')}
                  />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </Box>

              {/* Continuity */}
              <Box
                as="article"
                aria-label={t('thresholds.aria.continuityCard')}
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius={{ base: 'lg', md: 'xl' }}
                bg="neutral.50"
                py={{ base: 4, md: 6 }}
                px={4}
                minH={{ base: 'auto', lg: '174px' }}
              >
                <Heading
                  as="h3"
                  size="h3"
                  fontWeight="400"
                  fontSize={{ base: 'md', md: 'xl' }}
                  mb={1}
                >
                  {t('thresholds.continuity.title')}
                </Heading>
                <Text fontSize={{ base: '12px', md: '14px' }} lineHeight="20px" mb={4}>
                  {t('thresholds.continuity.description')}
                </Text>
                <NumberInput
                  value={continuity}
                  onChange={(valueString) => setContinuity(valueString)}
                  min={0}
                  w={{ base: 'full', xl: '490px' }}
                >
                  <NumberInputField
                    placeholder={t('common:enter')}
                    h="36px"
                    fontSize="md"
                    borderRadius="6px"
                    borderWidth="1px"
                    borderColor="neutral.200"
                    pr="32px"
                    pl="16px"
                    aria-label={t('thresholds.aria.enterContinuity')}
                  />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </Box>

              {/* Quantity (per capita) */}
              <Box
                as="article"
                aria-label={t('thresholds.aria.quantityCard')}
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius={{ base: 'lg', md: 'xl' }}
                bg="neutral.50"
                py={{ base: 4, md: 6 }}
                px={4}
                minH={{ base: 'auto', lg: '174px' }}
              >
                <Heading
                  as="h3"
                  size="h3"
                  fontWeight="400"
                  fontSize={{ base: 'md', md: 'xl' }}
                  mb={1}
                >
                  {t('thresholds.quantity.title')}
                </Heading>
                <Text fontSize={{ base: '12px', md: '14px' }} lineHeight="20px" mb={4}>
                  {t('thresholds.quantity.description')}
                </Text>
                <NumberInput
                  value={quantity}
                  onChange={(valueString) => setQuantity(valueString)}
                  min={0}
                  w={{ base: 'full', xl: '490px' }}
                >
                  <NumberInputField
                    placeholder={t('common:enter')}
                    h="36px"
                    fontSize="md"
                    borderRadius="6px"
                    borderWidth="1px"
                    borderColor="neutral.200"
                    pr="32px"
                    pl="16px"
                    aria-label={t('thresholds.aria.enterQuantity')}
                  />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </Box>

              {/* Regularity Threshold */}
              <Box
                as="article"
                aria-label={t('thresholds.aria.regularityCard')}
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius={{ base: 'lg', md: 'xl' }}
                bg="neutral.50"
                py={{ base: 4, md: 6 }}
                px={4}
                minH={{ base: 'auto', lg: '174px' }}
              >
                <Heading
                  as="h3"
                  size="h3"
                  fontWeight="400"
                  fontSize={{ base: 'md', md: 'xl' }}
                  mb={1}
                >
                  {t('thresholds.regularity.title')}
                </Heading>
                <Text fontSize={{ base: '12px', md: '14px' }} lineHeight="20px" mb={4}>
                  {t('thresholds.regularity.description')}
                </Text>
                <NumberInput
                  value={regularity}
                  onChange={(valueString) => setRegularity(valueString)}
                  min={0}
                  w={{ base: 'full', xl: '490px' }}
                >
                  <NumberInputField
                    placeholder={t('common:enter')}
                    h="36px"
                    fontSize="md"
                    borderRadius="6px"
                    borderWidth="1px"
                    borderColor="neutral.200"
                    pr="32px"
                    pl="16px"
                    aria-label={t('thresholds.aria.enterRegularity')}
                  />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </Box>
            </SimpleGrid>
          </Flex>

          {/* Action Buttons */}
          <HStack
            spacing={3}
            justify={{ base: 'stretch', sm: 'flex-end' }}
            flexDirection={{ base: 'column-reverse', sm: 'row' }}
            mt={{ base: 4, lg: 6 }}
          >
            <Button
              variant="secondary"
              size="md"
              width={{ base: 'full', sm: '174px' }}
              onClick={handleCancel}
              isDisabled={isSaving || !hasChanges}
            >
              {t('common:button.cancel')}
            </Button>
            <Button
              variant="primary"
              size="md"
              width={{ base: 'full', sm: '174px' }}
              onClick={handleSave}
              isLoading={isSaving}
              isDisabled={!coverage || !continuity || !quantity || !regularity || !hasChanges}
            >
              {t('common:button.save')}
            </Button>
          </HStack>
        </Flex>
      </Box>

      {/* Toast Container */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
    </Box>
  )
}
