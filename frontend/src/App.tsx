import { useEffect } from 'react'
import { RouterProvider } from 'react-router-dom'
import { QueryProvider } from '@/app/providers/query-provider'
import { ErrorBoundary } from '@/shared/components/common/error-boundary'
import { router } from '@/app/router'
import { useAuthStore } from '@/app/store'

// Initialize i18n
import '@/app/i18n'

function App() {
  const bootstrap = useAuthStore((state) => state.bootstrap)

  useEffect(() => {
    void bootstrap()
  }, [bootstrap])

  return (
    <ErrorBoundary>
      <QueryProvider>
        <RouterProvider router={router} />
      </QueryProvider>
    </ErrorBoundary>
  )
}

export default App
