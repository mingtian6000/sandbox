import MarkdownRenderer from '../components/MarkdownRenderer'

const markdown = `
# 技术栈分析

## 本项目（技术报告站）技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 构建工具 | Vite | ^8.0 | 快速开发服务器与构建 |
| UI 框架 | React | ^19.2 | 组件化 UI 构建 |
| 语言 | TypeScript | ~6.0 | 类型安全 |
| CSS 框架 | Tailwind CSS | ^4.2 | 原子化样式 |
| 路由 | React Router | ^7.14 | SPA 路由管理 |
| Markdown | react-markdown | ^10.1 | Markdown 渲染 |
| 图表 | Mermaid | (动态导入) | 架构图生成 |
| 扩展语法 | remark-gfm | ^4.0 | GFM 表格/任务列表 |

## Claude Code 本身技术栈推断

基于公开文档和架构分析，Claude Code 的核心技术栈：

### 运行环境
- **Node.js**：CLI 工具的运行时基础
- **TypeScript**：主要开发语言，提供类型安全
- **ESM 模块**：现代 Node.js 模块系统

### 核心架构组件
- **Harness 模式**：受 LangChain / Vercel AI SDK 启发的 Agent 执行框架
- **Tool 系统**：基于 JSON Schema 的工具定义与验证
- **进程管理**：SubAgent 通过子进程或 Worker Threads 实现隔离

### 关键设计模式
1. **责任链模式**：VerificationHooks 形成验证链
2. **策略模式**：不同 SubAgent 可选择不同的执行策略
3. **观察者模式**：ToolCallLoop 监听 Hook 事件
4. **工厂模式**：根据任务类型动态创建对应的 SubAgent

### 通信协议
- **stdin/stdout JSON-RPC**：Harness 与 SubAgent 间的通信
- **事件驱动**：基于 EventEmitter 的异步事件处理
- **流式响应**：支持 SSE 风格的流式输出

## Claude Code 的工程优势

### 1. 低幻觉设计
通过 PlanMode 强制规划、VerificationHooks 验证、ToolCallLoop 迭代的三角机制，大幅降低错误输出。

### 2. 长任务可靠性
ContextManager 的滑动窗口机制确保长对话不丢失核心上下文；SubAgent 的隔离执行防止单点故障。

### 3. 可扩展性
ToolSchema 的动态注册机制允许轻松添加新工具；SubAgent 系统支持灵活的自定义代理类型。

### 4. 安全优先
VerificationHooks 中的权限检查和安全审查确保所有操作都在用户授权范围内执行。
`

export default function TechStackContent() {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <MarkdownRenderer content={markdown} />
    </div>
  )
}
