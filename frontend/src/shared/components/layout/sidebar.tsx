import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import {
  Box,
  Flex,
  Stack,
  Text,
  Icon,
  Image,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { MdOutlineMoving, MdOutlinePlace } from 'react-icons/md'
import { AiOutlineEye, AiOutlineSetting, AiOutlineWarning, AiOutlineApi } from 'react-icons/ai'
import { BiKey } from 'react-icons/bi'
import { FiLogOut } from 'react-icons/fi'
import { IoLanguageOutline, IoWaterOutline } from 'react-icons/io5'
import { HiOutlineTemplate } from 'react-icons/hi'
import { BsPerson, BsListUl } from 'react-icons/bs'
import jalsoochakLogo from '@/assets/media/jalsoochak-logo.svg'
import { useAuthStore } from '@/app/store'
import { ROUTES } from '@/shared/constants/routes'
import { AUTH_ROLES } from '@/shared/constants/auth'

interface SidebarProps {
  onNavClick?: () => void
}

interface NavItem {
  path: string
  labelKey: string
  roles: string[]
  icon?: React.ComponentType<{ className?: string }>
}

const NAV_ITEMS: NavItem[] = [
  // Super Admin navigation
  {
    path: ROUTES.SUPER_ADMIN_OVERVIEW,
    labelKey: 'sidebar.overview',
    roles: [AUTH_ROLES.SUPER_ADMIN],
    icon: AiOutlineEye,
  },
  {
    path: ROUTES.SUPER_ADMIN_SYSTEM_RULES,
    labelKey: 'sidebar.systemRules',
    roles: [AUTH_ROLES.SUPER_ADMIN],
    icon: AiOutlineSetting,
  },
  {
    path: ROUTES.SUPER_ADMIN_STATES_UTS,
    labelKey: 'sidebar.statesUts',
    roles: [AUTH_ROLES.SUPER_ADMIN],
    icon: MdOutlinePlace,
  },
  {
    path: ROUTES.SUPER_ADMIN_API_CREDENTIALS,
    labelKey: 'sidebar.apiCredentials',
    roles: [AUTH_ROLES.SUPER_ADMIN],
    icon: BiKey,
  },
  {
    path: ROUTES.SUPER_ADMIN_INGESTION_MONITOR,
    labelKey: 'sidebar.ingestionMonitor',
    roles: [AUTH_ROLES.SUPER_ADMIN],
    icon: AiOutlineApi,
  },

  // State Admin navigation
  {
    path: ROUTES.STATE_ADMIN_OVERVIEW,
    labelKey: 'sidebar.overview',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: AiOutlineEye,
  },
  {
    path: ROUTES.STATE_ADMIN_LANGUAGE,
    labelKey: 'sidebar.language',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: IoLanguageOutline,
  },
  {
    path: ROUTES.STATE_ADMIN_WATER_NORMS,
    labelKey: 'sidebar.waterNorms',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: IoWaterOutline,
  },
  {
    path: ROUTES.STATE_ADMIN_INTEGRATION,
    labelKey: 'sidebar.integration',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: AiOutlineSetting,
  },
  {
    path: ROUTES.STATE_ADMIN_ESCALATIONS,
    labelKey: 'sidebar.escalations',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: MdOutlineMoving,
  },
  {
    path: ROUTES.STATE_ADMIN_THRESHOLDS,
    labelKey: 'sidebar.thresholds',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: AiOutlineWarning,
  },
  {
    path: ROUTES.STATE_ADMIN_NUDGES,
    labelKey: 'sidebar.nudgesTemplate',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: HiOutlineTemplate,
  },
  {
    path: ROUTES.STATE_ADMIN_API_INGESTION,
    labelKey: 'sidebar.apiIngestion',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: AiOutlineApi,
  },
  {
    path: ROUTES.STATE_ADMIN_OPERATOR_SYNC,
    labelKey: 'sidebar.operatorSync',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: BsPerson,
  },
  {
    path: ROUTES.STATE_ADMIN_ACTIVITY,
    labelKey: 'sidebar.activity',
    roles: [AUTH_ROLES.STATE_ADMIN],
    icon: BsListUl,
  },
]

export function Sidebar({ onNavClick }: SidebarProps) {
  const { t } = useTranslation('common')
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const location = useLocation()
  const navigate = useNavigate()

  const userRole = user?.role
  const visibleNavItems = NAV_ITEMS.filter((item) => userRole && item.roles.includes(userRole))

  const getInitials = (name: string) => {
    const parts = name.split(' ')
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
    }
    return name.substring(0, 2).toUpperCase()
  }

  const handleLogout = async () => {
    try {
      await logout()
    } catch {
      // ignore
    }
    navigate(ROUTES.LOGIN, { replace: true })
  }

  return (
    <Box
      as="aside"
      position={{ base: 'relative', lg: 'fixed' }}
      left={0}
      top={0}
      zIndex={40}
      h="100vh"
      w="224px"
    >
      <Flex
        direction="column"
        h="100vh"
        w="224px"
        bg="white"
        borderRight="1px"
        borderColor="neutral.100"
        py={10}
      >
        {/* Brand Section */}
        <Flex
          h="84px"
          align="center"
          justify="center"
          gap={2}
          borderBottom="1px"
          borderColor="neutral.100"
          px={7}
        >
          <Image
            src={jalsoochakLogo}
            alt={t('sidebar.logoAlt', 'JalSoochak logo')}
            height="84px"
            width="168px"
          />
        </Flex>

        {/* Navigation Section */}
        <Box
          as="nav"
          role="navigation"
          aria-label={t('sidebar.mainNavigation', 'Main navigation')}
          flex={1}
          overflowY="auto"
        >
          <Stack gap={4} px={7} py={4}>
            {visibleNavItems.map((item) => {
              const isActive =
                location.pathname === item.path ||
                (item.path !== ROUTES.SUPER_ADMIN_OVERVIEW &&
                  item.path !== ROUTES.STATE_ADMIN_OVERVIEW &&
                  location.pathname.startsWith(item.path + '/'))
              const ItemIcon = item.icon

              return (
                <RouterLink
                  key={item.path}
                  to={item.path}
                  style={{ textDecoration: 'none' }}
                  onClick={onNavClick}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Flex
                    alignItems="center"
                    gap={2}
                    borderRadius="lg"
                    px={3}
                    py={2}
                    fontSize="sm"
                    fontWeight="medium"
                    transition="all 0.2s"
                    bg={isActive ? 'primary.25' : 'transparent'}
                    color={isActive ? 'primary.700' : 'neutral.950'}
                    minH="44px"
                    _hover={{
                      bg: isActive ? 'primary.25' : 'neutral.100',
                    }}
                  >
                    {ItemIcon && (
                      <Icon as={ItemIcon} boxSize={5} flexShrink={0} aria-hidden="true" />
                    )}
                    <Text isTruncated>{t(item.labelKey)}</Text>
                  </Flex>
                </RouterLink>
              )
            })}
          </Stack>
        </Box>

        {/* Profile Section */}
        <Menu placement="top-start">
          <MenuButton
            w="100%"
            borderTop="1px"
            borderColor="neutral.100"
            px={7}
            py={4}
            cursor="pointer"
            _hover={{ bg: 'neutral.50' }}
          >
            <Flex align="center" gap={3}>
              <Flex
                h="40px"
                w="40px"
                flexShrink={0}
                align="center"
                justify="center"
                borderRadius="full"
                bg="primary.500"
                color="white"
              >
                <Text fontSize="sm" fontWeight="semibold">
                  {user ? getInitials(user.name) : 'U'}
                </Text>
              </Flex>
              <Flex direction="column" minW={0}>
                <Text fontSize="sm" fontWeight="medium" color="neutral.950" isTruncated>
                  {user?.name || 'User'}
                </Text>
              </Flex>
            </Flex>
          </MenuButton>
          <MenuList height="44px" px={7} py={0}>
            <MenuItem
              height="full"
              width="167px"
              px={3}
              py={0}
              gap={2}
              borderRadius={2}
              onClick={handleLogout}
              _hover={{ bg: 'neutral.100' }}
            >
              <FiLogOut />
              <Text fontSize="16px" fontWeight="500">
                {t('sidebar.logout')}
              </Text>
            </MenuItem>
          </MenuList>
        </Menu>
      </Flex>
    </Box>
  )
}
