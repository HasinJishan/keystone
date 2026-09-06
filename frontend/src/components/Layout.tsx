import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const NAV_BY_ROLE: Record<string, { to: string; label: string }[]> = {
  DISPATCHER: [
    { to: '/board', label: 'Work Order Board' },
    { to: '/customers', label: 'Customers & Sites' },
    { to: '/parts', label: 'Parts' },
  ],
  MANAGER: [
    { to: '/dashboard', label: 'Dashboard' },
    { to: '/board', label: 'Work Order Board' },
    { to: '/customers', label: 'Customers & Sites' },
    { to: '/parts', label: 'Parts' },
  ],
  TECHNICIAN: [
    { to: '/my-jobs', label: 'My Jobs' },
  ],
  CUSTOMER: [
    { to: '/portal', label: 'My Requests' },
  ],
};

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const links = NAV_BY_ROLE[user.role] || [];

  function handleSignOut() {
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <nav className="nav-rail">
        <div className="nav-brand">
          <div className="nav-brand-mark">KEYSTONE</div>
          <div className="nav-brand-sub">Field Service Platform</div>
        </div>
        <div className="nav-links">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
            >
              {l.label}
            </NavLink>
          ))}
        </div>
        <div className="nav-footer">
          <div className="nav-user-name">{user.name}</div>
          <div className="nav-user-role">{user.role.toLowerCase()}</div>
          <button className="nav-signout" onClick={handleSignOut}>Sign out</button>
        </div>
      </nav>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
