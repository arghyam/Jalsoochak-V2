import { Flex, Spinner } from '@chakra-ui/react'

export function LoadingSpinner() {
  return (
    <Flex align="center" justify="center" p={8}>
      <Spinner
        thickness="4px"
        speed="0.65s"
        emptyColor="gray.300"
        color="blue.600"
        size="md"
        label="Loading..."
      />
    </Flex>
  )
}
