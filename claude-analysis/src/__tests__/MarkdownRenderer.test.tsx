import { render, screen } from '@testing-library/react'
import MarkdownRenderer from '../components/MarkdownRenderer'

// Mock react-markdown and remark-gfm since they're ESM-only
jest.mock('react-markdown', () => ({
  __esModule: true,
  default: ({ children }: { children: string }) => <div data-testid="markdown">{children}</div>,
}))

jest.mock('remark-gfm', () => ({
  __esModule: true,
  default: () => [],
}))

describe('MarkdownRenderer', () => {
  it('renders the markdown content', () => {
    render(<MarkdownRenderer content="# Hello" />)
    expect(screen.getByTestId('markdown')).toBeInTheDocument()
    expect(screen.getByText('# Hello')).toBeInTheDocument()
  })

  it('renders empty content without error', () => {
    render(<MarkdownRenderer content="" />)
    expect(screen.getByTestId('markdown')).toBeInTheDocument()
  })
})
