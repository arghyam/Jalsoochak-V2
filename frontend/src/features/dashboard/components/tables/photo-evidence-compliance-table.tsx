import { Box, Table, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import type { PhotoEvidenceComplianceData } from '../../types'

interface PhotoEvidenceComplianceTableProps {
  data: PhotoEvidenceComplianceData[]
  title?: string
  maxItems?: number
  showVillageColumn?: boolean
}

export function PhotoEvidenceComplianceTable({
  data,
  title = 'Photo Evidence Compliance',
  maxItems,
  showVillageColumn = true,
}: PhotoEvidenceComplianceTableProps) {
  const safeMaxItems =
    typeof maxItems === 'number' && Number.isFinite(maxItems) ? Math.max(0, maxItems) : undefined
  const rows = typeof safeMaxItems === 'number' ? data.slice(0, safeMaxItems) : data

  return (
    <Box borderRadius="lg" overflow="hidden">
      <Box textStyle="bodyText3" fontWeight="400" mb="16px">
        {title}
      </Box>
      <Box
        maxH="432px"
        overflowY="auto"
        overflowX="auto"
        pr={2}
        sx={{
          '&::-webkit-scrollbar': { width: '4px' },
          '&::-webkit-scrollbar-track': { bg: 'neutral.100', borderRadius: '999px' },
          '&::-webkit-scrollbar-thumb': {
            bg: 'neutral.300',
            borderRadius: '999px',
            minHeight: '165px',
          },
        }}
      >
        <Table size="sm">
          <Thead
            sx={{
              position: 'sticky',
              top: 0,
              zIndex: 1,
              bg: 'white',
              th: {
                textStyle: 'bodyText7',
                textTransform: 'none',
                fontWeight: '500',
                px: 3,
                py: 4,
              },
            }}
          >
            <Tr>
              <Th>Name</Th>
              {showVillageColumn ? <Th>Village</Th> : null}
              <Th>Last Submission</Th>
              <Th>Reading Value</Th>
            </Tr>
          </Thead>
          <Tbody
            sx={{
              td: {
                textStyle: 'bodyText7',
                fontWeight: '400',
                px: 3,
                py: 0,
                height: '40px',
                lineHeight: '40px',
                whiteSpace: 'nowrap',
              },
            }}
          >
            {rows.map((row) => (
              <Tr key={row.id} _odd={{ bg: 'primary.25' }}>
                <Td>{row.name}</Td>
                {showVillageColumn ? <Td>{row.village}</Td> : null}
                <Td>{row.lastSubmission}</Td>
                <Td>{row.readingValue}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </Box>
    </Box>
  )
}
