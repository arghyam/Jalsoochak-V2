import { Box, Flex, Text, Image } from '@chakra-ui/react'
import jalsoochakLogo from '@/assets/media/jalsoochak-logo.svg'

export function Header() {
  return (
    <Flex as="header" borderBottomWidth="1px" bg="white" boxShadow="sm" mb="32px">
      <Flex
        w="full"
        maxW="100%"
        align="center"
        px={{ base: '16px', md: '40px', lg: '80px' }}
        py={{ base: '12px', md: '12px' }}
        direction="row"
        gap={{ base: 3, md: 0 }}
      >
        <Flex align="center" flexShrink={0}>
          <Image
            src={jalsoochakLogo}
            alt="JalSoochak logo"
            w={{ base: '70px', md: '117.61px' }}
            h={{ base: '40px', md: '68.55px' }}
          />
        </Flex>

        <Flex flex="1" justify="center" w="full">
          <Text
            textStyle="h6"
            color="primary.500"
            textAlign="center"
            fontSize={{ base: '16px', md: '24px' }}
            lineHeight={{ base: 'short', md: 'normal' }}
            whiteSpace={{ base: 'nowrap', md: 'normal' }}
          >
            JalSoochak Dashboard
          </Text>
        </Flex>

        <Box
          display={{ base: 'none', md: 'block' }}
          w={{ md: '117.61px' }}
          h={{ md: '1px' }}
          aria-hidden="true"
        />
      </Flex>
    </Flex>
  )
}
