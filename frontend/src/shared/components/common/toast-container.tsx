import { Box, Stack } from '@chakra-ui/react'
import { Toast, type ToastType } from './toast'

export interface ToastItem {
  id: string
  message: string
  type: ToastType
}

interface ToastContainerProps {
  toasts: ToastItem[]
  onRemove: (id: string) => void
}

export function ToastContainer({ toasts, onRemove }: ToastContainerProps) {
  return (
    <Box position="fixed" bottom={28} right={12} zIndex={50}>
      <Stack spacing={2}>
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            id={toast.id}
            message={toast.message}
            type={toast.type}
            onClose={onRemove}
          />
        ))}
      </Stack>
    </Box>
  )
}
