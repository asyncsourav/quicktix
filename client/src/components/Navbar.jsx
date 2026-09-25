import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Ticket, PlusCircle, User, LogOut, Calendar, ShieldCheck } from 'lucide-react';

export default function Navbar() {
  const { user, isAuthenticated, isOrganizer, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header style={{ borderBottom: '1px solid var(--panel-border)', background: 'rgba(11, 15, 23, 0.85)', backdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 50 }}>
      <div className="container flex items-center justify-between" style={{ height: '68px' }}>
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2.5" style={{ fontWeight: 800, fontSize: '19px', letterSpacing: '-0.5px' }}>
          <div style={{ background: 'var(--primary)', color: '#fff', padding: '6px', borderRadius: '8px', display: 'flex' }}>
            <Ticket size={19} />
          </div>
          <span>Quick<span style={{ color: 'var(--primary)' }}>Tix</span></span>
        </Link>

        {/* Navigation Links */}
        <nav className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-1.5" style={{ fontSize: '13.5px', color: 'var(--text-muted)', fontWeight: 500 }}>
            <Calendar size={15} />
            <span>Discover Events</span>
          </Link>

          {isOrganizer && (
            <Link to="/organizer/create" className="flex items-center gap-1.5" style={{ fontSize: '13.5px', color: 'var(--text-muted)', fontWeight: 500 }}>
              <PlusCircle size={15} />
              <span>Create Event</span>
            </Link>
          )}

          {isAdmin && (
            <Link to="/admin" className="flex items-center gap-1.5" style={{ fontSize: '13.5px', color: '#a78bfa', fontWeight: 600 }}>
              <ShieldCheck size={15} color="#a78bfa" />
              <span>Admin Panel</span>
            </Link>
          )}

          {isAuthenticated && (
            <Link to="/my-bookings" className="flex items-center gap-1.5" style={{ fontSize: '13.5px', color: 'var(--text-muted)', fontWeight: 500 }}>
              <Ticket size={15} />
              <span>My Tickets</span>
            </Link>
          )}
        </nav>

        {/* User / Auth Actions */}
        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2" style={{ background: 'var(--panel-hover)', padding: '5px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--panel-border)' }}>
                <User size={14} color="var(--primary)" />
                <span style={{ fontSize: '13px', fontWeight: 600 }}>{user?.name}</span>
                <span className={`badge ${user?.role === 'ADMIN' ? 'badge-purple' : user?.role === 'ORGANIZER' ? 'badge-primary' : 'badge-muted'}`} style={{ fontSize: '9px', padding: '2px 6px' }}>
                  {user?.role}
                </span>
              </div>
              <button onClick={handleLogout} className="btn btn-secondary btn-sm" title="Log out">
                <LogOut size={14} />
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
