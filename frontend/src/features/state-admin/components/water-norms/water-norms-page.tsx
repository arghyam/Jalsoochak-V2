import { useState, useEffect } from 'react'
import {
  Box,
  Text,
  Button,
  Flex,
  HStack,
  Input,
  IconButton,
  Heading,
  Spinner,
  SimpleGrid,
  Stack,
} from '@chakra-ui/react'
import { EditIcon } from '@chakra-ui/icons'
import { MdDeleteOutline } from 'react-icons/md'
import { useTranslation } from 'react-i18next'
import {
  getMockWaterNormsConfiguration,
  saveMockWaterNormsConfiguration,
} from '../../services/mock-data'
import type { WaterNormsConfiguration, DistrictOverride } from '../../types/water-norms'
import { AVAILABLE_DISTRICTS } from '../../types/water-norms'
import { useToast } from '@/shared/hooks/use-toast'
import { ToastContainer, SearchableSelect } from '@/shared/components/common'

export function WaterNormsPage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const [config, setConfig] = useState<WaterNormsConfiguration | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [fetchError, setFetchError] = useState(false)
  const [isEditing, setIsEditing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [stateQuantity, setStateQuantity] = useState('')
  const [districtOverrides, setDistrictOverrides] = useState<DistrictOverride[]>([])
  const toast = useToast()

  useEffect(() => {
    document.title = `${t('waterNorms.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const data = await getMockWaterNormsConfiguration()
        setConfig(data)
        setStateQuantity(data.stateQuantity ? String(data.stateQuantity) : '')
        setDistrictOverrides(data.districtOverrides || [])
        setIsEditing(!data.isConfigured)
      } catch (error) {
        console.error('Failed to fetch water norms configuration:', error)
        setFetchError(true)
        toast.addToast(t('waterNorms.messages.failedToLoad'), 'error')
      } finally {
        setIsLoading(false)
      }
    }

    fetchConfig()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleEdit = () => {
    setIsEditing(true)
  }

  const handleCancel = () => {
    if (config) {
      setStateQuantity(config.stateQuantity ? String(config.stateQuantity) : '')
      setDistrictOverrides(config.districtOverrides || [])
      setIsEditing(false)
    }
  }

  const handleSave = async () => {
    if (!stateQuantity) {
      toast.addToast(t('waterNorms.messages.quantityRequired'), 'error')
      return
    }

    const quantity = Number(stateQuantity)
    if (isNaN(quantity) || quantity <= 0) {
      toast.addToast(t('waterNorms.messages.invalidQuantity'), 'error')
      return
    }

    // Validate district overrides
    for (const override of districtOverrides) {
      if (!override.districtName || override.quantity <= 0) {
        toast.addToast(t('waterNorms.messages.invalidOverrides'), 'error')
        return
      }
    }

    setIsSaving(true)
    try {
      const savedConfig = await saveMockWaterNormsConfiguration({
        stateQuantity: quantity,
        districtOverrides,
        isConfigured: true,
      })
      setConfig(savedConfig)
      setIsEditing(false)
      toast.addToast(t('common:toast.changesSavedShort'), 'success')
    } catch (error) {
      console.error('Failed to save water norms configuration:', error)
      toast.addToast(t('common:toast.failedToSave'), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const handleAddDistrict = () => {
    // Check if there's any unfilled district override
    const hasUnfilledOverride = districtOverrides.some(
      (override) => !override.districtName || override.quantity <= 0
    )

    if (hasUnfilledOverride) {
      toast.addToast(t('waterNorms.messages.fillExisting'), 'error')
      return
    }

    const newOverride: DistrictOverride = {
      id: `district-${Date.now()}`,
      districtName: '',
      quantity: 0,
    }
    setDistrictOverrides([...districtOverrides, newOverride])
  }

  const handleRemoveDistrict = (id: string) => {
    setDistrictOverrides(districtOverrides.filter((d) => d.id !== id))
  }

  const handleDistrictChange = (
    id: string,
    field: keyof DistrictOverride,
    value: string | number
  ) => {
    setDistrictOverrides(districtOverrides.map((d) => (d.id === id ? { ...d, [field]: value } : d)))
  }

  const getDistrictLabel = (value: string) => {
    const district = AVAILABLE_DISTRICTS.find((d) => d.value === value)
    return district ? district.label : value
  }

  const getAvailableDistricts = () => {
    const usedDistricts = new Set(districtOverrides.map((d) => d.districtName))
    return AVAILABLE_DISTRICTS.filter((d) => !usedDistricts.has(d.value))
  }

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('waterNorms.title')}
        </Heading>
        <Flex align="center" role="status" aria-live="polite" aria-busy="true">
          <Spinner size="md" color="primary.500" mr={3} />
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  if (fetchError) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('waterNorms.title')}
        </Heading>
        <Text role="alert" color="error.500">
          {t('waterNorms.messages.failedToLoad')}
        </Text>
      </Box>
    )
  }

  return (
    <Box w="full">
      {/* Page Header */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('waterNorms.title')}
        </Heading>
      </Box>

      {/* Water Norms Configuration Card */}
      <Box
        as="section"
        aria-labelledby="water-norms-heading"
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.100"
        borderRadius={{ base: 'lg', md: 'xl' }}
        w="full"
        minH={{ base: 'auto', lg: 'calc(100vh - 148px)' }}
        py={{ base: 4, md: 6 }}
        px={4}
      >
        <Flex direction="column" w="full" h="full" justify="space-between">
          {/* Card Header */}
          <Flex justify="space-between" align="center" mb={4}>
            <Heading
              as="h2"
              id="water-norms-heading"
              size="h3"
              fontWeight="400"
              fontSize={{ base: 'md', md: 'xl' }}
            >
              {t('waterNorms.stateUtWaterNorms')}
            </Heading>
            {config?.isConfigured && !isEditing && (
              <Button
                variant="ghost"
                h={6}
                w={6}
                minW={6}
                pl="2px"
                pr="2px"
                onClick={handleEdit}
                color="neutral.500"
                _hover={{ bg: 'primary.50', color: 'primary.500' }}
                aria-label={t('waterNorms.aria.editConfiguration')}
              >
                <EditIcon h={5} w={5} aria-hidden="true" />
              </Button>
            )}
          </Flex>

          {/* View Mode */}
          {!isEditing && config?.isConfigured ? (
            <Box w="full" h="full" minH={{ base: 'auto', lg: 'calc(100vh - 250px)' }}>
              {/* State Quantity */}
              <Box mb={7}>
                <Text fontSize={{ base: 'xs', md: 'sm' }} fontWeight="medium" mb={1}>
                  {t('waterNorms.currentQuantity')}
                </Text>
                <Text fontSize={{ base: 'xs', md: 'sm' }} color="neutral.950">
                  {config.stateQuantity}
                </Text>
              </Box>

              {/* District-Level Overrides */}
              {districtOverrides.length > 0 && (
                <Box>
                  <Heading
                    as="h3"
                    size="h3"
                    fontWeight="400"
                    fontSize={{ base: 'md', md: 'xl' }}
                    mb={4}
                  >
                    {t('waterNorms.districtOverrides.title')}
                  </Heading>
                  <Stack spacing={4}>
                    {districtOverrides.map((override) => (
                      <SimpleGrid
                        key={override.id}
                        columns={{ base: 1, md: 2 }}
                        spacing={{ base: 3, md: 6 }}
                      >
                        <Box>
                          <Text fontSize={{ base: 'xs', md: 'sm' }} fontWeight="medium" mb={1}>
                            {t('waterNorms.districtOverrides.districtName')}
                          </Text>
                          <Text fontSize={{ base: 'xs', md: 'sm' }} color="neutral.950">
                            {getDistrictLabel(override.districtName)}
                          </Text>
                        </Box>
                        <Box>
                          <Text fontSize={{ base: 'xs', md: 'sm' }} fontWeight="medium" mb={1}>
                            {t('waterNorms.districtOverrides.quantity')}
                          </Text>
                          <Text fontSize={{ base: 'xs', md: 'sm' }} color="neutral.950">
                            {override.quantity}
                          </Text>
                        </Box>
                      </SimpleGrid>
                    ))}
                  </Stack>
                </Box>
              )}
            </Box>
          ) : (
            /* Edit Mode */
            <Flex
              as="form"
              role="form"
              aria-label={t('waterNorms.stateUtWaterNorms')}
              direction="column"
              w="full"
              h="full"
              justify="space-between"
              minH={{ base: 'auto', lg: 'calc(100vh - 250px)' }}
              gap={{ base: 6, lg: 0 }}
            >
              <Box>
                {/* State Quantity Input */}
                <Box mb={6}>
                  <Text
                    as="label"
                    htmlFor="state-quantity"
                    fontSize={{ base: 'xs', md: 'sm' }}
                    fontWeight="medium"
                    color="neutral.950"
                    mb={1}
                    display="block"
                  >
                    {t('waterNorms.currentQuantity')}
                    <Text as="span" color="error.500" ml={1}>
                      *
                    </Text>
                  </Text>
                  <Input
                    id="state-quantity"
                    placeholder={t('common:enter')}
                    value={stateQuantity}
                    onChange={(e) => setStateQuantity(e.target.value)}
                    type="number"
                    w={{ base: 'full', lg: '319px', xl: '486px' }}
                    h="36px"
                    fontSize="sm"
                    borderColor="neutral.300"
                    borderRadius="6px"
                    aria-label={t('waterNorms.aria.enterQuantity')}
                    _hover={{ borderColor: 'neutral.400' }}
                    _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                  />
                </Box>

                {/* District-Level Overrides */}
                <Box>
                  <Heading
                    as="h3"
                    size="h3"
                    fontWeight="400"
                    fontSize={{ base: 'md', md: 'xl' }}
                    mb={4}
                  >
                    {t('waterNorms.districtOverrides.title')}
                  </Heading>

                  <Stack spacing={3} mb={districtOverrides.length > 0 ? 3 : 0}>
                    {districtOverrides.map((override) => (
                      <Flex
                        key={override.id}
                        direction={{ base: 'column', lg: 'row' }}
                        gap={{ base: 3, lg: 6 }}
                        align={{ base: 'stretch', lg: 'flex-end' }}
                      >
                        <Box w={{ base: 'full', lg: '319px', xl: '486px' }}>
                          <Text
                            fontSize={{ base: 'xs', md: 'sm' }}
                            fontWeight="medium"
                            color="neutral.950"
                            mb={1}
                          >
                            {t('waterNorms.districtOverrides.districtName')}
                          </Text>
                          <SearchableSelect
                            options={[
                              ...getAvailableDistricts(),
                              ...(override.districtName
                                ? [
                                    {
                                      value: override.districtName,
                                      label: getDistrictLabel(override.districtName),
                                    },
                                  ]
                                : []),
                            ]}
                            value={override.districtName}
                            onChange={(value) =>
                              handleDistrictChange(override.id, 'districtName', value)
                            }
                            placeholder={t('common:select')}
                            width="100%"
                            ariaLabel={t('waterNorms.aria.selectDistrict')}
                          />
                        </Box>
                        <Box flex={{ base: 'none', lg: 1 }}>
                          <Text
                            fontSize={{ base: 'xs', md: 'sm' }}
                            fontWeight="medium"
                            color="neutral.950"
                            mb={1}
                          >
                            {t('waterNorms.districtOverrides.quantity')}
                          </Text>
                          <Flex gap={2}>
                            <Input
                              placeholder={t('common:enter')}
                              value={override.quantity || ''}
                              onChange={(e) =>
                                handleDistrictChange(
                                  override.id,
                                  'quantity',
                                  Number(e.target.value)
                                )
                              }
                              type="number"
                              fontSize="sm"
                              w={{ base: 'full', lg: '319px', xl: '486px' }}
                              h="36px"
                              borderColor="neutral.300"
                              borderRadius="6px"
                              aria-label={t('waterNorms.aria.enterDistrictQuantity')}
                              _hover={{ borderColor: 'neutral.400' }}
                              _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                            />
                            <IconButton
                              aria-label={t('waterNorms.aria.deleteDistrict', {
                                name: getDistrictLabel(override.districtName) || '',
                              })}
                              icon={<MdDeleteOutline size={24} aria-hidden="true" />}
                              variant="ghost"
                              size="sm"
                              color="neutral.400"
                              onClick={() => handleRemoveDistrict(override.id)}
                              h="36px"
                              _hover={{ bg: 'error.50', color: 'error.500' }}
                            />
                          </Flex>
                        </Box>
                      </Flex>
                    ))}
                  </Stack>

                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={handleAddDistrict}
                    w={{ base: 'full', sm: 'auto' }}
                  >
                    {t('waterNorms.districtOverrides.addNew')}
                  </Button>
                </Box>
              </Box>

              {/* Action Buttons */}
              <HStack
                spacing={3}
                justify={{ base: 'stretch', sm: 'flex-end' }}
                flexDirection={{ base: 'column-reverse', sm: 'row' }}
                mt={{ base: 4, lg: 0 }}
              >
                <Button
                  variant="secondary"
                  size="md"
                  width={{ base: 'full', sm: '174px' }}
                  onClick={handleCancel}
                  isDisabled={isSaving}
                >
                  {t('common:button.cancel')}
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  width={{ base: 'full', sm: '174px' }}
                  onClick={handleSave}
                  isLoading={isSaving}
                  isDisabled={!stateQuantity}
                >
                  {config?.isConfigured ? t('common:button.saveChanges') : t('common:button.save')}
                </Button>
              </HStack>
            </Flex>
          )}
        </Flex>
      </Box>

      {/* Toast Container */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
    </Box>
  )
}
