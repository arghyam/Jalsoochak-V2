import { useState } from 'react'
import {
  Button,
  Checkbox,
  FormControl,
  FormLabel,
  Input,
  InputGroup,
  InputRightElement,
  List,
  ListItem,
  Text,
} from '@chakra-ui/react'
import { AiOutlineEye, AiOutlineEyeInvisible } from 'react-icons/ai'

type CreatePasswordPageProps = {
  onNext: () => void
}

export function CreatePasswordPage({ onNext }: CreatePasswordPageProps) {
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const hasUppercase = /[A-Z]/.test(password)
  const hasLowercase = /[a-z]/.test(password)
  const hasNumber = /\d/.test(password)
  const hasSpecialChar = /[^A-Za-z0-9]/.test(password)
  const hasMinLength = password.length >= 8
  const isPasswordValid =
    hasUppercase && hasLowercase && hasNumber && hasSpecialChar && hasMinLength
  const isPasswordMatch = password === confirmPassword
  const canSubmit =
    password.length > 0 && confirmPassword.length > 0 && isPasswordValid && isPasswordMatch

  return (
    <>
      <Text textStyle="h5" mb={3}>
        Create password
      </Text>
      <Text textStyle="bodyText5" mb="20px">
        Enter a new password.
      </Text>

      <FormControl>
        <FormLabel textStyle="bodyText6" mb="4px" fontSize="16px">
          Create new password
          <Text as="span" color="error.500">
            *
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
            _placeholder={{ color: 'neutral.300' }}
            fontSize="sm"
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

      <FormControl mt="1.5rem">
        <FormLabel textStyle="bodyText6" mb="4px">
          Rewrite password
          <Text as="span" color="error.500">
            *
          </Text>
        </FormLabel>
        <InputGroup>
          <Input
            type={showConfirmPassword ? 'text' : 'password'}
            placeholder="Enter your password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            h="36px"
            px="12px"
            py="8px"
            borderRadius="4px"
            borderColor="neutral.300"
            _placeholder={{ color: 'neutral.300' }}
            fontSize="sm"
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
              onClick={() => setShowConfirmPassword((prev) => !prev)}
              aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
              _hover={{ bg: 'transparent' }}
              _active={{ bg: 'transparent' }}
            >
              {showConfirmPassword ? (
                <AiOutlineEye size="16px" />
              ) : (
                <AiOutlineEyeInvisible size="16px" />
              )}
            </Button>
          </InputRightElement>
        </InputGroup>
        {!isPasswordMatch && confirmPassword ? (
          <Text mt="6px" fontSize="sm" color="error.500">
            Passwords do not match.
          </Text>
        ) : null}
      </FormControl>

      {password.length > 0 && !isPasswordValid ? (
        <List mt="10px" spacing="0.5px" fontSize="sm" color="error.500" pl="18px" styleType="disc">
          <ListItem>Include at least 1 lowercase letter.</ListItem>
          <ListItem>Include at least 1 uppercase letter.</ListItem>
          <ListItem>Include at least 1 number.</ListItem>
          <ListItem>Include at least 1 special character.</ListItem>
          <ListItem>Be at least 8 characters long.</ListItem>
        </List>
      ) : null}

      <Checkbox
        mt="1.25rem"
        isChecked={rememberMe}
        onChange={(e) => setRememberMe(e.target.checked)}
        sx={{
          '.chakra-checkbox__control': {
            borderWidth: '1px',
            borderStyle: 'solid',
            borderColor: 'neutral.400',
            borderRadius: '4px',
            overflow: 'hidden',
            _checked: {
              bg: 'primary.500',
              borderColor: 'primary.500',
              color: 'white',
            },
            _hover: {
              bg: 'primary.500',
              borderColor: 'primary.500',
            },
            _focusVisible: {
              boxShadow: 'none',
            },
          },
        }}
      >
        <Text textStyle="bodyText5" color="neutral.950" fontWeight="400">
          Remember me
        </Text>
      </Checkbox>

      <Button
        w="full"
        mt="1.25rem"
        fontSize="16px"
        fontWeight="600"
        isDisabled={!canSubmit || isSubmitting}
        isLoading={isSubmitting}
        loadingText="Saving..."
        _loading={{ bg: 'primary.500', color: 'white' }}
        onClick={() => {
          setIsSubmitting(true)
          setTimeout(() => onNext(), 400)
        }}
      >
        Next
      </Button>
    </>
  )
}
