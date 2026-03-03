import type { ReactNode } from 'react'
import { Box, Flex, Text } from '@chakra-ui/react'

interface KPICardProps {
  title: string
  value: string | number
  icon?: ReactNode
}

export function KPICard({ title, value, icon }: KPICardProps) {
  const formattedValue = typeof value === 'number' ? value.toLocaleString('en-IN') : value

  return (
    <Box
      bg="white"
      borderRadius="12px"
      borderWidth="0.5px"
      borderColor="neutral.200"
      w="full"
      h="112px"
      px="16px"
      py="24px"
      boxShadow="sm"
      transition="box-shadow 0.2s"
    >
      <Flex align="center" gap="12px">
        {icon && (
          <Flex align="center" justify="center">
            {icon}
          </Flex>
        )}
        <Flex direction="column" gap={2}>
          <Text
            textStyle="bodyText4"
            fontWeight="400"
            color="neutral.600"
            fontSize={{ base: '14px', md: '16px' }}
          >
            {title}
          </Text>
          <Flex align="baseline" gap={2}>
            <Text textStyle="bodyText2" color="neutral.950">
              {formattedValue}
            </Text>
          </Flex>
        </Flex>
      </Flex>
    </Box>
  )
}
