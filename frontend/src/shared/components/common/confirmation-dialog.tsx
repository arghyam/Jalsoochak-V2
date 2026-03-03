import { Dialog } from './dialog'
import { VStack, Text, HStack, Button } from '@chakra-ui/react'

interface ConfirmationDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  isLoading?: boolean
}

export function ConfirmationDialog({
  open,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  isLoading = false,
}: ConfirmationDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} title={title} maxWidth="sm">
      <VStack spacing={4} align="stretch">
        <Text color="gray.600">{message}</Text>

        <HStack justify="flex-end" spacing={3}>
          <Button variant="outline" onClick={onClose} isDisabled={isLoading}>
            {cancelLabel}
          </Button>

          <Button
            colorScheme="red"
            onClick={onConfirm}
            isDisabled={isLoading}
            isLoading={isLoading}
          >
            {isLoading ? 'Processing...' : confirmLabel}
          </Button>
        </HStack>
      </VStack>
    </Dialog>
  )
}
