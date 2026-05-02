import ArchitectureDiagram from './ArchitectureDiagram'
import { Link } from 'react-router-dom'

export default function HeroSection() {
  return (
    <div className="max-w-5xl mx-auto">
      {/* Hero */}
      <section className="text-center py-16 border-b border-white/5">
        <h2 className="text-5xl font-bold text-white mb-4 tracking-tight">
          Claude Code 架构深度解析
        </h2>
        <p className="text-gray-400 text-lg max-w-2xl mx-auto leading-relaxed">
          Harness 驱动的 AI 编程助手核心架构分析 — 深入理解 SystemPrompt 管理器、
          ToolCallLoop、SubAgent 等六大组件的设计哲学与工程实践。
        </p>
        <div className="flex gap-4 justify-center mt-8">
          <Link
            to="/architecture"
            className="px-6 py-3 bg-[#e94560] text-white rounded-lg font-medium text-sm hover:bg-[#ff6b81] transition-colors"
          >
            探索架构 →
          </Link>
          <Link
            to="/modules"
            className="px-6 py-3 border border-white/20 text-gray-300 rounded-lg font-medium text-sm hover:border-white/40 hover:text-white transition-colors"
          >
            模块详情
          </Link>
        </div>
      </section>

      {/* Architecture Diagram */}
      <section className="py-12 border-b border-white/5">
        <h3 className="text-2xl font-bold text-white mb-2 text-center font-[JetBrains_Mono]">
          核心架构图
        </h3>
        <p className="text-gray-400 text-sm text-center mb-8">
          Claude Code 六大组件之间的数据流关系
        </p>
        <ArchitectureDiagram />
      </section>

      {/* Design Philosophy Cards */}
      <section className="py-12">
        <h3 className="text-2xl font-bold text-white mb-8 text-center font-[JetBrains_Mono]">
          设计哲学
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {cards.map((card) => (
            <div
              key={card.title}
              className="bg-[#16213e] rounded-xl p-6 border border-white/5 hover:border-white/20 transition-colors"
            >
              <div className="text-3xl mb-4">{card.icon}</div>
              <h4 className="text-white font-bold mb-2">{card.title}</h4>
              <p className="text-gray-400 text-sm leading-relaxed">{card.desc}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}

const cards = [
  {
    icon: '🧠',
    title: '规划与执行分离',
    desc: 'PlanMode 与 ExecuteMode 的严格分离，确保在生成代码前先进行完整的思考与规划，从根本上降低 AI 幻觉。',
  },
  {
    icon: '🔁',
    title: 'Harness 驱动循环',
    desc: 'ToolCallLoop 作为核心执行引擎，通过"感知-推理-行动-观察"的迭代循环处理复杂任务，每步都有验证钩子把关。',
  },
  {
    icon: '🧩',
    title: '模块化代理',
    desc: 'SubAgent 机制允许将复杂任务分解为并行子任务，每个 SubAgent 拥有独立的上下文窗口，实现高效的资源管理。',
  },
]
