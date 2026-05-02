import MarkdownRenderer from '../components/MarkdownRenderer'
import ModuleTreeDiagram from '../components/ModuleTreeDiagram'

const markdown = `
# 模块拆解

## 各模块职责与协作关系

### 模块依赖图

下面展示了本报告站所有模块间的依赖关系：
`

export default function ModulesContent() {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <MarkdownRenderer content={markdown} />
      <ModuleTreeDiagram />
      <MarkdownRenderer content={`

## 内容结构说明

### 1. 全局样式系统 (\`index.css\`)

使用 [Tailwind CSS v4](https://github.com/tailwindlabs/tailwindcss) 提供的原子化 CSS 框架，配合 \`@theme\` 指令自定义设计 Token：

- **主色 (#1a1a2e)**：深色背景，营造技术感和沉浸式阅读体验
- **强调色 (#e94560)**：用于关键交互元素和视觉焦点
- **辅助色 (#16213e)**：卡片、面板的背景色
- **文字层级**：通过灰度区分信息层级

### 2. 导航系统 (\`SideNav.tsx\`)

固定左侧的导航面板，使用 [React Router](https://github.com/remix-run/react-router) 的 \`NavLink\` 实现：

- **高亮规则**：当前路由匹配时显示强调色边框
- **响应式**：通过 \`fixed\` + \`ml-64\` 实现固定侧边栏
- **视觉分隔**：使用 \`border-white/5\` 创建微妙的层级感

### 3. 内容渲染 (\`MarkdownRenderer.tsx\`)

通用的 Markdown 内容渲染组件，基于 [react-markdown](https://github.com/remarkjs/react-markdown) 构建：

- 使用 \`react-markdown\` 库解析 Markdown
- 集成 \`remark-gfm\` 支持 GFM 扩展（表格、任务列表等）
- 自定义代码块样式，支持语法高亮
- 递归渲染标题层级、列表、引用等

### 4. 架构图组件 (\`ArchitectureDiagram.tsx\`) · [\`ArchFlowDiagram.tsx\`](https://github.com/anthropics/claude-code)

使用纯 SVG 绘制的高质量架构图组件：

- 手动设计的 SVG 图形，支持渐变、阴影和动画
- 深色主题适配，与网站整体风格保持一致
- 无需外部依赖，零加载延迟
- 替换了原有的 Mermaid 动态渲染方案

### 5. 工具函数 (\`utils/helpers.ts\`)

通用工具函数模块：

- \`slugify\`：中英文混合文本的 URL 友好化处理
- \`truncate\`：文本截断，自动追加省略号
- \`groupBy\`：数组分组，支持任意 key 函数

### 6. 首页 (\`HeroSection.tsx\`)

- 大标题 + 描述的区域
- 核心架构图展示
- 设计哲学卡片（3 列网格布局）
- Call-to-Action 按钮导航

## 状态管理与路由

本项目没有使用复杂的状态管理方案，而是：

1. **[React Router v7](https://reactrouter.com/)**：管理页面导航
2. **URL 驱动**：通过 URL 路径决定当前显示的内容
3. **Props 传递**：组件间通过 Props 传递数据
4. **无全局状态**：内容页面均为静态内容，无需跨页面状态

这种设计符合技术报告站的需求 —— 内容展示型应用不需要复杂的状态管理。

## 技术依赖与源代码

本项目基于以下开源框架构建，它们的源代码可在 GitHub 上获取：

| 项目 | GitHub | 用途 |
|------|--------|------|
| Vite | [vitejs/vite](https://github.com/vitejs/vite) | 构建工具与开发服务器 |
| React | [facebook/react](https://github.com/facebook/react) | UI 组件框架 |
| TypeScript | [microsoft/TypeScript](https://github.com/microsoft/TypeScript) | 类型安全 |
| Tailwind CSS | [tailwindlabs/tailwindcss](https://github.com/tailwindlabs/tailwindcss) | 原子化 CSS |
| React Router | [remix-run/react-router](https://github.com/remix-run/react-router) | 路由管理 |
| react-markdown | [remarkjs/react-markdown](https://github.com/remarkjs/react-markdown) | Markdown 渲染 |
| Mermaid | [mermaid-js/mermaid](https://github.com/mermaid-js/mermaid) | 图表生成引擎 |
`} />
    </div>
  )
}
