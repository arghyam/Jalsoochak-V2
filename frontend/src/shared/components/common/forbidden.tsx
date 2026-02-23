import { Box, Flex, Heading, Text } from '@chakra-ui/react'

export function ForbiddenPage() {
  return (
    <Flex minH="100vh" align="center" justify="center" bg="gray.50">
      <Box
        maxW="md"
        rounded="lg"
        borderWidth="1px"
        bg="white"
        p={8}
        textAlign="center"
        boxShadow="sm"
      >
        <Heading fontSize="3xl" fontWeight="bold" color="red.600">
          403 - Unauthorized
        </Heading>

        <Text mt={3} fontSize="sm" color="gray.600">
          You do not have permission to access this page. If you believe this is a mistake, please
          contact your system administrator.
        </Text>
      </Box>
    </Flex>
  )
}
