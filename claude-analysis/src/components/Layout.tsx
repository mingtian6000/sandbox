import type { ReactNode } from 'react'
import SideNav from './SideNav'

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <SideNav />
      <main className="flex-1 ml-64 p-8">
        {children}
      </main>
    </div>
  )
}
