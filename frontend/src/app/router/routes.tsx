import { createBrowserRouter } from 'react-router-dom'
import { ROUTES } from '@/shared/constants/routes'
import { MainLayout, DashboardLayout } from '@/shared/components/layout'
import { CentralDashboard } from '@/features/dashboard/components/central-dashboard'
import {
  OverviewPage as SuperAdminOverviewPage,
  SystemRulesPage,
  StatesUTsPage,
  AddStateUTPage,
  ViewStateUTPage,
  EditStateUTPage,
  ApiCredentialsPage,
  IngestionMonitorPage,
} from '@/features/super-admin'
import {
  OverviewPage,
  ActivityPage,
  LanguagePage,
  IntegrationPage,
  WaterNormsPage,
  EscalationsPage,
  ThresholdsPage,
  NudgesTemplatePage,
} from '@/features/state-admin'
import { LoginPage, ResetPasswordPage } from '@/features/auth'
import { SignupFlowPage } from '@/features/auth/components/signup/signup-flow-page'
import { ProtectedRoute, RedirectIfAuthenticated } from '@/shared/components/routing/ProtectedRoute'
import { AUTH_ROLES } from '@/shared/constants/auth'
import { NotFoundPage } from '@/shared/components/common'

import { Box, Heading, Text } from '@chakra-ui/react'

export const router = createBrowserRouter([
  // Public dashboards
  {
    path: ROUTES.DASHBOARD,
    element: (
      <DashboardLayout>
        <CentralDashboard />
      </DashboardLayout>
    ),
  },
  {
    path: '/states/:stateId',
    element: (
      <DashboardLayout>
        <Box p={6}>
          <Heading fontSize="2xl" fontWeight="bold">
            State Dashboard
          </Heading>
          <Text color="gray.600">State dashboard coming soon...</Text>
        </Box>
      </DashboardLayout>
    ),
  },
  {
    path: '/zones/:zoneId',
    element: (
      <DashboardLayout>
        <Box p={6}>
          <Heading fontSize="2xl" fontWeight="bold">
            Zone Dashboard
          </Heading>
          <Text color="gray.600">Zone dashboard coming soon...</Text>
        </Box>
      </DashboardLayout>
    ),
  },

  // Auth
  {
    path: ROUTES.LOGIN,
    element: (
      <RedirectIfAuthenticated>
        <LoginPage />
      </RedirectIfAuthenticated>
    ),
  },

  // Auth
  {
    path: ROUTES.SIGNUP,
    element: (
      <RedirectIfAuthenticated>
        <SignupFlowPage />
      </RedirectIfAuthenticated>
    ),
  },
  {
    path: ROUTES.CREATE_PASSWORD,
    element: (
      <RedirectIfAuthenticated>
        <SignupFlowPage initialStep="createPassword" />
      </RedirectIfAuthenticated>
    ),
  },
  {
    path: ROUTES.CREDENTIALS,
    element: (
      <RedirectIfAuthenticated>
        <SignupFlowPage initialStep="credentials" />
      </RedirectIfAuthenticated>
    ),
  },
  {
    path: ROUTES.RESET_PASSWORD,
    element: (
      <RedirectIfAuthenticated>
        <ResetPasswordPage />
      </RedirectIfAuthenticated>
    ),
  },
  // Super Admin routes
  {
    path: ROUTES.SUPER_ADMIN,
    element: (
      <ProtectedRoute allowedRoles={[AUTH_ROLES.SUPER_ADMIN]}>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <SuperAdminOverviewPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_SYSTEM_RULES,
        element: <SystemRulesPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_STATES_UTS,
        element: <StatesUTsPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_STATES_UTS_ADD,
        element: <AddStateUTPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_STATES_UTS_VIEW,
        element: <ViewStateUTPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_STATES_UTS_EDIT,
        element: <EditStateUTPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_API_CREDENTIALS,
        element: <ApiCredentialsPage />,
      },
      {
        path: ROUTES.SUPER_ADMIN_INGESTION_MONITOR,
        element: <IngestionMonitorPage />,
      },
    ],
  },

  // State Admin routes
  {
    path: ROUTES.STATE_ADMIN,
    element: (
      <ProtectedRoute allowedRoles={[AUTH_ROLES.STATE_ADMIN]}>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <OverviewPage />,
      },

      {
        path: ROUTES.STATE_ADMIN_LANGUAGE,
        element: <LanguagePage />,
      },
      {
        path: ROUTES.STATE_ADMIN_WATER_NORMS,
        element: <WaterNormsPage />,
      },
      {
        path: ROUTES.STATE_ADMIN_INTEGRATION,
        element: <IntegrationPage />,
      },
      {
        path: ROUTES.STATE_ADMIN_ESCALATIONS,
        element: <EscalationsPage />,
      },
      {
        path: ROUTES.STATE_ADMIN_THRESHOLDS,
        element: <ThresholdsPage />,
      },
      {
        path: ROUTES.STATE_ADMIN_NUDGES,
        element: <NudgesTemplatePage />,
      },
      {
        path: ROUTES.STATE_ADMIN_API_INGESTION,
        element: (
          <Box p={6}>
            <Heading fontSize="2xl" fontWeight="bold">
              API Ingestion
            </Heading>
            <Text color="gray.600">Coming soon...</Text>
          </Box>
        ),
      },
      {
        path: ROUTES.STATE_ADMIN_OPERATOR_SYNC,
        element: (
          <Box p={6}>
            <Heading fontSize="2xl" fontWeight="bold">
              Operator Sync
            </Heading>
            <Text color="gray.600">Coming soon...</Text>
          </Box>
        ),
      },
      {
        path: ROUTES.STATE_ADMIN_ACTIVITY,
        element: <ActivityPage />,
      },
    ],
  },

  {
    path: '*',
    element: <NotFoundPage />,
  },
])
