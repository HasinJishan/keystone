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

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
