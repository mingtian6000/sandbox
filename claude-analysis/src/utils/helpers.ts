/** Simple slugify for Chinese + English text */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[\s]+/g, '-')
    .replace(/[^\w一-鿿-]/g, '')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
}

/** Truncate text to a max length, appending "..." if truncated */
export function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength).replace(/\s+$/, '') + '...'
}

/** Group array items by a key function */
export function groupBy<T>(items: T[], keyFn: (item: T) => string): Record<string, T[]> {
  return items.reduce(
    (acc, item) => {
      const key = keyFn(item)
      ;(acc[key] ??= []).push(item)
      return acc
    },
    {} as Record<string, T[]>,
  )
}
