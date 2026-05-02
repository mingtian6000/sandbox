export default function ArchFlowDiagram() {
  return (
    <div className="bg-[#0d1117] rounded-xl p-6 border border-white/5 overflow-x-auto my-6">
      <svg viewBox="0 0 900 600" className="w-full h-auto" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="a-user" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#4facfe" /><stop offset="100%" stopColor="#00f2fe" />
          </linearGradient>
          <linearGradient id="a-harness-bg" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#1a1a2e" /><stop offset="100%" stopColor="#0f1629" />
          </linearGradient>
          <linearGradient id="a-sp" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#e94560" /><stop offset="100%" stopColor="#c23152" />
          </linearGradient>
          <linearGradient id="a-ts" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#0f3460" /><stop offset="100%" stopColor="#0a2540" />
          </linearGradient>
          <linearGradient id="a-tcl" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#533483" /><stop offset="100%" stopColor="#3b1f6e" />
          </linearGradient>
          <linearGradient id="a-vh" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#e94560" /><stop offset="100%" stopColor="#d63851" />
          </linearGradient>
          <linearGradient id="a-cm" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#16213e" /><stop offset="100%" stopColor="#0d1b3e" />
          </linearGradient>
          <linearGradient id="a-sub" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#4facfe" /><stop offset="100%" stopColor="#0062cc" />
          </linearGradient>
          <linearGradient id="a-resp" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#f093fb" /><stop offset="100%" stopColor="#f5576c" />
          </linearGradient>

          <filter id="a-shadow"><feDropShadow dx="0" dy="3" stdDeviation="4" floodColor="#000" floodOpacity="0.4" /></filter>
          <filter id="a-glow-blue"><feDropShadow dx="0" dy="0" stdDeviation="6" floodColor="#4facfe" floodOpacity="0.3" /></filter>
          <filter id="a-glow-accent"><feDropShadow dx="0" dy="0" stdDeviation="6" floodColor="#e94560" floodOpacity="0.3" /></filter>

          <marker id="a-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#4a5568" />
          </marker>
          <marker id="a-arrow-accent" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#e94560" />
          </marker>
          <marker id="a-arrow-blue" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#4facfe" />
          </marker>
        </defs>

        <pattern id="a-grid" width="20" height="20" patternUnits="userSpaceOnUse">
          <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#ffffff" strokeOpacity="0.02" strokeWidth="0.5" />
        </pattern>
        <rect width="900" height="600" fill="url(#a-grid)" />
        <ellipse cx="450" cy="280" rx="380" ry="240" fill="#e94560" opacity="0.03" />

        {/* User Input */}
        <g filter="url(#a-glow-blue)">
          <rect x="350" y="25" width="200" height="42" rx="21" fill="url(#a-user)" />
          <text x="450" y="51" textAnchor="middle" fill="#fff" fontSize="14" fontWeight="700" fontFamily="'JetBrains Mono',monospace">用户输入</text>
        </g>
        <path d="M 450 67 L 450 95" stroke="#4facfe" strokeWidth="2.5" markerEnd="url(#a-arrow-blue)" />

        {/* Harness Engine */}
        <g filter="url(#a-shadow)">
          <rect x="50" y="95" width="800" height="340" rx="14" fill="url(#a-harness-bg)" stroke="#e94560" strokeWidth="1.5" />
          <rect x="50" y="95" width="800" height="38" rx="14" fill="#e94560" opacity="0.12" />
          <rect x="50" y="119" width="800" height="14" fill="#e94560" opacity="0.12" />
          <circle cx="76" cy="114" r="5" fill="#e94560" opacity="0.5" />
          <text x="94" y="119" fill="#e94560" fontSize="13" fontWeight="700" fontFamily="'JetBrains Mono',monospace">Harness 执行引擎</text>
        </g>

        {/* Phase indicators */}
        <text x="165" y="166" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">① 感知阶段</text>
        <text x="365" y="166" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">② 推理阶段</text>
        <text x="565" y="166" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">③ 行动阶段</text>
        <text x="745" y="166" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">④ 观察阶段</text>

        {/* Row 1: SP → TS → TCL → VH */}
        <g filter="url(#a-shadow)">
          <rect x="100" y="180" width="130" height="90" rx="8" fill="url(#a-sp)" />
          <text x="165" y="218" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">SystemPrompt</text>
          <text x="165" y="238" textAnchor="middle" fill="#ffb3c1" fontSize="11">管理器</text>
          <rect x="110" y="256" width="110" height="4" rx="2" fill="#fff" opacity="0.12" />
        </g>

        <path d="M 230 225 L 282 225" stroke="#4a5568" strokeWidth="2" markerEnd="url(#a-arrow)" />

        <g filter="url(#a-shadow)">
          <rect x="290" y="180" width="130" height="90" rx="8" fill="url(#a-ts)" stroke="#1c4780" strokeWidth="1" />
          <text x="355" y="218" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">ToolSchema</text>
          <text x="355" y="238" textAnchor="middle" fill="#8ab4f8" fontSize="11">工具注册</text>
          <rect x="300" y="256" width="110" height="4" rx="2" fill="#fff" opacity="0.08" />
        </g>

        <path d="M 420 225 L 472 225" stroke="#4a5568" strokeWidth="2" markerEnd="url(#a-arrow)" />

        <g filter="url(#a-shadow)">
          <rect x="480" y="180" width="140" height="90" rx="8" fill="url(#a-tcl)" stroke="#7c5cbf" strokeWidth="1" />
          <text x="550" y="216" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">ToolCallLoop</text>
          <text x="550" y="234" textAnchor="middle" fill="#c4b5e3" fontSize="11">工具调用循环</text>
          <text x="550" y="252" textAnchor="middle" fill="#c4b5e3" fontSize="9">(迭代推理引擎)</text>
          <rect x="490" y="260" width="120" height="4" rx="2" fill="#fff" opacity="0.12" />
        </g>

        <path d="M 620 225 L 660 225" stroke="#4a5568" strokeWidth="2" markerEnd="url(#a-arrow)" />

        <g filter="url(#a-shadow)">
          <rect x="668" y="180" width="140" height="90" rx="8" fill="url(#a-vh)" />
          <text x="738" y="218" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">Verification</text>
          <text x="738" y="238" textAnchor="middle" fill="#ffb3c1" fontSize="11">验证钩子</text>
          <rect x="678" y="256" width="120" height="4" rx="2" fill="#fff" opacity="0.12" />
        </g>

        {/* Feedback label */}
        <text x="810" y="300" textAnchor="middle" fill="#e94560" fontSize="9" fontFamily="'Inter',sans-serif" transform="rotate(-90,810,300)">上下文反馈</text>

        {/* VH → CM */}
        <path d="M 738 270 L 738 310" stroke="#e94560" strokeWidth="1.5" strokeDasharray="5,3" markerEnd="url(#a-arrow-accent)" />

        {/* Row 2: CM */}
        <g filter="url(#a-shadow)">
          <rect x="668" y="315" width="140" height="85" rx="8" fill="url(#a-cm)" stroke="#1c4780" strokeWidth="1" />
          <text x="738" y="352" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">ContextManager</text>
          <text x="738" y="372" textAnchor="middle" fill="#8ab4f8" fontSize="11">上下文管理</text>
          <rect x="678" y="390" width="120" height="4" rx="2" fill="#fff" opacity="0.08" />
        </g>

        {/* CM → TCL feedback loop */}
        <path d="M 668 357 L 550 357 L 550 270" fill="none" stroke="#e94560" strokeWidth="1.5" strokeDasharray="5,3" markerEnd="url(#a-arrow-accent)" />

        {/* Loop label */}
        <text x="590" y="350" textAnchor="middle" fill="#e94560" fontSize="9" fontFamily="'Inter',sans-serif">迭代循环</text>

        {/* Harness → SubAgent */}
        <path d="M 450 435 L 450 465" stroke="#4facfe" strokeWidth="2.5" markerEnd="url(#a-arrow-blue)" />

        {/* SubAgent */}
        <g filter="url(#a-glow-blue)">
          <rect x="310" y="465" width="280" height="50" rx="12" fill="url(#a-sub)" />
          <text x="450" y="488" textAnchor="middle" fill="#fff" fontSize="13" fontWeight="700" fontFamily="'JetBrains Mono',monospace">SubAgent 子代理系统</text>
          <text x="450" y="504" textAnchor="middle" fill="#cce5ff" fontSize="10">隔离执行 · 并行处理 · 结果聚合 · 错误隔离</text>
        </g>

        <path d="M 450 515 L 450 545" stroke="#f5576c" strokeWidth="2.5" markerEnd="url(#a-arrow-accent)" />

        {/* Response */}
        <g filter="url(#a-glow-accent)">
          <rect x="360" y="545" width="180" height="38" rx="19" fill="url(#a-resp)" />
          <text x="450" y="569" textAnchor="middle" fill="#fff" fontSize="14" fontWeight="700" fontFamily="'JetBrains Mono',monospace">最终响应</text>
        </g>
      </svg>
    </div>
  )
}
