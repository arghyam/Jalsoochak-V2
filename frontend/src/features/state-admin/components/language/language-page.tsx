import { useState, useEffect } from 'react'
import { Box, Text, Button, Flex, HStack, Heading, Spinner, SimpleGrid } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { EditIcon } from '@chakra-ui/icons'
import {
  getMockLanguageConfiguration,
  saveMockLanguageConfiguration,
} from '../../services/mock-data'
import type { LanguageConfiguration } from '../../types/language'
import { AVAILABLE_LANGUAGES } from '../../types/language'
import { useToast } from '@/shared/hooks/use-toast'
import { ToastContainer, SearchableSelect } from '@/shared/components/common'

export function LanguagePage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const [config, setConfig] = useState<LanguageConfiguration | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [primaryLanguage, setPrimaryLanguage] = useState('')
  const [secondaryLanguage, setSecondaryLanguage] = useState('')
  const toast = useToast()

  useEffect(() => {
    document.title = `${t('language.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const data = await getMockLanguageConfiguration()
        setConfig(data)
        setPrimaryLanguage(data.primaryLanguage)
        setSecondaryLanguage(data.secondaryLanguage || '')
        setIsEditing(!data.isConfigured)
      } catch (error) {
        console.error('Failed to fetch language configuration:', error)
      } finally {
        setIsLoading(false)
      }
    }

    fetchConfig()
  }, [])

  const handleEdit = () => {
    setIsEditing(true)
  }

  const handleCancel = () => {
    if (config) {
      setPrimaryLanguage(config.primaryLanguage)
      setSecondaryLanguage(config.secondaryLanguage || '')
      setIsEditing(false)
    }
  }

  const handleSave = async () => {
    if (!primaryLanguage) {
      return
    }

    setIsSaving(true)
    try {
      const savedConfig = await saveMockLanguageConfiguration({
        primaryLanguage,
        secondaryLanguage: secondaryLanguage || undefined,
        isConfigured: true,
      })
      setConfig(savedConfig)
      setIsEditing(false)
      toast.addToast(t('common:toast.changesSavedShort'), 'success')
    } catch (error) {
      console.error('Failed to save language configuration:', error)
      toast.addToast(t('common:toast.failedToSave'), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const getPrimaryLanguageLabel = () => {
    const lang = AVAILABLE_LANGUAGES.find((l) => l.value === primaryLanguage)
    return lang ? lang.label : primaryLanguage
  }

  const getSecondaryLanguageLabel = () => {
    if (!secondaryLanguage) return ''
    const lang = AVAILABLE_LANGUAGES.find((l) => l.value === secondaryLanguage)
    return lang ? lang.label : secondaryLanguage
  }

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('language.title')}
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
          {t('language.title')}
        </Heading>
      </Box>

      {/* Language Configuration Card */}
      <Box
        as="section"
        aria-labelledby="language-config-heading"
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
              id="language-config-heading"
              size="h3"
              textStyle="h8"
              fontWeight="400"
              fontSize={{ base: 'md', md: 'xl' }}
            >
              {t('language.configuration')}
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
                color="neutral.950"
                _hover={{ bg: 'primary.50', color: 'primary.500' }}
                aria-label={t('language.aria.editConfiguration')}
              >
                <EditIcon h={5} w={5} aria-hidden="true" />
              </Button>
            )}
          </Flex>

          {/* View Mode */}
          {!isEditing && config?.isConfigured ? (
            <Box w="full" h="full" minH={{ base: 'auto', lg: 'calc(100vh - 250px)' }}>
              <SimpleGrid columns={{ base: 1, md: 2 }} spacing={{ base: 4, md: 6 }} mb={4}>
                <Box>
                  <Text
                    fontSize={{ base: 'xs', md: 'sm' }}
                    fontWeight="medium"
                    color="neutral.700"
                    mb={1}
                  >
                    {t('language.primaryLanguage')}
                  </Text>
                  <Text fontSize={{ base: 'xs', md: 'sm' }} color="neutral.950">
                    {getPrimaryLanguageLabel()}
                  </Text>
                </Box>
                <Box>
                  <Text
                    fontSize={{ base: 'xs', md: 'sm' }}
                    fontWeight="medium"
                    color="neutral.700"
                    mb={1}
                  >
                    {t('language.secondaryLanguage')}
                  </Text>
                  <Text fontSize={{ base: 'xs', md: 'sm' }} color="neutral.950">
                    {getSecondaryLanguageLabel() || '-'}
                  </Text>
                </Box>
              </SimpleGrid>
            </Box>
          ) : (
            /* Edit Mode */
            <Flex
              as="form"
              role="form"
              aria-label={t('language.configuration')}
              direction="column"
              w="full"
              h="full"
              justify="space-between"
              minH={{ base: 'auto', lg: 'calc(100vh - 250px)' }}
              gap={{ base: 6, lg: 0 }}
            >
              <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}>
                <Box w={{ base: 'full', xl: '486px' }}>
                  <Text
                    as="label"
                    id="primary-language-label"
                    fontSize={{ base: 'xs', md: 'sm' }}
                    fontWeight="medium"
                    color="neutral.950"
                    mb={1}
                    display="block"
                  >
                    {t('language.primaryLanguage')}
                    <Text as="span" color="error.500" ml={1}>
                      *
                    </Text>
                  </Text>
                  <SearchableSelect
                    options={AVAILABLE_LANGUAGES}
                    value={primaryLanguage}
                    onChange={setPrimaryLanguage}
                    placeholder={t('common:select')}
                    width="100%"
                    ariaLabel={t('language.aria.selectPrimaryLanguage')}
                  />
                </Box>
                <Box w={{ base: 'full', xl: '486px' }}>
                  <Text
                    as="label"
                    id="secondary-language-label"
                    fontSize={{ base: 'xs', md: 'sm' }}
                    fontWeight="medium"
                    color="neutral.950"
                    mb={1}
                    display="block"
                  >
                    {t('language.secondaryLanguage')}
                  </Text>
                  <SearchableSelect
                    options={AVAILABLE_LANGUAGES}
                    value={secondaryLanguage}
                    onChange={setSecondaryLanguage}
                    placeholder={t('common:select')}
                    width="100%"
                    ariaLabel={t('language.aria.selectSecondaryLanguage')}
                  />
                </Box>
              </SimpleGrid>

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
                  isDisabled={!primaryLanguage}
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
