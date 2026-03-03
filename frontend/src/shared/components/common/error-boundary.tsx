import { Component, type ReactNode } from 'react'
import { Flex, Box, Heading, Text } from '@chakra-ui/react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, errorInfo: unknown) {
    console.error('Error caught by boundary:', error, errorInfo)
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback || (
          <Flex h="100vh" align="center" justify="center">
            <Box textAlign="center">
              <Heading fontSize="2xl" fontWeight="bold" color="red.600">
                Something went wrong
              </Heading>

              <Text mt={2} color="gray.600">
                Please refresh the page or contact support if the problem persists.
              </Text>
            </Box>
          </Flex>
        )
      )
    }

    return this.props.children
  }
}
