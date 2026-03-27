import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, Flex, Image } from '@chakra-ui/react'
import jalsoochakLogo from '@/assets/media/jalsoochak-logo.svg'
import { AuthSideImage } from '@/features/auth/components/signup/auth-side-image'
import { SignupPage } from '@/features/auth/components/signup/signup-page'
import { CreatePasswordPage } from '@/features/auth/components/signup/create-password-page'
import { CredentialsPage } from '@/features/auth/components/signup/credentials-page'
import { ROUTES } from '@/shared/constants/routes'

type SignupStep = 'signup' | 'createPassword' | 'credentials'

type SignupFlowPageProps = {
  initialStep?: SignupStep
}

export function SignupFlowPage({ initialStep = 'signup' }: SignupFlowPageProps) {
  const [step, setStep] = useState<SignupStep>(initialStep)
  const navigate = useNavigate()

  useEffect(() => {
    setStep(initialStep)
  }, [initialStep])

  const showLogo = true
  const contentHeight = step === 'credentials' ? 'auto' : '360px'

  const renderStep = () => {
    if (step === 'createPassword') {
      return <CreatePasswordPage onNext={() => navigate(ROUTES.CREDENTIALS)} />
    }

    if (step === 'credentials') {
      return <CredentialsPage />
    }

    return <SignupPage onSuccess={() => navigate(ROUTES.CREATE_PASSWORD)} />
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
          {showLogo ? (
            <Box w="full" maxW="420px">
              <Image
                src={jalsoochakLogo}
                alt="JalSoochak logo"
                w="117.61px"
                h="68.55px"
                mb={{ base: 10, md: 12 }}
              />
            </Box>
          ) : null}

          <Flex flex="1" align="center" justify="center">
            <Box w="360px" h={contentHeight}>
              {renderStep()}
            </Box>
          </Flex>
        </Flex>
      </Flex>

      <AuthSideImage />
    </Flex>
  )
}
