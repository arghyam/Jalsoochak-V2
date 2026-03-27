import { Box, Image } from '@chakra-ui/react'
import jalImage from '@/assets/media/jalmain.jpg'

export function AuthSideImage() {
  return (
    <Box
      w={{ base: '100%', md: '50%' }}
      h={{ base: '260px', md: '100vh' }}
      position="relative"
      overflow="hidden"
      display={{ base: 'none', md: 'block' }}
      borderTopLeftRadius="60px"
      borderBottomLeftRadius="60px"
    >
      <Image
        src={jalImage}
        alt="Water tap"
        w="100%"
        h="100%"
        objectFit="cover"
        objectPosition="center right"
      />
    </Box>
  )
}
