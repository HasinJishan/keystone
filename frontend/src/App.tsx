import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import WorkOrderBoard from './pages/WorkOrderBoard';
import WorkOrderDetail from './pages/WorkOrderDetail';
import MyJobs from './pages/MyJobs';
import CustomerPortal from './pages/CustomerPortal';
import Customers from './pages/Customers';
import Parts from './pages/Parts';
import UserManagement from './pages/UserManagement';
import type { Role } from './api/types';

function RequireAuth({ children, allow }: { children: JSX.Element; allow?: Role[] }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (allow && !allow.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

function RoleHome() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  const home: Record<Role, string> = {
    ADMIN: '/dashboard', DISPATCHER: '/board', MANAGER: '/dashboard', TECHNICIAN: '/my-jobs', CUSTOMER: '/portal',
  };
  return <Navigate to={home[user.role]} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />

        <Route element={<RequireAuth><Layout /></RequireAuth>}>
          <Route path="/" element={<RoleHome />} />
          <Route path="/dashboard" element={<RequireAuth allow={['MANAGER', 'ADMIN']}><Dashboard /></RequireAuth>} />
          <Route path="/board" element={<RequireAuth allow={['DISPATCHER', 'MANAGER', 'ADMIN']}><WorkOrderBoard /></RequireAuth>} />
          <Route path="/work-orders/:id" element={<WorkOrderDetail />} />
          <Route path="/my-jobs" element={<RequireAuth allow={['TECHNICIAN']}><MyJobs /></RequireAuth>} />
          <Route path="/portal" element={<RequireAuth allow={['CUSTOMER']}><CustomerPortal /></RequireAuth>} />
          <Route path="/customers" element={<RequireAuth allow={['DISPATCHER', 'MANAGER', 'ADMIN']}><Customers /></RequireAuth>} />
          <Route path="/parts" element={<RequireAuth allow={['DISPATCHER', 'MANAGER', 'ADMIN']}><Parts /></RequireAuth>} />
          <Route path="/users" element={<RequireAuth allow={['ADMIN']}><UserManagement /></RequireAuth>} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
