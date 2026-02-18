import { useState, useEffect, useMemo, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Box,
  Heading,
  Text,
  Flex,
  SimpleGrid,
  Input,
  Button,
  HStack,
  Icon,
  FormControl,
  FormLabel,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { EditIcon } from '@chakra-ui/icons'
import { Toggle, ToastContainer } from '@/shared/components/common'
import { useToast } from '@/shared/hooks/use-toast'
import { getStateUTById, updateStateUT, updateStateUTStatus } from '../../services/mock-data'
import type { StateUT } from '../../types/states-uts'
import { ROUTES } from '@/shared/constants/routes'

export function EditStateUTPage() {
  const { t } = useTranslation(['super-admin', 'common'])
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const toast = useToast()

  const [originalState, setOriginalState] = useState<StateUT | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false)

  // Form state
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phone, setPhone] = useState('')
  const [secondaryEmail, setSecondaryEmail] = useState('')
  const [contactNumber, setContactNumber] = useState('')
  const [status, setStatus] = useState<'active' | 'inactive'>('active')

  useEffect(() => {
    document.title = `${t('statesUts.editTitle')} | JalSoochak`
  }, [t])

  const fetchState = useCallback(async (stateId: string) => {
    setIsLoading(true)
    try {
      const data = await getStateUTById(stateId)
      if (data) {
        setOriginalState(data)
        setFirstName(data.admin.firstName)
        setLastName(data.admin.lastName)
        setPhone(data.admin.phone)
        setSecondaryEmail(data.admin.secondaryEmail ?? '')
        setContactNumber(data.admin.contactNumber ?? '')
        setStatus(data.status)
      }
    } catch (error) {
      console.error('Failed to fetch state:', error)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (id) {
      fetchState(id)
    }
  }, [id, fetchState])

  // Validation
  const isValidEmail = (emailStr: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(emailStr)
  }

  const isValidPhone = (phoneStr: string): boolean => {
    const phoneRegex = /^\d{10}$/
    return phoneRegex.test(phoneStr)
  }

  const isFormValid = useMemo(() => {
    const requiredFieldsValid =
      firstName.trim() !== '' && lastName.trim() !== '' && phone.trim() !== ''

    const phoneValid = isValidPhone(phone)

    // Optional fields validation (if provided)
    const secondaryEmailValid = secondaryEmail === '' || isValidEmail(secondaryEmail)
    const contactNumberValid = contactNumber === '' || isValidPhone(contactNumber)

    return requiredFieldsValid && phoneValid && secondaryEmailValid && contactNumberValid
  }, [firstName, lastName, phone, secondaryEmail, contactNumber])

  const hasChanges = useMemo(() => {
    if (!originalState) return false

    return (
      firstName !== originalState.admin.firstName ||
      lastName !== originalState.admin.lastName ||
      phone !== originalState.admin.phone ||
      secondaryEmail !== (originalState.admin.secondaryEmail ?? '') ||
      contactNumber !== (originalState.admin.contactNumber ?? '')
    )
  }, [originalState, firstName, lastName, phone, secondaryEmail, contactNumber])

  const handleStatusToggle = async () => {
    if (!originalState || isUpdatingStatus) return

    const newStatus = status === 'active' ? 'inactive' : 'active'
    setIsUpdatingStatus(true)

    try {
      const updated = await updateStateUTStatus(originalState.id, newStatus)
      if (updated) {
        setStatus(newStatus)
        setOriginalState(updated)
        toast.addToast(
          newStatus === 'active'
            ? t('statesUts.messages.activatedSuccess')
            : t('statesUts.messages.deactivatedSuccess'),
          'success'
        )
      }
    } catch (error) {
      console.error('Failed to update status:', error)
      toast.addToast(t('statesUts.messages.failedToUpdateStatus'), 'error')
    } finally {
      setIsUpdatingStatus(false)
    }
  }

  const handleCancel = () => {
    if (id) {
      navigate(ROUTES.SUPER_ADMIN_STATES_UTS_VIEW.replace(':id', id))
    } else {
      navigate(ROUTES.SUPER_ADMIN_STATES_UTS)
    }
  }

  const handleSave = async () => {
    if (!isFormValid || !hasChanges || !id) {
      return
    }

    setIsSaving(true)
    try {
      const updated = await updateStateUT(id, {
        admin: {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          phone: phone.trim(),
          secondaryEmail: secondaryEmail.trim() || undefined,
          contactNumber: contactNumber.trim() || undefined,
        },
      })
      if (updated) {
        toast.addToast(t('common:toast.changesSaved'), 'success')
        setTimeout(() => {
          navigate(ROUTES.SUPER_ADMIN_STATES_UTS_VIEW.replace(':id', id))
        }, 500)
      }
    } catch (error) {
      console.error('Failed to update state:', error)
      toast.addToast(t('common:toast.failedToSave'), 'error')
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('statesUts.editTitle')}
        </Heading>
        <Flex role="status" aria-live="polite" align="center" justify="center" minH="200px">
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  if (!originalState) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('statesUts.editTitle')}
        </Heading>
        <Text color="neutral.600" mt={4}>
          {t('statesUts.messages.notFound')}
        </Text>
      </Box>
    )
  }

  return (
    <Box w="full">
      {/* Page Header with Breadcrumb */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={2}>
          {t('statesUts.editTitle')}
        </Heading>
        <Flex as="nav" aria-label="Breadcrumb" gap={2} flexWrap="wrap">
          <Text
            as="a"
            fontSize="14px"
            lineHeight="21px"
            color="neutral.500"
            cursor="pointer"
            _hover={{ textDecoration: 'underline' }}
            onClick={() => navigate(ROUTES.SUPER_ADMIN_STATES_UTS)}
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && navigate(ROUTES.SUPER_ADMIN_STATES_UTS)}
          >
            {t('statesUts.breadcrumb.manage')}
          </Text>
          <Text fontSize="14px" lineHeight="21px" color="neutral.500" aria-hidden="true">
            /
          </Text>
          <Text fontSize="14px" lineHeight="21px" color="#26272B" aria-current="page">
            {t('statesUts.breadcrumb.edit')}
          </Text>
        </Flex>
      </Box>

      {/* Form Card */}
      <Box
        as="form"
        role="form"
        aria-label={t('statesUts.editTitle')}
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.200"
        borderRadius="12px"
        w="full"
        minH={{ base: 'auto', lg: 'calc(100vh - 180px)' }}
        py={6}
        px={{ base: 3, md: 4 }}
        onSubmit={(e: React.FormEvent) => {
          e.preventDefault()
          handleSave()
        }}
      >
        <Flex
          direction="column"
          h="full"
          justify="space-between"
          minH={{ base: 'auto', lg: 'calc(100vh - 232px)' }}
        >
          <Box>
            {/* State/UT Details Section */}
            <Flex justify="space-between" align="flex-start" mb={4}>
              <Heading as="h2" size="h3" fontWeight="400" id="state-details-heading">
                {t('statesUts.details.title')}
              </Heading>
              <Icon as={EditIcon} boxSize={5} cursor="not-allowed" h={5} w={5} aria-hidden="true" />
            </Flex>
            <SimpleGrid
              columns={{ base: 1, lg: 2 }}
              spacing={6}
              mb={7}
              aria-labelledby="state-details-heading"
            >
              <FormControl>
                <FormLabel textStyle="h10" color="neutral.400" mb={1}>
                  {t('statesUts.details.name')}
                </FormLabel>
                <Input
                  value={originalState.name}
                  isReadOnly
                  isDisabled
                  bg="neutral.50"
                  borderColor="neutral.200"
                  color="neutral.400"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  aria-readonly="true"
                />
              </FormControl>
              <FormControl>
                <FormLabel textStyle="h10" color="neutral.400" mb={1}>
                  {t('statesUts.details.code')}
                </FormLabel>
                <Input
                  value={originalState.code}
                  isReadOnly
                  isDisabled
                  bg="neutral.50"
                  borderColor="neutral.200"
                  color="neutral.400"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  aria-readonly="true"
                />
              </FormControl>
            </SimpleGrid>

            {/* State Admin Details Section */}
            <Heading as="h2" size="h3" fontWeight="400" mb={4} id="admin-details-heading">
              {t('statesUts.adminDetails.title')}
            </Heading>
            <SimpleGrid
              columns={{ base: 1, lg: 2 }}
              spacing={6}
              mb={7}
              aria-labelledby="admin-details-heading"
            >
              <FormControl isRequired>
                <FormLabel textStyle="h10" mb={1}>
                  {t('statesUts.adminDetails.firstName')}
                </FormLabel>
                <Input
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder={t('common:enter')}
                  borderColor="neutral.200"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  _placeholder={{ color: 'neutral.400' }}
                  aria-required="true"
                />
              </FormControl>
              <FormControl isRequired>
                <FormLabel textStyle="h10" mb={1}>
                  {t('statesUts.adminDetails.lastName')}
                </FormLabel>
                <Input
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  placeholder={t('common:enter')}
                  borderColor="neutral.200"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  _placeholder={{ color: 'neutral.400' }}
                  aria-required="true"
                />
              </FormControl>
              <FormControl>
                <FormLabel textStyle="h10" color="neutral.400" mb={1}>
                  {t('statesUts.adminDetails.email')}
                </FormLabel>
                <Input
                  value={originalState.admin.email}
                  isReadOnly
                  isDisabled
                  bg="neutral.50"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  borderColor="neutral.200"
                  color="neutral.400"
                  aria-readonly="true"
                />
              </FormControl>
              <FormControl isRequired>
                <FormLabel textStyle="h10" mb={1}>
                  {t('statesUts.adminDetails.phone')}
                </FormLabel>
                <Input
                  type="tel"
                  value={phone}
                  onChange={(e) => {
                    // Only allow digits
                    const value = e.target.value.replace(/\D/g, '')
                    if (value.length <= 10) {
                      setPhone(value)
                    }
                  }}
                  placeholder="+91"
                  borderColor="neutral.200"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  _placeholder={{ color: 'neutral.400' }}
                  aria-required="true"
                  inputMode="numeric"
                />
              </FormControl>
              <FormControl>
                <FormLabel textStyle="h10" mb={1}>
                  {t('statesUts.adminDetails.secondaryEmail')}
                </FormLabel>
                <Input
                  type="email"
                  value={secondaryEmail}
                  onChange={(e) => setSecondaryEmail(e.target.value)}
                  placeholder={t('common:enter')}
                  borderColor="neutral.200"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  _placeholder={{ color: 'neutral.400' }}
                />
              </FormControl>
              <FormControl>
                <FormLabel textStyle="h10" mb={1}>
                  {t('statesUts.adminDetails.contactNumber')}
                </FormLabel>
                <Input
                  type="tel"
                  value={contactNumber}
                  onChange={(e) => {
                    // Only allow digits
                    const value = e.target.value.replace(/\D/g, '')
                    if (value.length <= 10) {
                      setContactNumber(value)
                    }
                  }}
                  placeholder={t('common:enter')}
                  borderColor="neutral.200"
                  h={9}
                  maxW={{ base: '100%', lg: '486px' }}
                  _placeholder={{ color: 'neutral.400' }}
                  inputMode="numeric"
                />
              </FormControl>
            </SimpleGrid>

            {/* Status Section */}
            <Heading as="h2" size="h3" fontWeight="400" mb={4} id="status-heading">
              {t('statesUts.statusSection.title')}
            </Heading>
            <Flex align="center" gap={2} h={6} aria-labelledby="status-heading">
              <Text textStyle="h10" id="activated-label">
                {t('statesUts.statusSection.activated')}
              </Text>
              <Toggle
                isChecked={status === 'active'}
                onChange={handleStatusToggle}
                isDisabled={isUpdatingStatus}
                aria-labelledby="activated-label"
              />
            </Flex>
          </Box>

          {/* Action Buttons */}
          <HStack
            spacing={3}
            justify={{ base: 'stretch', sm: 'flex-end' }}
            mt={6}
            flexDirection={{ base: 'column-reverse', sm: 'row' }}
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
              type="submit"
              variant="primary"
              size="md"
              width={{ base: 'full', sm: '174px' }}
              isLoading={isSaving}
              isDisabled={!isFormValid || !hasChanges}
            >
              {t('common:button.saveChanges')}
            </Button>
          </HStack>
        </Flex>
      </Box>

      {/* Toast Container */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
    </Box>
  )
}
