import { useState } from 'react'
import {
  Button,
  FormControl,
  FormLabel,
  Input,
  InputGroup,
  InputRightElement,
  Text,
} from '@chakra-ui/react'
import { AiOutlineEye, AiOutlineEyeInvisible } from 'react-icons/ai'

type SignupPageProps = {
  onSuccess: () => void
}

export function SignupPage({ onSuccess }: SignupPageProps) {
  const [showPassword, setShowPassword] = useState(false)
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [authError, setAuthError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Temporary hardcoded credentials until signup API is available.
  const HARD_CODED_USER = {
    userId: 'demo.user',
    password: '1234567',
  }

  const handleSignup = () => {
    setAuthError('')

    const isMatch =
      userId.trim() === HARD_CODED_USER.userId && password === HARD_CODED_USER.password

    if (!isMatch) {
      setAuthError('User ID or password is incorrect.')
      return
    }

    setIsSubmitting(true)
    onSuccess()
  }

  return (
    <>
      <Text textStyle="h5" mb={3}>
        Welcome
      </Text>
      <Text textStyle="bodyText5" mb="3rem" fontWeight="400">
        Please enter your details.
      </Text>

      <FormControl>
        <FormLabel>
          <Text textStyle="bodyText6" mb="4px" fontWeight="500">
            User ID
            <Text as="span" color="error.500">
              *
            </Text>
          </Text>
        </FormLabel>
        <Input
          placeholder="Enter your user ID"
          autoComplete="off"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          h="36px"
          px="12px"
          py="8px"
          borderRadius="4px"
          borderColor="neutral.300"
          _placeholder={{ color: 'neutral.300' }}
          fontSize="sm"
          focusBorderColor="primary.500"
        />
      </FormControl>

      <FormControl mt="1.5rem">
        <FormLabel>
          <Text textStyle="bodyText6" mb="4px" fontWeight="500">
            Password sent via email
            <Text as="span" color="error.500">
              *
            </Text>
          </Text>
        </FormLabel>

        <InputGroup>
          <Input
            type={showPassword ? 'text' : 'password'}
            placeholder="Enter your password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            h="36px"
            px="12px"
            py="8px"
            borderRadius="4px"
            borderColor="neutral.300"
            fontSize="sm"
            _placeholder={{ color: 'neutral.300' }}
            focusBorderColor="primary.500"
            pr="36px"
          />
          <InputRightElement h="36px">
            <Button
              variant="unstyled"
              size="sm"
              color="neutral.400"
              display="flex"
              alignItems="center"
              justifyContent="center"
              onClick={() => setShowPassword((prev) => !prev)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              _hover={{ bg: 'transparent' }}
              _active={{ bg: 'transparent' }}
            >
              {showPassword ? <AiOutlineEye size="16px" /> : <AiOutlineEyeInvisible size="16px" />}
            </Button>
          </InputRightElement>
        </InputGroup>
      </FormControl>

      {authError ? (
        <Text mt="12px" fontSize="sm" color="error.500">
          {authError}
        </Text>
      ) : null}

      <Button
        w="full"
        mt="2rem"
        fontSize="16px"
        fontWeight="600"
        isDisabled={!userId || !password || isSubmitting}
        isLoading={isSubmitting}
        loadingText="Signing up..."
        _loading={{ bg: 'primary.500', color: 'white' }}
        onClick={handleSignup}
      >
        Sign up
      </Button>
    </>
  )
}
