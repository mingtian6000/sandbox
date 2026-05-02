import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SideNav from '../components/SideNav'

describe('SideNav', () => {
  it('renders all navigation links', () => {
    render(
      <MemoryRouter>
        <SideNav />
      </MemoryRouter>,
    )

    expect(screen.getByText('首页概览')).toBeInTheDocument()
    expect(screen.getByText('架构详解')).toBeInTheDocument()
    expect(screen.getByText('模块拆解')).toBeInTheDocument()
    expect(screen.getByText('技术栈')).toBeInTheDocument()
  })

  it('renders the title', () => {
    render(
      <MemoryRouter>
        <SideNav />
      </MemoryRouter>,
    )

    expect(screen.getByText('Claude Code')).toBeInTheDocument()
  })

  it('renders 4 navigation links', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <SideNav />
      </MemoryRouter>,
    )

    const links = screen.getAllByRole('link')
    expect(links).toHaveLength(4)
  })
})
