import { useState, useEffect } from 'react'
import {
  Box,
  Text,
  Button,
  Flex,
  Input,
  VStack,
  HStack,
  FormControl,
  FormLabel,
  Heading,
  Spinner,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import {
  getMockIntegrationConfiguration,
  saveMockIntegrationConfiguration,
} from '../../services/mock-data'
import type { IntegrationConfiguration } from '../../types/integration'
import { useToast } from '@/shared/hooks/use-toast'
import { ToastContainer } from '@/shared/components/common'

export function IntegrationPage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const [config, setConfig] = useState<IntegrationConfiguration | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  const [whatsappBusinessAccountName, setWhatsappBusinessAccountName] = useState('')
  const [senderPhoneNumber, setSenderPhoneNumber] = useState('')
  const [whatsappBusinessAccountId, setWhatsappBusinessAccountId] = useState('')
  const [apiAccessToken, setApiAccessToken] = useState('')

  const toast = useToast()

  useEffect(() => {
    document.title = `${t('integration.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const data = await getMockIntegrationConfiguration()
        setConfig(data)
        setWhatsappBusinessAccountName(data.whatsappBusinessAccountName)
        setSenderPhoneNumber(data.senderPhoneNumber)
        setWhatsappBusinessAccountId(data.whatsappBusinessAccountId)
        setApiAccessToken(data.apiAccessToken)
      } catch (error) {
        console.error('Failed to fetch integration configuration:', error)
        toast.addToast(t('integration.messages.failedToLoad'), 'error')
      } finally {
        setIsLoading(false)
      }
    }

    fetchConfig()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleCancel = () => {
    if (config) {
      setWhatsappBusinessAccountName(config.whatsappBusinessAccountName)
      setSenderPhoneNumber(config.senderPhoneNumber)
      setWhatsappBusinessAccountId(config.whatsappBusinessAccountId)
      setApiAccessToken(config.apiAccessToken)
    }
  }

  const handleSave = async () => {
    if (
      !whatsappBusinessAccountName ||
      !senderPhoneNumber ||
      !whatsappBusinessAccountId ||
      !apiAccessToken
    ) {
      toast.addToast(t('common:toast.fillAllFields'), 'error')
      return
    }

    setIsSaving(true)
    try {
      const savedConfig = await saveMockIntegrationConfiguration({
        whatsappBusinessAccountName,
        senderPhoneNumber,
        whatsappBusinessAccountId,
        apiAccessToken,
      })
      setConfig(savedConfig)
      toast.addToast(t('common:toast.changesSavedShort'), 'success')
    } catch (error) {
      console.error('Failed to save integration configuration:', error)
      toast.addToast(t('common:toast.failedToSave'), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const hasChanges =
    config &&
    (whatsappBusinessAccountName !== config.whatsappBusinessAccountName ||
      senderPhoneNumber !== config.senderPhoneNumber ||
      whatsappBusinessAccountId !== config.whatsappBusinessAccountId ||
      apiAccessToken !== config.apiAccessToken)

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('integration.title')}
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
          {t('integration.title')}
        </Heading>
      </Box>

      {/* Integration Configuration Card */}
      <Box
        as="section"
        aria-labelledby="integration-heading"
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
          aria-label={t('integration.aria.formLabel')}
          direction="column"
          w="full"
          h="full"
          justify="space-between"
          minH={{ base: 'auto', lg: 'calc(100vh - 200px)' }}
          gap={{ base: 6, lg: 0 }}
        >
          <Flex direction="column" gap={4}>
            <Heading
              as="h2"
              id="integration-heading"
              size="h3"
              fontWeight="400"
              fontSize={{ base: 'md', md: 'xl' }}
            >
              {t('integration.whatsappDetails')}
            </Heading>
            {/* Form Fields */}
            <VStack align="stretch" spacing={3} flex={1}>
              <FormControl isRequired>
                <FormLabel textStyle="h10" fontSize={{ base: 'xs', md: 'sm' }} mb={1}>
                  {t('integration.fields.businessAccountName')}
                </FormLabel>
                <Input
                  placeholder={t('integration.fields.businessAccountNamePlaceholder')}
                  fontSize="14px"
                  fontWeight="400"
                  value={whatsappBusinessAccountName}
                  onChange={(e) => setWhatsappBusinessAccountName(e.target.value)}
                  size="md"
                  h="36px"
                  maxW={{ base: '100%', lg: '486px' }}
                  px={3}
                  py={2}
                  borderColor="neutral.300"
                  borderRadius="4px"
                  aria-label={t('integration.aria.enterBusinessAccountName')}
                  _hover={{ borderColor: 'neutral.400' }}
                  _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                />
              </FormControl>

              <FormControl isRequired>
                <FormLabel textStyle="h10" fontSize={{ base: 'xs', md: 'sm' }} mb={1}>
                  {t('integration.fields.senderPhoneNumber')}
                </FormLabel>
                <Input
                  placeholder={t('integration.fields.senderPhoneNumberPlaceholder')}
                  fontSize="14px"
                  fontWeight="400"
                  value={senderPhoneNumber}
                  onChange={(e) => setSenderPhoneNumber(e.target.value)}
                  size="md"
                  h="36px"
                  maxW={{ base: '100%', lg: '486px' }}
                  px={3}
                  py={2}
                  borderColor="neutral.300"
                  borderRadius="4px"
                  aria-label={t('integration.aria.enterPhoneNumber')}
                  _hover={{ borderColor: 'neutral.400' }}
                  _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                />
              </FormControl>

              <FormControl isRequired>
                <FormLabel textStyle="h10" fontSize={{ base: 'xs', md: 'sm' }} mb={1}>
                  {t('integration.fields.businessAccountId')}
                </FormLabel>
                <Input
                  placeholder={t('common:enter')}
                  fontSize="14px"
                  fontWeight="400"
                  value={whatsappBusinessAccountId}
                  onChange={(e) => setWhatsappBusinessAccountId(e.target.value)}
                  size="md"
                  h="36px"
                  maxW={{ base: '100%', lg: '486px' }}
                  px={3}
                  py={2}
                  borderColor="neutral.300"
                  borderRadius="4px"
                  aria-label={t('integration.aria.enterAccountId')}
                  _hover={{ borderColor: 'neutral.400' }}
                  _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                />
              </FormControl>

              <FormControl isRequired>
                <FormLabel textStyle="h10" fontSize={{ base: 'xs', md: 'sm' }} mb={1}>
                  {t('integration.fields.apiAccessToken')}
                </FormLabel>
                <Input
                  placeholder={t('common:enter')}
                  fontSize="14px"
                  fontWeight="400"
                  type="password"
                  value={apiAccessToken}
                  onChange={(e) => setApiAccessToken(e.target.value)}
                  size="md"
                  h="36px"
                  maxW={{ base: '100%', lg: '486px' }}
                  px={3}
                  py={2}
                  borderColor="neutral.300"
                  borderRadius="4px"
                  aria-label={t('integration.aria.enterApiToken')}
                  _hover={{ borderColor: 'neutral.400' }}
                  _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                />
              </FormControl>
            </VStack>
          </Flex>

          {/* Action Buttons */}
          <HStack
            spacing={3}
            justify={{ base: 'stretch', sm: 'flex-end' }}
            flexDirection={{ base: 'column-reverse', sm: 'row' }}
            mt={4}
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
              isDisabled={
                !whatsappBusinessAccountName ||
                !senderPhoneNumber ||
                !whatsappBusinessAccountId ||
                !apiAccessToken ||
                !hasChanges
              }
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
