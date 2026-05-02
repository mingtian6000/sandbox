import { TextEncoder, TextDecoder } from 'util'
import '@testing-library/jest-dom'

global.TextEncoder = TextEncoder as typeof global.TextEncoder
global.TextDecoder = TextDecoder as typeof global.TextDecoder
