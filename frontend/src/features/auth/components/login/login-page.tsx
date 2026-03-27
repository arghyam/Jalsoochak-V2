import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Flex,
  Image,
  Text,
  Button,
  Input,
  InputGroup,
  InputRightElement,
  FormControl,
  FormLabel,
  Checkbox,
  Link,
  useDisclosure,
} from '@chakra-ui/react'
import { AuthSideImage } from '@/features/auth/components/signup/auth-side-image'
import jalsoochakLogo from '@/assets/media/jalsoochak-logo.svg'
import { useAuthStore } from '@/app/store'
import { AiOutlineEye, AiOutlineEyeInvisible } from 'react-icons/ai'
import { ForgotPasswordModal } from '@/features/auth/components/login/forgot-password-modal'

export function LoginPage() {
  const navigate = useNavigate()
  const { login, loading, error } = useAuthStore()
  const { isOpen: isForgotPasswordOpen, onOpen, onClose } = useDisclosure()
  const [showPassword, setShowPassword] = useState(false)
  const [phoneNumber, setPhoneNumber] = useState('')
  const [password, setPassword] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  const validatePhoneNumber = (phone: string): boolean => {
    return /^\d{10}$/.test(phone)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLocalError(null)

    if (!validatePhoneNumber(phoneNumber)) {
      setLocalError('Phone number must be 10 digits')
      return
    }

    try {
      const redirectPath = await login({ phoneNumber, password })
      navigate(redirectPath, { replace: true })
    } catch (err) {
      console.error(err)
      setLocalError('Unable to login. Please try again.')
    }
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
            <Image
              src={jalsoochakLogo}
              alt="JalSoochak logo"
              w="117.61px"
              h="68.55px"
              mb={{ base: 10, md: 12 }}
            />
          </Box>

          <Flex flex="1" align="center" justify="center">
            <Box w="360px" h="360px">
              <Text textStyle="h5" mb={3}>
                Welcome
              </Text>
              <Text textStyle="bodyText5" mb="3rem">
                Welcome ! Please enter your details.
              </Text>

              <Box as="form" onSubmit={handleSubmit}>
                <Flex direction="column" gap="1.5rem">
                  <FormControl>
                    <FormLabel textStyle="bodyText6" mb="4px">
                      Phone Number{' '}
                      <Text as="span" color="error.500">
                        *
                      </Text>
                    </FormLabel>
                    <Input
                      type="tel"
                      placeholder="+91"
                      autoComplete="tel"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value.replace(/\D/g, ''))}
                      maxLength={10}
                      textStyle="bodyText5"
                      color="neutral.950"
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

                  <FormControl>
                    <FormLabel textStyle="bodyText6" mb="4px">
                      Password{' '}
                      <Text as="span" color="error.500">
                        *
                      </Text>
                    </FormLabel>
                    <InputGroup>
                      <Input
                        type={showPassword ? 'text' : 'password'}
                        placeholder="Enter your password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        textStyle="bodyText5"
                        color="neutral.950"
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
                          {showPassword ? (
                            <AiOutlineEye size="16px" />
                          ) : (
                            <AiOutlineEyeInvisible size="16px" />
                          )}
                        </Button>
                      </InputRightElement>
                    </InputGroup>
                  </FormControl>

                  {(localError || error) && (
                    <Text fontSize="sm" color="error.500">
                      {localError || error}
                    </Text>
                  )}

                  <Flex align="center" justify="space-between">
                    <Checkbox
                      fontSize="14px"
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

                    <Link
                      as="button"
                      type="button"
                      fontSize="14px"
                      fontWeight="600"
                      color="primary.500"
                      onClick={onOpen}
                    >
                      Forgot password
                    </Link>
                  </Flex>

                  <Button
                    type="submit"
                    w="full"
                    fontSize="16px"
                    fontWeight="600"
                    isLoading={loading}
                    loadingText="Signing in..."
                    _loading={{ bg: 'primary.500', color: 'white' }}
                  >
                    Log in
                  </Button>
                </Flex>
              </Box>
            </Box>
          </Flex>
        </Flex>
      </Flex>

      <AuthSideImage />
      <ForgotPasswordModal isOpen={isForgotPasswordOpen} onClose={onClose} />
    </Flex>
  )
}
