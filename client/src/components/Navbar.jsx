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
    <header className="navbar-header" style={{ borderBottom: '1px solid var(--panel-border)', background: 'var(--panel-glass)', backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 50 }}>
      <div className="container flex items-center justify-between" style={{ height: '62px' }}>
        {/* Logo */}
        <Link to="/" style={{ fontWeight: 700, fontSize: '18px', letterSpacing: '-0.3px', color: 'var(--text-main)', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
          <span>Quick<span style={{ color: 'var(--primary)' }}>Tix</span></span>
        </Link>

        {/* Navigation Links */}
        <nav className="flex items-center gap-6">
          <a href="/#events" style={{ fontSize: '13.5px', color: 'var(--text-muted)', transition: 'color 0.15s ease' }} className="nav-link">
            Events
          </a>
          <a href="/#architecture" style={{ fontSize: '13.5px', color: 'var(--text-muted)', transition: 'color 0.15s ease' }} className="nav-link">
            Architecture
          </a>
          <a href="/#concurrency-flow" style={{ fontSize: '13.5px', color: 'var(--text-muted)', transition: 'color 0.15s ease' }} className="nav-link">
            Concurrency Flow
          </a>

          {isOrganizer && (
            <Link to="/organizer/create" style={{ fontSize: '13.5px', color: 'var(--text-muted)' }} className="nav-link">
              Create Event
            </Link>
          )}

          {isAdmin && (
            <Link to="/admin" style={{ fontSize: '13.5px', color: 'var(--primary)', fontWeight: 600 }}>
              Admin Panel
            </Link>
          )}

          {isAuthenticated && (
            <Link to="/my-bookings" style={{ fontSize: '13.5px', color: 'var(--text-muted)' }} className="nav-link">
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
            {theme === 'dark' ? <Sun size={15} /> : <Moon size={15} />}
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
              <Link to="/register" className="btn btn-primary btn-sm">Register</Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
