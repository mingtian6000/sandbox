import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import HeroSection from './components/HeroSection'
import ArchitectureContent from './content/ArchitectureContent'
import ModulesContent from './content/ModulesContent'
import TechStackContent from './content/TechStackContent'

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HeroSection />} />
        <Route path="/architecture" element={<ArchitectureContent />} />
        <Route path="/modules" element={<ModulesContent />} />
        <Route path="/tech-stack" element={<TechStackContent />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}
