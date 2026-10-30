import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Sun, Moon, Ticket } from 'lucide-react';

export default function Navbar() {
  const { user, isAuthenticated, isOrganizer, isAdmin, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <header
      className="navbar-header"
      style={{
        borderBottom: '1px solid var(--panel-border)',
        background: 'var(--panel-glass)',
        backdropFilter: 'blur(12px)',
        WebkitBackdropFilter: 'blur(12px)',
        position: 'sticky',
        top: 0,
        zIndex: 50,
      }}
    >
      <div className="container flex items-center justify-between" style={{ height: '62px' }}>
        {/* Brand Logo */}
        <Link
          to="/"
          style={{
            fontWeight: 800,
            fontSize: '18px',
            letterSpacing: '-0.3px',
            color: 'var(--text-main)',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            textDecoration: 'none',
          }}
        >
          <div
            style={{
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              background: 'var(--primary)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Ticket size={16} />
          </div>
          <span>
            Quick<span style={{ color: 'var(--primary)' }}>Tix</span>
          </span>
        </Link>

        {/* Navigation Links - Exactly 3 role-tailored links */}
        <nav className="flex items-center gap-6">
          {/* Link 1: Events (Visible to everyone) */}
          <Link
            to="/events"
            style={{
              fontSize: '13.5px',
              color: isActive('/events') ? 'var(--primary)' : 'var(--text-muted)',
              fontWeight: isActive('/events') ? 600 : 500,
              textDecoration: 'none',
              transition: 'color 0.15s ease',
            }}
            className="nav-link"
          >
            Events
          </Link>

          {/* Links for Guest (Unauthenticated) */}
          {!isAuthenticated && (
            <>
              <a
                href="/#how-it-works"
                style={{
                  fontSize: '13.5px',
                  color: 'var(--text-muted)',
                  fontWeight: 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                How It Works
              </a>
              <a
                href="/#organizers"
                style={{
                  fontSize: '13.5px',
                  color: 'var(--text-muted)',
                  fontWeight: 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                For Organizers
              </a>
            </>
          )}

          {/* Links for Regular Attendee (USER role) */}
          {isAuthenticated && !isOrganizer && !isAdmin && (
            <>
              <Link
                to="/my-bookings"
                style={{
                  fontSize: '13.5px',
                  color: isActive('/my-bookings') ? 'var(--primary)' : 'var(--text-muted)',
                  fontWeight: isActive('/my-bookings') ? 600 : 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                My Bookings
              </Link>
              <a
                href="/#how-it-works"
                style={{
                  fontSize: '13.5px',
                  color: 'var(--text-muted)',
                  fontWeight: 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                How It Works
              </a>
            </>
          )}

          {/* Links for Organizer (ORGANIZER role) */}
          {isAuthenticated && isOrganizer && (
            <>
              <Link
                to="/organizer/create"
                style={{
                  fontSize: '13.5px',
                  color: isActive('/organizer/create') ? 'var(--primary)' : 'var(--text-muted)',
                  fontWeight: isActive('/organizer/create') ? 600 : 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                Create Event
              </Link>
              <Link
                to="/my-bookings"
                style={{
                  fontSize: '13.5px',
                  color: isActive('/my-bookings') ? 'var(--primary)' : 'var(--text-muted)',
                  fontWeight: isActive('/my-bookings') ? 600 : 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                My Bookings
              </Link>
            </>
          )}

          {/* Links for Admin (ADMIN role) */}
          {isAuthenticated && isAdmin && (
            <>
              <Link
                to="/admin"
                style={{
                  fontSize: '13.5px',
                  color: isActive('/admin') ? 'var(--primary)' : 'var(--primary)',
                  fontWeight: 600,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
              >
                Admin Panel
              </Link>
              <Link
                to="/my-bookings"
                style={{
                  fontSize: '13.5px',
                  color: isActive('/my-bookings') ? 'var(--primary)' : 'var(--text-muted)',
                  fontWeight: isActive('/my-bookings') ? 600 : 500,
                  textDecoration: 'none',
                  transition: 'color 0.15s ease',
                }}
                className="nav-link"
              >
                My Bookings
              </Link>
            </>
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
              <div
                className="flex items-center gap-2"
                style={{
                  background: 'var(--bg-subtle)',
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--panel-border)',
                }}
              >
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
              <Link to="/login" className="btn btn-secondary btn-sm" style={{ textDecoration: 'none' }}>
                Sign In
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
