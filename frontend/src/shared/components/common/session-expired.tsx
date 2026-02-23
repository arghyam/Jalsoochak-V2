import { Link } from 'react-router-dom'
import { Box, Flex, Heading, Text, Button } from '@chakra-ui/react'
import { ROUTES } from '@/shared/constants/routes'

export function SessionExpiredPage() {
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
        <Heading as="h1" fontSize="2xl" fontWeight="bold" color="orange.600">
          Session Expired
        </Heading>

        <Text mt={3} fontSize="sm" color="gray.600">
          Your session has expired for security reasons. Please sign in again to continue using
          JalSoochak.
        </Text>

        <Button as={Link} to={ROUTES.LOGIN} colorScheme="blue" mt={6} size="sm">
          Go to Login
        </Button>
      </Box>
    </Flex>
  )
}
