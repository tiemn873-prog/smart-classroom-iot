import { NavLink } from 'react-router-dom'
import { IconBulb, IconDatabase, IconGrid } from './Icons.jsx'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Bảng điều khiển', icon: IconGrid },
  { to: '/data-sensor', label: 'Dữ liệu cảm biến', icon: IconDatabase },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-icon">
          <IconBulb />
        </div>
        <div>
          <div className="brand-name">Smart Class</div>
          <div className="brand-sub">IoT Dashboard</div>
        </div>
      </div>

      <nav className="nav">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
            <Icon />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">© 2026 Lớp học thông minh</div>
    </aside>
  )
}
