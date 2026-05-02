import MarkdownRenderer from '../components/MarkdownRenderer'
import ArchFlowDiagram from '../components/ArchFlowDiagram'

const markdown = `
# 架构详解

## 整体架构概览

Claude Code 的核心架构围绕 **Harness 执行引擎** 构建，采用"感知-推理-行动-观察"（Perceive-Reason-Act-Observe）的循环模式来处理复杂任务。

下面展示了六大核心组件之间的完整数据流：
`

export default function ArchitectureContent() {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <MarkdownRenderer content={markdown} />
      <ArchFlowDiagram />
      <MarkdownRenderer content={`
## 六大核心组件

### 1. SystemPrompt 管理器

负责动态构建系统提示词（System Prompt），根据当前会话上下文选择性注入相关指令：

- **角色定义**：设定 AI 助手的身份和行为准则
- **工具描述**：注入当前可用的工具定义和调用规范
- **规则注入**：按需添加约束条件（如安全规则、代码风格）
- **上下文优化**：通过 Token 预算管理确保核心指令不被裁减

### 2. ToolSchema（工具注册）

定义所有可用工具的接口规范：

- **模式声明**：使用 JSON Schema 描述每个工具的输入输出
- **权限控制**：区分只读操作（文件读取、搜索）和写入操作
- **动态注册**：根据当前上下文启用或禁用特定工具集
- **类型保障**：通过 TypeScript 类型定义确保工具调用的类型安全

### 3. ToolCallLoop（工具调用循环）

Harness 的核心执行引擎，驱动多步推理过程：

1. **分析阶段**：解析用户请求，识别意图和关键信息
2. **规划阶段**：确定需要哪些工具调用以及调用顺序
3. **执行阶段**：依次或并行调度工具调用
4. **观察阶段**：收集工具返回结果，更新上下文
5. **迭代阶段**：根据结果决定是否需要继续调用或生成最终响应

### 4. ContextManager（上下文管理器）

管理会话的上下文窗口，确保信息的高效利用：

- **Token 预算分配**：为系统提示、工具定义、对话历史等分配 Token 配额
- **滑动窗口**：基于 Token 限制自动裁减早期对话内容
- **优先级策略**：工具执行结果 > 近期对话 > 历史对话
- **摘要压缩**：在上下文溢出时对早期内容进行摘要总结

### 5. VerificationHooks（验证钩子）

在关键执行点插入验证环节，降低错误率：

- **预执行验证**：工具调用前的参数校验和权限检查
- **后执行验证**：工具返回结果的完整性检查
- **安全审查**：检测潜在的危险操作并请求确认
- **质量门禁**：确保生成代码符合既定标准

### 6. SubAgent（子代理系统）

将复杂任务分解为并行子任务，通过独立代理执行：

- **隔离执行**：每个 SubAgent 拥有独立的上下文窗口
- **并行处理**：多个 SubAgent 可同时运行，加速任务完成
- **结果聚合**：主 Agent 收集各 SubAgent 的结果并整合
- **错误隔离**：单个 SubAgent 的失败不影响其他代理
`} />
    </div>
  )
}
