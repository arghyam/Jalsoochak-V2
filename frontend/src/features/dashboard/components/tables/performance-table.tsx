import { Box, Text, Table, Thead, Tbody, Tr, Th, Td } from '@chakra-ui/react'
import type { EntityPerformance } from '../../types'

interface PerformanceTableProps {
  data: EntityPerformance[]
  title: string
  isBest?: boolean
  className?: string
}

export function PerformanceTable({ data, title }: PerformanceTableProps) {
  const getStatusConfig = (status: EntityPerformance['status']) => {
    const statusConfig = {
      good: { label: 'Good', color: 'success.500', bg: 'success.50' },
      'needs-attention': { label: 'Needs Attention', color: 'warning.600', bg: 'warning.50' },
      critical: { label: 'Critical', color: 'error.600', bg: 'error.50' },
    }
    return statusConfig[status]
  }

  return (
    <Box>
      <Text fontSize="lg" fontWeight="semibold" mb={4}>
        {title}
      </Text>
      <Box borderRadius="lg" borderWidth="1px" borderColor="neutral.100" overflow="hidden">
        <Table size="sm">
          <Thead>
            <Tr>
              <Th>Entity</Th>
              <Th>Coverage %</Th>
              <Th>Regularity %</Th>
              <Th>Continuity</Th>
              <Th>Quantity (LPCD)</Th>
              <Th>Composite Score</Th>
              <Th>Status</Th>
            </Tr>
          </Thead>
          <Tbody>
            {data.map((entity) => {
              const statusConfig = getStatusConfig(entity.status)
              return (
                <Tr key={entity.name}>
                  <Td>
                    <Text fontWeight="medium">{entity.name}</Text>
                  </Td>
                  <Td>{entity.coverage.toFixed(1)}%</Td>
                  <Td>{entity.regularity.toFixed(1)}%</Td>
                  <Td>{entity.continuity.toFixed(1)}</Td>
                  <Td>{entity.quantity}</Td>
                  <Td>
                    <Text fontWeight="semibold">{entity.compositeScore.toFixed(2)}</Text>
                  </Td>
                  <Td>
                    <Box
                      as="span"
                      px={2}
                      py={1}
                      borderRadius="md"
                      fontSize="xs"
                      fontWeight="medium"
                      bg={statusConfig.bg}
                      color={statusConfig.color}
                    >
                      {statusConfig.label}
                    </Box>
                  </Td>
                </Tr>
              )
            })}
          </Tbody>
        </Table>
      </Box>
    </Box>
  )
}
