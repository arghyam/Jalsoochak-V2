import { useState, type FormEvent } from 'react'
import {
  Box,
  Button,
  Checkbox,
  Flex,
  FormControl,
  FormLabel,
  Image,
  Input,
  InputGroup,
  InputRightElement,
  Text,
} from '@chakra-ui/react'
import { AiOutlineEye, AiOutlineEyeInvisible } from 'react-icons/ai'
import jalsoochakLogo from '@/assets/media/jalsoochak-logo.svg'
import { AuthSideImage } from '@/features/auth/components/signup/auth-side-image'

export function ResetPasswordPage() {
  const [showEmailPassword, setShowEmailPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [emailPassword, setEmailPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const isPasswordMatch = newPassword === confirmPassword
  const canSubmit =
    emailPassword.trim().length > 0 &&
    newPassword.trim().length > 0 &&
    confirmPassword.trim().length > 0 &&
    isPasswordMatch

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setIsSubmitting(true)
  }

  return (
    <Flex minH="100vh" w="full" direction={{ base: 'column', md: 'row' }}>
      <Flex
        w={{ base: '100%', md: '50%' }}
        align="stretch"
        justify="flex-start"
        bg="white"
        px={{ base: 10, md: 8 }}
        py={{ base: 10, md: 8 }}
      >
        <Flex w="full" direction="column">
          <Box w="full" maxW="420px">
            <Image src={jalsoochakLogo} alt="JalSoochak logo" h="50px" mb={{ base: 10, md: 12 }} />
          </Box>

          <Flex flex="1" align="center" justify="center">
            <Box w="360px">
              <Text textStyle="h5" mb={3}>
                Reset password
              </Text>

              <Box as="form" onSubmit={handleSubmit}>
                <Flex direction="column" gap="1.5rem">
                  <FormControl>
                    <FormLabel textStyle="bodyText6" mb="4px">
                      Password sent via email{' '}
                      <Text as="span" color="error.500">
                        *
                      </Text>
                    </FormLabel>
                    <InputGroup>
                      <Input
                        type={showEmailPassword ? 'text' : 'password'}
                        placeholder="Enter your password"
                        autoComplete="one-time-code"
                        value={emailPassword}
                        onChange={(e) => setEmailPassword(e.target.value)}
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
                          onClick={() => setShowEmailPassword((prev) => !prev)}
                          aria-label={showEmailPassword ? 'Hide password' : 'Show password'}
                          _hover={{ bg: 'transparent' }}
                          _active={{ bg: 'transparent' }}
                        >
                          {showEmailPassword ? (
                            <AiOutlineEye size="16px" />
                          ) : (
                            <AiOutlineEyeInvisible size="16px" />
                          )}
                        </Button>
                      </InputRightElement>
                    </InputGroup>
                  </FormControl>

                  <FormControl>
                    <FormLabel textStyle="bodyText6" mb="4px">
                      Create new password{' '}
                      <Text as="span" color="error.500">
                        *
                      </Text>
                    </FormLabel>
                    <InputGroup>
                      <Input
                        type={showNewPassword ? 'text' : 'password'}
                        placeholder="Enter your password"
                        autoComplete="new-password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
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
                          onClick={() => setShowNewPassword((prev) => !prev)}
                          aria-label={showNewPassword ? 'Hide password' : 'Show password'}
                          _hover={{ bg: 'transparent' }}
                          _active={{ bg: 'transparent' }}
                        >
                          {showNewPassword ? (
                            <AiOutlineEye size="16px" />
                          ) : (
                            <AiOutlineEyeInvisible size="16px" />
                          )}
                        </Button>
                      </InputRightElement>
                    </InputGroup>
                  </FormControl>

                  <FormControl>
                    <FormLabel textStyle="bodyText6" mb="4px">
                      Rewrite password{' '}
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
                  </FormControl>

                  {!isPasswordMatch && confirmPassword ? (
                    <Text mt="-8px" fontSize="sm" color="error.500">
                      Passwords do not match.
                    </Text>
                  ) : null}

                  <Checkbox
                    fontSize="14px"
                    isChecked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    sx={{
                      '.chakra-checkbox__control': {
                        borderWidth: '1px',
                        borderStyle: 'solid',
                        borderColor: 'neutral.400',
                        borderRadius: '4px',
                        overflow: 'hidden',
                      },
                    }}
                  >
                    <Text textStyle="bodyText5" fontWeight="400" color="neutral.950">
                      Remember me
                    </Text>
                  </Checkbox>

                  <Button
                    type="submit"
                    w="full"
                    fontSize="16px"
                    fontWeight="600"
                    isDisabled={!canSubmit || isSubmitting}
                    isLoading={isSubmitting}
                    loadingText="Resetting..."
                    _loading={{ bg: 'primary.500', color: 'white' }}
                  >
                    Reset Password
                  </Button>
                </Flex>
              </Box>
            </Box>
          </Flex>
        </Flex>
      </Flex>

      <AuthSideImage />
    </Flex>
  )
}
