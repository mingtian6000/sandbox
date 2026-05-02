import { slugify, truncate, groupBy } from '../utils/helpers'

describe('slugify', () => {
  it('converts spaces to hyphens', () => {
    expect(slugify('hello world')).toBe('hello-world')
  })

  it('handles Chinese characters', () => {
    expect(slugify('架构设计')).toBe('架构设计')
  })

  it('removes special characters', () => {
    expect(slugify('Hello, World!')).toBe('hello-world')
  })

  it('handles mixed Chinese and English', () => {
    expect(slugify('Claude Code 架构分析')).toBe('claude-code-架构分析')
  })

  it('collapses multiple hyphens', () => {
    expect(slugify('a  b   c')).toBe('a-b-c')
  })

  it('trims leading and trailing hyphens', () => {
    expect(slugify('  hello  ')).toBe('hello')
  })
})

describe('truncate', () => {
  it('returns full text when shorter than max', () => {
    expect(truncate('hello', 10)).toBe('hello')
  })

  it('truncates when text exceeds max length', () => {
    expect(truncate('hello world this is long', 10)).toBe('hello worl...')
  })

  it('trims trailing whitespace before appending ellipsis', () => {
    expect(truncate('hello world', 5)).toBe('hello...')
  })

  it('returns empty string for empty input', () => {
    expect(truncate('', 5)).toBe('')
  })
})

describe('groupBy', () => {
  it('groups items by key function', () => {
    const items = [
      { category: 'a', name: 'x' },
      { category: 'b', name: 'y' },
      { category: 'a', name: 'z' },
    ]
    const result = groupBy(items, (item) => item.category)
    expect(result).toEqual({
      a: [{ category: 'a', name: 'x' }, { category: 'a', name: 'z' }],
      b: [{ category: 'b', name: 'y' }],
    })
  })

  it('returns empty object for empty array', () => {
    expect(groupBy([], () => 'key')).toEqual({})
  })
})
