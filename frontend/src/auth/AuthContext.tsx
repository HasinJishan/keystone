import { createContext, useContext, useState, ReactNode } from 'react';
import { api } from '../api/client';
import type { LoginResponse, Role } from '../api/types';

interface AuthUser {
  userId: number;
  name: string;
  email: string;
  role: Role;
  customerId: number | null;
}

interface AuthContextValue {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string, companyName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = 'keystone_user';
const TOKEN_KEY = 'keystone_token';

function loadUser(): AuthUser | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw) as AuthUser; } catch { return null; }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadUser());

  async function login(email: string, password: string) {
    const res = await api.post<LoginResponse>('/api/auth/login', { email, password });
    const authUser: AuthUser = {
      userId: res.userId, name: res.name, email: res.email, role: res.role, customerId: res.customerId,
    };
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authUser));
    setUser(authUser);
  }

  // Registration only creates the account - it does NOT sign the user in.
  // The user is sent to the login page and signs in with the new credentials.
  async function register(name: string, email: string, password: string, companyName: string) {
    await api.post('/api/auth/register', { name, email, password, companyName });
  }

  // Tell the server to kill the token first (it reads the token from localStorage
  // synchronously when the request starts), then clear the browser session no matter what.
  function logout() {
    api.post('/api/auth/logout').catch(() => { /* server unreachable: still log out locally */ });
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
  }

  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
