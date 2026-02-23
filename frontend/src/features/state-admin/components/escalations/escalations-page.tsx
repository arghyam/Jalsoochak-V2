import { useState, useEffect, useRef } from 'react'
import {
  Box,
  Text,
  Button,
  Flex,
  SimpleGrid,
  IconButton,
  HStack,
  Heading,
  Spinner,
  Stack,
  useBreakpointValue,
} from '@chakra-ui/react'
import { FiEdit } from 'react-icons/fi'
import { MdDeleteOutline } from 'react-icons/md'
import { useTranslation } from 'react-i18next'
import {
  getMockEscalations,
  getMockEscalationById,
  saveMockEscalation,
  updateMockEscalation,
  deleteMockEscalation,
} from '../../services/mock-data'
import type { Escalation, EscalationLevel } from '../../types/escalations'
import { AVAILABLE_ALERT_TYPES, AVAILABLE_ROLES, AVAILABLE_HOURS } from '../../types/escalations'
import { useToast } from '@/shared/hooks/use-toast'
import { ToastContainer, SearchableSelect, ConfirmationDialog } from '@/shared/components/common'
import { IoAddOutline } from 'react-icons/io5'

type ViewMode = 'list' | 'add' | 'edit'

export function EscalationsPage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const levelIdCounterRef = useRef(0)
  const generateLevelId = () => `level-${Date.now()}-${++levelIdCounterRef.current}`
  const [viewMode, setViewMode] = useState<ViewMode>('list')
  const [escalations, setEscalations] = useState<Escalation[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const showAddButtonText = useBreakpointValue({ base: false, sm: true }) ?? true

  // Form state
  const [alertType, setAlertType] = useState('')
  const [levels, setLevels] = useState<EscalationLevel[]>([
    {
      id: `level-${Date.now()}`,
      levelNumber: 1,
      targetRole: '',
      escalateAfterHours: 0,
    },
  ])

  const toast = useToast()

  useEffect(() => {
    document.title = `${t('escalations.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    fetchEscalations()
  }, [])

  const fetchEscalations = async () => {
    setIsLoading(true)
    try {
      const data = await getMockEscalations()
      setEscalations(data)
    } catch (error) {
      console.error('Failed to fetch escalations:', error)
      toast.addToast(t('escalations.messages.failedToLoad'), 'error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleAddNewClick = () => {
    setViewMode('add')
    setAlertType('')
    setLevels([
      {
        id: `level-${Date.now()}`,
        levelNumber: 1,
        targetRole: '',
        escalateAfterHours: 0,
      },
    ])
  }

  const handleEditClick = async (id: string) => {
    try {
      const escalation = await getMockEscalationById(id)
      if (escalation) {
        setEditingId(id)
        setAlertType(escalation.alertType)
        setLevels(escalation.levels)
        setViewMode('edit')
      } else {
        toast.addToast(t('escalations.messages.notFound'), 'error')
      }
    } catch (error) {
      console.error('Failed to fetch escalation:', error)
      toast.addToast(t('escalations.messages.failedToLoadSingle'), 'error')
    }
  }

  const handleDeleteClick = (id: string) => {
    setDeletingId(id)
  }

  const handleConfirmDelete = async () => {
    if (!deletingId) return

    setIsDeleting(true)
    try {
      await deleteMockEscalation(deletingId)
      await fetchEscalations()
      toast.addToast(t('escalations.messages.deletedSuccess'), 'success')
      setDeletingId(null)
    } catch (error) {
      console.error('Failed to delete escalation:', error)
      toast.addToast(t('escalations.messages.failedToDelete'), 'error')
    } finally {
      setIsDeleting(false)
    }
  }

  const handleCancelDelete = () => {
    setDeletingId(null)
  }

  const handleCancel = () => {
    setViewMode('list')
    setEditingId(null)
    setAlertType('')
    setLevels([
      {
        id: `level-${Date.now()}`,
        levelNumber: 1,
        targetRole: '',
        escalateAfterHours: 0,
      },
    ])
  }

  const handleSave = async () => {
    if (!alertType) {
      toast.addToast(t('escalations.messages.selectAlertType'), 'error')
      return
    }

    // Validate levels
    for (const level of levels) {
      if (!level.targetRole || level.escalateAfterHours <= 0) {
        toast.addToast(t('escalations.messages.invalidLevels'), 'error')
        return
      }
    }

    setIsSaving(true)
    try {
      if (viewMode === 'add') {
        await saveMockEscalation({ alertType, levels })
        toast.addToast(t('escalations.messages.addedSuccess'), 'success')
      } else if (viewMode === 'edit' && editingId) {
        await updateMockEscalation(editingId, { alertType, levels })
        toast.addToast(t('common:toast.changesSavedShort'), 'success')
      }
      await fetchEscalations()
      setViewMode('list')
      setEditingId(null)
    } catch (error) {
      console.error('Failed to save escalation:', error)
      toast.addToast(t('escalations.messages.failedToSave'), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  const handleAddLevel = () => {
    const hasUnfilledLevel = levels.some(
      (level) => !level.targetRole || level.escalateAfterHours <= 0
    )

    if (hasUnfilledLevel) {
      toast.addToast(t('escalations.messages.fillExisting'), 'error')
      return
    }

    const newLevel: EscalationLevel = {
      id: generateLevelId(),
      levelNumber: levels.length + 1,
      targetRole: '',
      escalateAfterHours: 0,
    }
    setLevels([...levels, newLevel])
  }

  const handleRemoveLevel = (id: string) => {
    const updatedLevels = levels.filter((l) => l.id !== id)
    const renumberedLevels = updatedLevels.map((level, index) => ({
      ...level,
      levelNumber: index + 1,
    }))
    setLevels(renumberedLevels)
  }

  const handleLevelChange = (id: string, field: keyof EscalationLevel, value: string | number) => {
    setLevels(levels.map((level) => (level.id === id ? { ...level, [field]: value } : level)))
  }

  const getRoleLabel = (value: string) => {
    const role = AVAILABLE_ROLES.find((r) => r.value === value)
    return role ? role.label : value
  }

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={6}>
          {t('escalations.title')}
        </Heading>
        <Flex align="center" role="status" aria-live="polite" aria-busy="true">
          <Spinner size="md" color="primary.500" mr={3} />
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  // List View
  if (viewMode === 'list') {
    return (
      <Box w="full">
        {/* Page Header */}
        <Box mb={5}>
          <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
            {t('escalations.title')}
          </Heading>
        </Box>

        {/* All Escalations Section */}
        <Box
          as="section"
          aria-labelledby="escalations-heading"
          bg="neutral.25"
          borderWidth="0.5px"
          borderColor="neutral.100"
          borderRadius={{ base: 'lg', md: 'xl' }}
          w="full"
          minH={{ base: 'auto', lg: 'calc(100vh - 148px)' }}
          py={{ base: 4, md: 6 }}
          px={4}
        >
          <Flex
            justify="space-between"
            align="center"
            mb={4}
            flexDirection={{ base: 'column', sm: 'row' }}
            gap={{ base: 3, sm: 0 }}
          >
            <Heading
              as="h2"
              id="escalations-heading"
              size="h3"
              fontWeight="400"
              fontSize={{ base: 'md', md: 'xl' }}
            >
              {t('escalations.allEscalations')}
            </Heading>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleAddNewClick}
              fontSize="14px"
              fontWeight="600"
              h="32px"
              px="12px"
              py="8px"
              gap={1}
              w={{ base: 'full', sm: 'auto' }}
            >
              <IoAddOutline size={24} aria-hidden="true" />
              {showAddButtonText
                ? t('escalations.addNewEscalationType')
                : t('escalations.addNewShort')}
            </Button>
          </Flex>

          {/* Escalation Cards Grid */}
          <SimpleGrid
            columns={{ base: 1, lg: 2 }}
            spacing={{ base: 4, md: 7 }}
            w="full"
            maxW="1200px"
          >
            {escalations.map((escalation) => (
              <Box
                key={escalation.id}
                as="article"
                aria-label={t('escalations.aria.escalationCard', { name: escalation.name })}
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius={{ base: 'lg', md: 'xl' }}
                bg="neutral.50"
                py={{ base: 4, md: 6 }}
                px={4}
                position="relative"
              >
                {/* Card Header with Actions */}
                <Flex justify="space-between" align="flex-start" mb={4}>
                  <Heading
                    as="h3"
                    size="h3"
                    fontWeight="400"
                    fontSize={{ base: 'md', md: 'xl' }}
                    color="neutral.950"
                  >
                    {escalation.name}
                  </Heading>
                  <HStack spacing={1}>
                    <IconButton
                      aria-label={t('escalations.aria.editEscalation', { name: escalation.name })}
                      icon={<FiEdit size={20} aria-hidden="true" />}
                      variant="ghost"
                      size="sm"
                      color="neutral.400"
                      _hover={{ bg: 'primary.50', color: 'primary.500' }}
                      onClick={() => handleEditClick(escalation.id)}
                      h={5}
                      w={5}
                      minW={5}
                    />
                    <IconButton
                      aria-label={t('escalations.aria.deleteEscalation', { name: escalation.name })}
                      icon={<MdDeleteOutline size={20} aria-hidden="true" />}
                      variant="ghost"
                      size="sm"
                      color="neutral.400"
                      _hover={{ bg: 'error.50', color: 'error.500' }}
                      onClick={() => handleDeleteClick(escalation.id)}
                      h={5}
                      w={5}
                      minW={5}
                    />
                  </HStack>
                </Flex>

                {/* Escalation Levels */}
                <Stack spacing={3}>
                  {escalation.levels.map((level) => (
                    <Flex
                      key={level.id}
                      justify="space-between"
                      align={{ base: 'flex-start', sm: 'center' }}
                      flexDirection={{ base: 'column', sm: 'row' }}
                      gap={{ base: 1, sm: 0 }}
                    >
                      <Text fontSize={{ base: '12px', md: '14px' }}>
                        {t('escalations.levelDisplay', {
                          number: level.levelNumber,
                          role: getRoleLabel(level.targetRole),
                        })}
                      </Text>
                      <Text fontSize={{ base: '12px', md: '14px' }}>
                        {t('escalations.escalateAfterHours', { hours: level.escalateAfterHours })}
                      </Text>
                    </Flex>
                  ))}
                </Stack>
              </Box>
            ))}
          </SimpleGrid>
        </Box>

        {/* Delete Confirmation Dialog */}
        <ConfirmationDialog
          open={deletingId !== null}
          onClose={handleCancelDelete}
          onConfirm={handleConfirmDelete}
          title={t('escalations.messages.confirmDeleteTitle')}
          message={t('escalations.messages.confirmDelete')}
          confirmLabel={t('common:button.delete')}
          cancelLabel={t('common:button.cancel')}
          isLoading={isDeleting}
        />

        {/* Toast Container */}
        <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
      </Box>
    )
  }

  // Add/Edit Form View
  return (
    <Box w="full">
      {/* Page Header */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('escalations.title')}
        </Heading>
      </Box>

      {/* Form Card */}
      <Box
        as="section"
        aria-labelledby="escalation-form-heading"
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
          aria-label={t('escalations.aria.formLabel')}
          direction="column"
          w="full"
          h="full"
          justify="space-between"
          minH={{ base: 'auto', lg: 'calc(100vh - 200px)' }}
          gap={{ base: 6, lg: 0 }}
        >
          <Flex direction="column" gap={3}>
            {/* Card Header with Edit Icon */}
            <Flex justify="space-between" align="center" mb={1}>
              <Heading
                as="h2"
                id="escalation-form-heading"
                size="h3"
                fontWeight="400"
                fontSize={{ base: 'md', md: 'xl' }}
              >
                {t('escalations.escalationRules')}
              </Heading>
              {viewMode === 'edit' && (
                <IconButton
                  aria-label={t('escalations.aria.editMode')}
                  icon={<FiEdit size={20} aria-hidden="true" />}
                  variant="ghost"
                  size="sm"
                  color="neutral.950"
                  h={6}
                  w={6}
                  minW={6}
                  cursor="default"
                  _hover={{ bg: 'transparent' }}
                />
              )}
            </Flex>

            {/* Alert Type Selection */}
            <Box>
              <Text
                as="label"
                fontSize={{ base: 'xs', md: 'sm' }}
                fontWeight="medium"
                color="neutral.950"
                mb={1}
                display="block"
              >
                {t('escalations.typeOfAlert')}
                <Text as="span" color="error.500" ml={1}>
                  *
                </Text>
              </Text>
              <SearchableSelect
                options={AVAILABLE_ALERT_TYPES}
                value={alertType}
                onChange={setAlertType}
                placeholder={t('common:select')}
                width={{ base: '100%', lg: '350px', xl: '486px' }}
                ariaLabel={t('escalations.aria.selectAlertType')}
              />
            </Box>

            {/* Escalation Levels */}
            <Flex
              justify="space-between"
              align="flex-start"
              direction="row"
              gap={{ base: 4, lg: 6 }}
            >
              {/* Left Group: Level of Escalation */}
              <Box flex={{ base: 1, lg: 1 }}>
                <Text
                  as="label"
                  fontSize={{ base: 'xs', md: 'sm' }}
                  fontWeight="medium"
                  color="neutral.950"
                  mb={1}
                  display="block"
                >
                  {t('escalations.levelOfEscalation')}
                  <Text as="span" color="error.500" ml={1}>
                    *
                  </Text>
                </Text>
                <Stack spacing={3}>
                  {levels.map((level, index) => (
                    <Flex key={level.id} gap={3} align="center" direction="row">
                      <Text
                        fontSize={{ base: 'xs', md: 'sm' }}
                        fontWeight="medium"
                        color="neutral.950"
                        whiteSpace="nowrap"
                        w={{ base: 'auto' }}
                      >
                        {t('escalations.level', { number: index + 1 })}
                      </Text>
                      <SearchableSelect
                        options={AVAILABLE_ROLES}
                        value={level.targetRole}
                        onChange={(value) => handleLevelChange(level.id, 'targetRole', value)}
                        placeholder={t('common:select')}
                        width={{ base: '100%', lg: '294px', xl: '430px' }}
                        ariaLabel={t('escalations.aria.selectRole', { number: index + 1 })}
                      />
                    </Flex>
                  ))}
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={handleAddLevel}
                    w={{ base: 'auto', sm: '152px' }}
                    fontSize="14px"
                    fontWeight="400"
                    h="32px"
                    gap={1}
                    mt={1}
                  >
                    <IoAddOutline size={24} aria-hidden="true" />
                    {t('escalations.addNewLevel')}
                  </Button>
                </Stack>
              </Box>

              {/* Right Group: Escalate after (hours) */}
              <Box flex={{ base: 1, lg: 1 }}>
                <Text
                  as="label"
                  fontSize={{ base: 'xs', md: 'sm' }}
                  fontWeight="medium"
                  color="neutral.950"
                  mb={1}
                  display="block"
                >
                  {t('escalations.escalateAfter')}
                  <Text as="span" color="error.500" ml={1}>
                    *
                  </Text>
                </Text>
                <Stack spacing={3}>
                  {levels.map((level, index) => (
                    <Flex key={level.id} gap={2} align="center">
                      <SearchableSelect
                        options={AVAILABLE_HOURS}
                        value={String(level.escalateAfterHours)}
                        onChange={(value) =>
                          handleLevelChange(level.id, 'escalateAfterHours', Number(value))
                        }
                        placeholder={t('common:select')}
                        width={{ base: '100%', lg: '294px', xl: '486px' }}
                        ariaLabel={t('escalations.aria.selectHours', { number: index + 1 })}
                      />
                      {levels.length > 1 && (
                        <IconButton
                          aria-label={t('escalations.aria.deleteLevel', {
                            number: index + 1,
                          })}
                          icon={<MdDeleteOutline size={24} aria-hidden="true" />}
                          variant="ghost"
                          size="sm"
                          color="neutral.400"
                          onClick={() => handleRemoveLevel(level.id)}
                          h="36px"
                          minW="36px"
                          _hover={{ bg: 'error.50', color: 'error.500' }}
                        />
                      )}
                    </Flex>
                  ))}
                </Stack>
              </Box>
            </Flex>
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
                !alertType || levels.some((l) => !l.targetRole || l.escalateAfterHours <= 0)
              }
            >
              {viewMode === 'add'
                ? t('escalations.buttons.addEscalation')
                : t('common:button.saveChanges')}
            </Button>
          </HStack>
        </Flex>
      </Box>

      {/* Toast Container */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
    </Box>
  )
}
