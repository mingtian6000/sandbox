export default function ArchitectureDiagram() {
  return (
    <div className="bg-[#0d1117] rounded-xl p-6 border border-white/5 overflow-x-auto">
      <svg viewBox="0 0 1000 750" className="w-full h-auto" xmlns="http://www.w3.org/2000/svg">
        <defs>
          {/* Gradients */}
          <linearGradient id="g-user" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#4facfe" />
            <stop offset="100%" stopColor="#00f2fe" />
          </linearGradient>
          <linearGradient id="g-harness" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#1a1a2e" />
            <stop offset="100%" stopColor="#0f1629" />
          </linearGradient>
          <linearGradient id="g-sp" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#e94560" />
            <stop offset="100%" stopColor="#c23152" />
          </linearGradient>
          <linearGradient id="g-ts" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#0f3460" />
            <stop offset="100%" stopColor="#0a2540" />
          </linearGradient>
          <linearGradient id="g-tcl" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#533483" />
            <stop offset="100%" stopColor="#3b1f6e" />
          </linearGradient>
          <linearGradient id="g-vh" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#e94560" />
            <stop offset="100%" stopColor="#d63851" />
          </linearGradient>
          <linearGradient id="g-cm" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#16213e" />
            <stop offset="100%" stopColor="#0d1b3e" />
          </linearGradient>
          <linearGradient id="g-sub" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#4facfe" />
            <stop offset="100%" stopColor="#0062cc" />
          </linearGradient>
          <linearGradient id="g-resp" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#f093fb" />
            <stop offset="100%" stopColor="#f5576c" />
          </linearGradient>

          {/* Filter: Glow drop shadow */}
          <filter id="shadow" x="-5%" y="-5%" width="115%" height="120%">
            <feDropShadow dx="0" dy="3" stdDeviation="4" floodColor="#000" floodOpacity="0.4" />
          </filter>
          <filter id="glow-accent">
            <feDropShadow dx="0" dy="0" stdDeviation="6" floodColor="#e94560" floodOpacity="0.3" />
          </filter>
          <filter id="glow-blue">
            <feDropShadow dx="0" dy="0" stdDeviation="6" floodColor="#4facfe" floodOpacity="0.3" />
          </filter>
          <filter id="inner-shadow">
            <feOffset dx="0" dy="1" />
            <feGaussianBlur stdDeviation="2" result="offset-blur" />
            <feComposite operator="out" in="SourceGraphic" in2="offset-blur" result="inverse" />
            <feFlood floodColor="white" floodOpacity="0.05" result="color" />
            <feComposite operator="in" in="color" in2="inverse" result="shadow" />
            <feComposite operator="over" in="shadow" in2="SourceGraphic" />
          </filter>

          {/* Arrow markers */}
          <marker id="arrow-gray" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#4a5568" />
          </marker>
          <marker id="arrow-accent" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#e94560" />
          </marker>
          <marker id="arrow-blue" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#4facfe" />
          </marker>
          <marker id="arrow-white" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#e2e8f0" />
          </marker>
        </defs>

        {/* Background grid */}
        <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
          <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#ffffff" strokeOpacity="0.02" strokeWidth="0.5" />
        </pattern>
        <rect width="1000" height="750" fill="url(#grid)" />

        {/* Background subtle glow */}
        <ellipse cx="500" cy="350" rx="420" ry="280" fill="#e94560" opacity="0.03" />

        {/* ===== CONNECTION PATHS ===== */}

        {/* User → Harness */}
        <path d="M 500 68 L 500 100" stroke="#4facfe" strokeWidth="2.5" markerEnd="url(#arrow-blue)" />

        {/* Harness box internal flow */}
        {/* SP → TS */}
        <path d="M 175 280 L 175 300 L 310 300 L 310 280" fill="none" stroke="#4a5568" strokeWidth="2" markerEnd="url(#arrow-gray)" />
        {/* TS → TCL */}
        <path d="M 390 280 L 390 300 L 470 300 L 470 280" fill="none" stroke="#4a5568" strokeWidth="2" markerEnd="url(#arrow-gray)" />
        {/* TCL → VH */}
        <path d="M 550 280 L 550 300 L 650 300 L 650 280" fill="none" stroke="#4a5568" strokeWidth="2" markerEnd="url(#arrow-gray)" />
        {/* VH → CM */}
        <path d="M 730 280 L 730 300 L 800 300 L 800 280" fill="none" stroke="#4a5568" strokeWidth="2" markerEnd="url(#arrow-gray)" />
        {/* CM → TCL (feedback loop) */}
        <path d="M 800 200 L 870 200 L 870 140 L 550 140 L 550 200" fill="none" stroke="#e94560" strokeWidth="1.5" strokeDasharray="5,3" markerEnd="url(#arrow-accent)" />

        {/* Harness → SubAgent */}
        <path d="M 850 364 L 920 364 L 920 500" fill="none" stroke="#4facfe" strokeWidth="2" markerEnd="url(#arrow-blue)" />

        {/* SubAgent → Harness */}
        <path d="M 920 580 L 920 590 L 500 590 L 500 520" fill="none" stroke="#4facfe" strokeWidth="1.5" strokeDasharray="5,3" markerEnd="url(#arrow-blue)" />

        {/* Harness → Response */}
        <path d="M 500 520 L 500 660" fill="none" stroke="#f5576c" strokeWidth="2.5" markerEnd="url(#arrow-accent)" />

        {/* ===== USER INPUT ===== */}
        <g filter="url(#glow-blue)">
          <rect x="420" y="30" width="160" height="38" rx="19" fill="url(#g-user)" />
          <text x="500" y="54" textAnchor="middle" fill="#fff" fontSize="14" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            用户输入
          </text>
        </g>

        {/* ===== HARNESS ENGINE ===== */}
        <g filter="url(#shadow)">
          <rect x="90" y="100" width="760" height="420" rx="16" fill="url(#g-harness)" stroke="#e94560" strokeWidth="1.5" opacity="0.95" />
          {/* Harness header */}
          <rect x="90" y="100" width="760" height="42" rx="16" fill="#e94560" opacity="0.15" />
          <rect x="90" y="126" width="760" height="16" fill="#e94560" opacity="0.15" />
          <circle cx="114" cy="121" r="5" fill="#e94560" opacity="0.6" />
          <text x="130" y="126" fill="#e94560" fontSize="14" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            Harness 执行引擎
          </text>
        </g>

        {/* Internal components inside Harness */}
        {/* SP */}
        <g filter="url(#shadow)">
          <rect x="115" y="180" width="135" height="100" rx="10" fill="url(#g-sp)" />
          <text x="182" y="230" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            SystemPrompt
          </text>
          <text x="182" y="250" textAnchor="middle" fill="#ffb3c1" fontSize="11" fontFamily="'Inter',sans-serif">
            管理器
          </text>
          <rect x="125" y="266" width="115" height="4" rx="2" fill="#fff" opacity="0.15" />
        </g>

        {/* TS */}
        <g filter="url(#shadow)">
          <rect x="330" y="180" width="135" height="100" rx="10" fill="url(#g-ts)" stroke="#1c4780" strokeWidth="1" />
          <text x="397" y="230" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            ToolSchema
          </text>
          <text x="397" y="250" textAnchor="middle" fill="#8ab4f8" fontSize="11" fontFamily="'Inter',sans-serif">
            工具注册
          </text>
          <rect x="340" y="266" width="115" height="4" rx="2" fill="#fff" opacity="0.1" />
        </g>

        {/* TCL */}
        <g filter="url(#shadow)">
          <rect x="490" y="180" width="135" height="100" rx="10" fill="url(#g-tcl)" stroke="#7c5cbf" strokeWidth="1" />
          <text x="557" y="225" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            ToolCallLoop
          </text>
          <text x="557" y="242" textAnchor="middle" fill="#c4b5e3" fontSize="11" fontFamily="'Inter',sans-serif">
            工具调用循环
          </text>
          <text x="557" y="258" textAnchor="middle" fill="#c4b5e3" fontSize="10" fontFamily="'Inter',sans-serif">
            (感知→推理→行动→观察)
          </text>
          <rect x="500" y="266" width="115" height="4" rx="2" fill="#fff" opacity="0.15" />
        </g>

        {/* VH */}
        <g filter="url(#shadow)">
          <rect x="650" y="180" width="150" height="100" rx="10" fill="url(#g-vh)" />
          <text x="725" y="228" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            Verification
          </text>
          <text x="725" y="248" textAnchor="middle" fill="#ffb3c1" fontSize="11" fontFamily="'Inter',sans-serif">
            验证钩子
          </text>
          <rect x="660" y="266" width="130" height="4" rx="2" fill="#fff" opacity="0.15" />
        </g>

        {/* CM */}
        <g filter="url(#shadow)">
          <rect x="650" y="380" width="150" height="100" rx="10" fill="url(#g-cm)" stroke="#1c4780" strokeWidth="1" />
          <text x="725" y="430" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            Context
          </text>
          <text x="725" y="450" textAnchor="middle" fill="#8ab4f8" fontSize="11" fontFamily="'Inter',sans-serif">
            上下文管理器
          </text>
          <rect x="660" y="466" width="130" height="4" rx="2" fill="#fff" opacity="0.1" />
        </g>

        {/* Flow labels inside harness */}
        <text x="240" y="302" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">→</text>
        <text x="460" y="302" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">→</text>
        <text x="610" y="302" textAnchor="middle" fill="#718096" fontSize="10" fontFamily="'Inter',sans-serif">→</text>

        {/* Feedback loop label */}
        <text x="868" y="165" textAnchor="middle" fill="#e94560" fontSize="9" fontFamily="'Inter',sans-serif" transform="rotate(-90,868,165)">
          循环反馈
        </text>

        {/* ===== SUB AGENT ===== */}
        <g filter="url(#glow-blue)">
          <rect x="855" y="500" width="130" height="80" rx="12" fill="url(#g-sub)" />
          <text x="920" y="540" textAnchor="middle" fill="#fff" fontSize="13" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            SubAgent
          </text>
          <text x="920" y="560" textAnchor="middle" fill="#cce5ff" fontSize="11" fontFamily="'Inter',sans-serif">
            子代理系统
          </text>
        </g>

        {/* ===== RESPONSE ===== */}
        <g filter="url(#glow-accent)">
          <rect x="410" y="660" width="180" height="38" rx="19" fill="url(#g-resp)" />
          <text x="500" y="684" textAnchor="middle" fill="#fff" fontSize="14" fontWeight="700" fontFamily="'JetBrains Mono',monospace">
            最终响应
          </text>
        </g>

        {/* Platform label */}
        <text x="500" y="735" textAnchor="middle" fill="#4a5568" fontSize="11" fontFamily="'Inter',sans-serif">
          基于 TypeScript / Node.js · PlanMode 与 ExecuteMode 分离架构
        </text>
      </svg>
    </div>
  )
}
