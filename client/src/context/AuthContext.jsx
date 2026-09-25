import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('quicktix_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('quicktix_token'));
  const [loading, setLoading] = useState(false);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/login', { email, password });
      // res.data is AuthResponse { token, tokenType, user }
      const authData = res.data;
      setToken(authData.token);
      setUser(authData.user);
      localStorage.setItem('quicktix_token', authData.token);
      localStorage.setItem('quicktix_user', JSON.stringify(authData.user));
      return authData.user;
    } finally {
      setLoading(false);
    }
  };

  const register = async (name, email, password, role = 'USER') => {
    setLoading(true);
    try {
      await api.post('/auth/register', { name, email, password, role });
      // Automatically login after successful registration
      return await login(email, password);
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('quicktix_token');
    localStorage.removeItem('quicktix_user');
  };

  const isOrganizer = user?.role === 'ORGANIZER' || user?.role === 'ADMIN';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        register,
        logout,
        isAuthenticated: !!token,
        isOrganizer,
        isAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
