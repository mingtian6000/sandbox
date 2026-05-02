import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { Components } from 'react-markdown'

interface Props {
  content: string
}

const components: Components = {
  h1: ({ children, ...props }) => (
    <h1 className="text-3xl font-bold text-white mb-6 mt-8 font-[JetBrains_Mono] tracking-tight" {...props}>
      {children}
    </h1>
  ),
  h2: ({ children, ...props }) => (
    <h2 className="text-2xl font-bold text-white mb-4 mt-8 font-[JetBrains_Mono]" {...props}>
      {children}
    </h2>
  ),
  h3: ({ children, ...props }) => (
    <h3 className="text-xl font-semibold text-gray-100 mb-3 mt-6" {...props}>
      {children}
    </h3>
  ),
  p: ({ children, ...props }) => (
    <p className="text-gray-300 leading-relaxed mb-4" {...props}>
      {children}
    </p>
  ),
  code: ({ className, children, ...props }) => {
    const isInline = !className
    if (isInline) {
      return (
        <code className="bg-[#16213e] text-[#e94560] px-1.5 py-0.5 rounded text-sm" {...props}>
          {children}
        </code>
      )
    }
    return (
      <pre className="bg-[#0d1117] rounded-xl p-4 mb-6 overflow-x-auto border border-white/5">
        <code className={className} {...props}>
          {children}
        </code>
      </pre>
    )
  },
  pre: ({ children }) => <>{children}</>,
  ul: ({ children, ...props }) => (
    <ul className="list-disc list-inside text-gray-300 mb-4 space-y-1" {...props}>
      {children}
    </ul>
  ),
  ol: ({ children, ...props }) => (
    <ol className="list-decimal list-inside text-gray-300 mb-4 space-y-1" {...props}>
      {children}
    </ol>
  ),
  li: ({ children, ...props }) => (
    <li className="text-gray-300" {...props}>
      {children}
    </li>
  ),
  table: ({ children, ...props }) => (
    <div className="overflow-x-auto mb-6">
      <table className="min-w-full border-collapse border border-white/10 text-sm" {...props}>
        {children}
      </table>
    </div>
  ),
  th: ({ children, ...props }) => (
    <th className="border border-white/10 px-4 py-2 bg-[#16213e] text-white font-semibold text-left" {...props}>
      {children}
    </th>
  ),
  td: ({ children, ...props }) => (
    <td className="border border-white/10 px-4 py-2 text-gray-300" {...props}>
      {children}
    </td>
  ),
  blockquote: ({ children, ...props }) => (
    <blockquote className="border-l-4 border-[#e94560] pl-4 my-4 text-gray-400 italic" {...props}>
      {children}
    </blockquote>
  ),
  hr: (props) => (
    <hr className="border-white/10 my-8" {...props} />
  ),
  a: ({ children, href, ...props }) => (
    <a href={href} className="text-[#4a9eff] hover:text-[#6bb5ff] underline underline-offset-2" target="_blank" rel="noopener noreferrer" {...props}>
      {children}
    </a>
  ),
}

export default function MarkdownRenderer({ content }: Props) {
  return (
    <div className="markdown-content">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {content}
      </ReactMarkdown>
    </div>
  )
}
