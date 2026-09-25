import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Sun, Moon } from 'lucide-react';

export default function Navbar() {
  const { user, isAuthenticated, isOrganizer, isAdmin, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header style={{ borderBottom: '1px solid var(--panel-border)', background: 'var(--panel)', position: 'sticky', top: 0, zIndex: 50 }}>
      <div className="container flex items-center justify-between" style={{ height: '60px' }}>
        {/* Logo */}
        <Link to="/" style={{ fontWeight: 700, fontSize: '18px', letterSpacing: '-0.3px', color: 'var(--text-main)' }}>
          Quick<span style={{ color: 'var(--primary)' }}>Tix</span>
        </Link>

        {/* Navigation Links */}
        <nav className="flex items-center gap-5">
          <Link to="/" style={{ fontSize: '13.5px', color: 'var(--text-muted)' }}>
            Events
          </Link>

          {isOrganizer && (
            <Link to="/organizer/create" style={{ fontSize: '13.5px', color: 'var(--text-muted)' }}>
              Create Event
            </Link>
          )}

          {isAdmin && (
            <Link to="/admin" style={{ fontSize: '13.5px', color: 'var(--primary)', fontWeight: 600 }}>
              Admin Panel
            </Link>
          )}

          {isAuthenticated && (
            <Link to="/my-bookings" style={{ fontSize: '13.5px', color: 'var(--text-muted)' }}>
              My Bookings
            </Link>
          )}
        </nav>

        {/* User / Auth Actions + Theme Toggle */}
        <div className="flex items-center gap-3">
          {/* Theme Toggle Button */}
          <button
            onClick={toggleTheme}
            className="theme-toggle-btn"
            title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} mode`}
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
          </button>

          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2" style={{ background: 'var(--bg-subtle)', padding: '4px 10px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--panel-border)' }}>
                <span style={{ fontSize: '13px', fontWeight: 500 }}>{user?.name}</span>
                <span className="badge badge-muted" style={{ fontSize: '9px', padding: '1px 5px' }}>
                  {user?.role}
                </span>
              </div>
              <button onClick={handleLogout} className="btn btn-secondary btn-sm">
                Sign Out
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login" className="btn btn-secondary btn-sm">Sign In</Link>
              <Link to="/login" className="btn btn-primary btn-sm">Demo Login</Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
