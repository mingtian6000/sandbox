import { NavLink } from 'react-router-dom'

const navItems = [
  { to: '/', label: '首页概览', icon: '🏠' },
  { to: '/architecture', label: '架构详解', icon: '🏗️' },
  { to: '/modules', label: '模块拆解', icon: '🧩' },
  { to: '/tech-stack', label: '技术栈', icon: '⚙️' },
]

export default function SideNav() {
  return (
    <nav className="fixed left-0 top-0 h-screen w-64 bg-[#16213e] border-r border-white/5 flex flex-col z-50">
      <div className="p-6 border-b border-white/5">
        <h1 className="text-lg font-bold text-white font-[JetBrains_Mono] tracking-tight">
          Claude Code
        </h1>
        <p className="text-xs text-gray-400 mt-1">架构深度解析</p>
      </div>
      <div className="flex-1 py-4">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-6 py-3 text-sm transition-colors ${
                isActive
                  ? 'bg-white/10 text-[#e94560] border-r-2 border-[#e94560]'
                  : 'text-gray-400 hover:text-white hover:bg-white/5'
              }`
            }
          >
            <span className="text-base">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </div>
      <div className="p-4 border-t border-white/5">
        <p className="text-xs text-gray-500">技术报告 · 生成于 2026-05</p>
      </div>
    </nav>
  )
}
