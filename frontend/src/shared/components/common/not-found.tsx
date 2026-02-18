import { Link } from 'react-router-dom'
import { Box, Flex, Heading, Text, Button, Stack } from '@chakra-ui/react'
import { ROUTES } from '@/shared/constants/routes'

export function NotFoundPage() {
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
        <Heading as="h1" fontSize="3xl" fontWeight="bold" color="gray.900">
          404 - Page Not Found
        </Heading>

        <Text mt={3} fontSize="sm" color="gray.600">
          The page you’re looking for doesn’t exist or may have been moved.
        </Text>

        <Stack mt={6} direction="row" spacing={3} justify="center">
          <Button as={Link} to={ROUTES.DASHBOARD} colorScheme="blue" size="sm">
            Go to Dashboard
          </Button>

          <Button as={Link} to={ROUTES.LOGIN} variant="outline" size="sm">
            Go to Login
          </Button>
        </Stack>
      </Box>
    </Flex>
  )
}
