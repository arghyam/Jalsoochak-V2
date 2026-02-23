import { useState, useEffect, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Box, Heading, Text, Flex, SimpleGrid, IconButton } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { EditIcon } from '@chakra-ui/icons'
import { Toggle, ToastContainer } from '@/shared/components/common'
import { useToast } from '@/shared/hooks/use-toast'
import { getStateUTById, updateStateUTStatus } from '../../services/mock-data'
import type { StateUT } from '../../types/states-uts'
import { ROUTES } from '@/shared/constants/routes'

export function ViewStateUTPage() {
  const { t } = useTranslation(['super-admin', 'common'])
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const toast = useToast()

  const [state, setState] = useState<StateUT | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false)

  useEffect(() => {
    document.title = `${t('statesUts.viewTitle')} | JalSoochak`
  }, [t])

  const fetchState = useCallback(async (stateId: string) => {
    setIsLoading(true)
    try {
      const data = await getStateUTById(stateId)
      setState(data)
    } catch (error) {
      console.error('Failed to fetch state:', error)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!id) {
      setState(null)
      setIsLoading(false)
      return
    }
    fetchState(id)
  }, [id, fetchState])

  const handleStatusToggle = async () => {
    if (!state || isUpdatingStatus) return

    const newStatus = state.status === 'active' ? 'inactive' : 'active'
    setIsUpdatingStatus(true)

    try {
      const updated = await updateStateUTStatus(state.id, newStatus)
      if (updated) {
        setState(updated)
        toast.addToast(
          newStatus === 'active'
            ? t('statesUts.messages.activatedSuccess')
            : t('statesUts.messages.deactivatedSuccess'),
          'success'
        )
      } else {
        toast.addToast(t('statesUts.messages.notFound'), 'error')
      }
    } catch (error) {
      console.error('Failed to update status:', error)
      toast.addToast(t('statesUts.messages.failedToUpdateStatus'), 'error')
    } finally {
      setIsUpdatingStatus(false)
    }
  }

  const handleEdit = () => {
    if (id) {
      navigate(ROUTES.SUPER_ADMIN_STATES_UTS_EDIT.replace(':id', id))
    }
  }

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('statesUts.title')}
        </Heading>
        <Flex role="status" aria-live="polite" align="center" justify="center" minH="200px">
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  if (!state) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('statesUts.title')}
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
          {t('statesUts.title')}
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
            {t('statesUts.breadcrumb.view')}
          </Text>
        </Flex>
      </Box>

      {/* Details Card */}
      <Box
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.200"
        borderRadius="12px"
        w="full"
        minH={{ base: 'auto', lg: 'calc(100vh - 180px)' }}
        py={6}
        px={{ base: 3, md: 4 }}
      >
        {/* State/UT Details Section */}
        <Flex justify="space-between" align="flex-start" mb={4}>
          <Heading as="h2" size="h3" fontWeight="400" id="state-details-heading">
            {t('statesUts.details.title')}
          </Heading>
          <IconButton
            aria-label={`${t('statesUts.aria.editStateUt')} ${state.name}`}
            icon={<EditIcon boxSize={5} />}
            variant="ghost"
            size="sm"
            color="neutral.600"
            _hover={{ color: 'primary.500', bg: 'transparent' }}
            onClick={handleEdit}
          />
        </Flex>
        <SimpleGrid
          columns={{ base: 1, lg: 2 }}
          spacing={6}
          mb={7}
          aria-labelledby="state-details-heading"
        >
          <Box>
            <Text textStyle="h10" fontWeight="500" mb={1}>
              {t('statesUts.details.name')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.name}
            </Text>
          </Box>
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.details.code')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.code}
            </Text>
          </Box>
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
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.adminDetails.firstName')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.admin.firstName}
            </Text>
          </Box>
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.adminDetails.lastName')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.admin.lastName}
            </Text>
          </Box>
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.adminDetails.email')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.admin.email}
            </Text>
          </Box>
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.adminDetails.phone')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              +91 {state.admin.phone.replace(/(\d{5})(\d{5})/, '$1-$2')}
            </Text>
          </Box>
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.adminDetails.secondaryEmail')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.admin.secondaryEmail || t('common:na')}
            </Text>
          </Box>
          <Box>
            <Text textStyle="h10" mb={1}>
              {t('statesUts.adminDetails.contactNumber')}
            </Text>
            <Text textStyle="h10" fontWeight="400">
              {state.admin.contactNumber || t('common:na')}
            </Text>
          </Box>
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
            isChecked={state.status === 'active'}
            onChange={handleStatusToggle}
            isDisabled={isUpdatingStatus}
            aria-labelledby="activated-label"
          />
        </Flex>
      </Box>

      {/* Toast Container */}
      <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
    </Box>
  )
}
