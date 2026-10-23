import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import api, { setAccessToken, setOnSessionExpired, clearSession } from '../api/client';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';
const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('quicktix_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(null); // Kept in memory only
  const [loading, setLoading] = useState(true);

  // Synchronize expired session event from API response interceptor
  useEffect(() => {
    setOnSessionExpired(() => {
      setToken(null);
      setUser(null);
    });
  }, []);

  // Silent restore session on initial app load using HttpOnly refresh cookie
  useEffect(() => {
    let isMounted = true;

    const restoreSession = async () => {
      // Clean up legacy localStorage tokens from older version if any exist
      localStorage.removeItem('quicktix_token');
      localStorage.removeItem('quicktix_refresh_token');

      try {
        const res = await axios.post(`${baseURL}/auth/refresh`, {}, { withCredentials: true });
        const authData = res.data?.data || res.data;
        const accessToken = authData.token || authData.accessToken;

        if (isMounted && accessToken) {
          setAccessToken(accessToken);
          setToken(accessToken);
          if (authData.user) {
            setUser(authData.user);
            localStorage.setItem('quicktix_user', JSON.stringify(authData.user));
          }
        }
      } catch {
        if (isMounted) {
          setAccessToken(null);
          setToken(null);
          setUser(null);
          localStorage.removeItem('quicktix_user');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    restoreSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/login', { email, password });
      const authData = res.data?.data || res.data || res;
      const accessToken = authData.token || authData.accessToken;

      setAccessToken(accessToken);
      setToken(accessToken);
      setUser(authData.user);

      if (authData.user) {
        localStorage.setItem('quicktix_user', JSON.stringify(authData.user));
      }
      return authData.user;
    } finally {
      setLoading(false);
    }
  };

  const register = async (name, email, password, role = 'USER') => {
    setLoading(true);
    try {
      await api.post('/auth/register', { name, email, password, role });
      return await login(email, password);
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout', {});
    } catch {
      // Ignore network failures on logout
    } finally {
      clearSession();
      setToken(null);
      setUser(null);
    }
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

