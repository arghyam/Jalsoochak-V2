import { VStack, Heading, Text } from '@chakra-ui/react'

export function Dashboard() {
  return (
    <VStack spacing={6} align="start">
      <Heading fontSize="3xl" fontWeight="bold">
        Dashboard
      </Heading>

      <Text color="gray.600">
        Dashboard content will be implemented here with charts, maps, and tables.
      </Text>
    </VStack>
  )
}
