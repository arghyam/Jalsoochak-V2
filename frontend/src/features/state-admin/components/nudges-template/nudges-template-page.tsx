import { useState, useEffect } from 'react'
import { Box, Text, Button, Flex, Textarea, Tag, Heading, Spinner, Stack } from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'
import { getMockNudgeTemplates, updateMockNudgeTemplate } from '../../services/mock-data'
import type { NudgeTemplate } from '../../types/nudges'
import { useToast } from '@/shared/hooks/use-toast'
import { ToastContainer, SearchableSelect } from '@/shared/components/common'

interface LanguageOption {
  value: string
  label: string
}

const LANGUAGE_OPTIONS: LanguageOption[] = [
  { value: 'english', label: 'English' },
  { value: 'telugu', label: 'Telugu' },
]

interface TemplateState {
  selectedLanguage: string
  message: string
  originalMessage: string
}

export function NudgesTemplatePage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const [templates, setTemplates] = useState<NudgeTemplate[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [savingTemplateId, setSavingTemplateId] = useState<string | null>(null)
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('')
  const [templateStates, setTemplateStates] = useState<Record<string, TemplateState>>({})

  const toast = useToast()

  useEffect(() => {
    document.title = `${t('nudges.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    fetchTemplates()
  }, [])

  const fetchTemplates = async () => {
    setIsLoading(true)
    try {
      const data = await getMockNudgeTemplates()
      setTemplates(data)
      if (data.length > 0) {
        setSelectedTemplateId(data[0].id)
      }

      // Initialize state for each template with first available language
      const initialStates: Record<string, TemplateState> = {}
      data.forEach((template) => {
        const firstMessage = template.messages[0]
        if (firstMessage) {
          initialStates[template.id] = {
            selectedLanguage: firstMessage.language,
            message: firstMessage.message,
            originalMessage: firstMessage.message,
          }
        }
      })
      setTemplateStates(initialStates)
    } catch (error) {
      console.error('Failed to fetch nudge templates:', error)
      toast.addToast(t('nudges.messages.failedToLoad'), 'error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleLanguageChange = (templateId: string, language: string) => {
    const template = templates.find((t) => t.id === templateId)
    if (!template) return

    setSelectedTemplateId(templateId)

    const messageForLanguage = template.messages.find((m) => m.language === language)
    if (messageForLanguage) {
      setTemplateStates((prev) => ({
        ...prev,
        [templateId]: {
          selectedLanguage: language,
          message: messageForLanguage.message,
          originalMessage: messageForLanguage.message,
        },
      }))
    }
  }

  const handleMessageChange = (templateId: string, message: string) => {
    setSelectedTemplateId(templateId)
    setTemplateStates((prev) => ({
      ...prev,
      [templateId]: {
        ...prev[templateId],
        message,
      },
    }))
  }

  const handleSave = async (templateId: string) => {
    const state = templateStates[templateId]
    if (!state?.message) {
      toast.addToast(t('nudges.messages.enterMessage'), 'error')
      return
    }

    setSavingTemplateId(templateId)
    try {
      const updatedTemplate = await updateMockNudgeTemplate(templateId, {
        language: state.selectedLanguage,
        message: state.message,
      })

      // Update templates list
      setTemplates((prev) => prev.map((t) => (t.id === templateId ? updatedTemplate : t)))

      // Update template state with new original message
      setTemplateStates((prev) => ({
        ...prev,
        [templateId]: {
          ...prev[templateId],
          originalMessage: state.message,
        },
      }))

      toast.addToast(t('common:toast.changesSavedShort'), 'success')
    } catch (error) {
      console.error('Failed to save nudge template:', error)
      toast.addToast(t('nudges.messages.failedToSave'), 'error')
    } finally {
      setSavingTemplateId(null)
    }
  }

  const handleTestSend = (templateId: string) => {
    const state = templateStates[templateId]
    if (!state) return
    // TODO: Implement actual test-send functionality when api is ready
    toast.addToast(t('nudges.messages.testSent'), 'success')
  }

  const hasChanges = (templateId: string): boolean => {
    const state = templateStates[templateId]
    return state ? state.message !== state.originalMessage : false
  }

  const getAvailableLanguages = (template: NudgeTemplate): LanguageOption[] => {
    return LANGUAGE_OPTIONS.filter((opt) => template.messages.some((m) => m.language === opt.value))
  }

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('nudges.title')}
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
          {t('nudges.title')}
        </Heading>
      </Box>

      {/* Templates Container */}
      <Box
        as="section"
        aria-label={t('nudges.aria.formLabel')}
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.100"
        borderRadius={{ base: 'lg', md: 'xl' }}
        w="full"
        minH={{ base: 'auto', lg: 'calc(100vh - 148px)' }}
        py={{ base: 4, md: 6 }}
        px={4}
      >
        <Stack spacing={{ base: 6, md: 7 }}>
          {templates.map((template) => {
            const state = templateStates[template.id]
            const availableLanguages = getAvailableLanguages(template)
            const isSaving = savingTemplateId === template.id

            return (
              <Box
                key={template.id}
                as="article"
                aria-label={t('nudges.aria.templateCard', { name: template.name })}
              >
                <Flex direction="column" gap={4}>
                  {/* Template Header */}
                  <Heading
                    as="h2"
                    size="h3"
                    fontWeight="400"
                    fontSize={{ base: 'md', md: 'xl' }}
                    color="neutral.950"
                  >
                    {template.name}
                  </Heading>

                  {/* Content Section */}
                  <Flex
                    gap={{ base: 4, lg: 6 }}
                    justify="space-between"
                    direction={{ base: 'column', lg: 'row' }}
                  >
                    {/* Left Side - Language Select and Variables */}
                    <Box
                      flex={{ base: 'none', lg: '0 0 auto' }}
                      w={{ base: 'full', lg: '339px', xl: '486px' }}
                    >
                      {/* Language Select */}
                      <Box mb={3}>
                        <Text
                          as="label"
                          textStyle="h10"
                          fontSize={{ base: 'xs', md: 'sm' }}
                          mb={1}
                          display="block"
                        >
                          {t('nudges.language')}
                          <Text as="span" color="error.500" ml={1}>
                            *
                          </Text>
                        </Text>
                        <SearchableSelect
                          options={availableLanguages}
                          value={state?.selectedLanguage || ''}
                          onChange={(value) => handleLanguageChange(template.id, value)}
                          placeholder={t('nudges.selectLanguage')}
                          width={{ base: '100%', xl: '486px' }}
                          textStyle="h10"
                          borderColor="neutral.300"
                          borderRadius="4px"
                          ariaLabel={t('nudges.aria.selectLanguage', { name: template.name })}
                        />
                      </Box>

                      {/* Available Variables */}
                      <Box>
                        <Text textStyle="h10" fontSize={{ base: 'xs', md: 'sm' }} mb={1}>
                          {t('nudges.availableVariables')}
                        </Text>
                        <Flex gap={2} wrap="wrap">
                          {template.availableVariables.map((variable) => (
                            <Tag
                              key={variable}
                              size="md"
                              borderRadius="16px"
                              bg="primary.50"
                              color="primary.500"
                              fontSize={{ base: '10px', md: '12px' }}
                              fontWeight="400"
                              px={2}
                              py={1}
                            >
                              {variable}
                            </Tag>
                          ))}
                        </Flex>
                      </Box>
                    </Box>

                    {/* Right Side - Message */}
                    <Box w={{ base: 'full', lg: '339px', xl: '486px' }}>
                      <Text
                        as="label"
                        textStyle="h10"
                        fontSize={{ base: 'xs', md: 'sm' }}
                        mb={1}
                        display="block"
                      >
                        {t('nudges.message')}
                      </Text>
                      <Box position="relative" w={{ base: 'full', xl: '486px' }}>
                        <Textarea
                          value={state?.message || ''}
                          onChange={(e) => handleMessageChange(template.id, e.target.value)}
                          onFocus={() => setSelectedTemplateId(template.id)}
                          placeholder={t('nudges.messagePlaceholder')}
                          fontSize={{ base: '12px', md: '14px' }}
                          fontWeight="400"
                          w="full"
                          height={{ base: '100px', md: '124px' }}
                          borderColor="neutral.300"
                          borderRadius="6px"
                          resize="none"
                          pr={8}
                          aria-label={t('nudges.aria.enterMessage', { name: template.name })}
                          _hover={{ borderColor: 'neutral.400' }}
                          _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
                          sx={{
                            '&::-webkit-scrollbar': {
                              display: 'none',
                            },
                            scrollbarWidth: 'none',
                            msOverflowStyle: 'none',
                          }}
                        />
                        <ChevronDownIcon
                          position="absolute"
                          right={3}
                          top="50%"
                          transform="translateY(-50%)"
                          boxSize={5}
                          color="neutral.400"
                          pointerEvents="none"
                          aria-hidden="true"
                        />
                      </Box>

                      {/* Action Buttons - Only show for selected template */}
                      {selectedTemplateId === template.id && (
                        <Flex
                          justify={{ base: 'stretch', sm: 'flex-end' }}
                          gap={3}
                          mt={5}
                          direction={{ base: 'column-reverse', sm: 'row' }}
                        >
                          <Button
                            variant="outline"
                            size="md"
                            width={{ base: 'full', sm: '174px' }}
                            onClick={() => handleTestSend(template.id)}
                            borderColor="primary.500"
                            color="primary.500"
                            _hover={{ bg: 'primary.50' }}
                          >
                            {t('nudges.testSend')}
                          </Button>
                          <Button
                            variant="primary"
                            size="md"
                            width={{ base: 'full', sm: '174px' }}
                            onClick={() => handleSave(template.id)}
                            isLoading={isSaving}
                            isDisabled={!state?.message || !hasChanges(template.id)}
                          >
                            {t('common:button.save')}
                          </Button>
                        </Flex>
                      )}
                    </Box>
                  </Flex>
                </Flex>
              </Box>
            )
          })}
        </Stack>
      </Box>

      {/* Toast Container */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
    </Box>
  )
}
