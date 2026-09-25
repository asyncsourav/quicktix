import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Ticket, AlertCircle, LogIn, Shield, Users, Sparkles } from 'lucide-react';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      const user = await login(email, password);
      if (user.role === 'ADMIN') {
        navigate('/admin');
      } else if (user.role === 'ORGANIZER') {
        navigate('/organizer/create');
      } else {
        navigate('/');
      }
    } catch (err) {
      setError(err.message || 'Invalid email or password.');
    }
  };

  const handleDemoLogin = async (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setError(null);
    try {
      const user = await login(demoEmail, demoPassword);
      if (user.role === 'ADMIN') {
        navigate('/admin');
      } else if (user.role === 'ORGANIZER') {
        navigate('/organizer/create');
      } else {
        navigate('/');
      }
    } catch (err) {
      setError(err.message || 'Demo login failed. Make sure backend is running.');
    }
  };

  return (
    <div className="container" style={{ minHeight: 'calc(100vh - 200px)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 0' }}>
      <div className="card" style={{ width: '100%', maxWidth: '440px', padding: '36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{ background: 'var(--primary)', color: '#fff', padding: '8px', borderRadius: '10px', display: 'inline-flex', marginBottom: '12px' }}>
            <Ticket size={24} />
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: 800 }}>Welcome Back</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginTop: '4px' }}>
            Sign in to manage bookings, host events, or administer the platform
          </p>
        </div>

        {/* 1-Click Demo Accounts Quick-Fill */}
        <div style={{ background: 'var(--bg-subtle)', border: '1px solid var(--panel-border)', borderRadius: 'var(--radius)', padding: '14px', marginBottom: '24px' }}>
          <div className="flex items-center gap-1.5" style={{ fontSize: '11.5px', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', fontFamily: 'var(--font-mono)', marginBottom: '10px' }}>
            <Sparkles size={13} color="var(--primary)" />
            <span>Instant Demo Accounts (1-Click)</span>
          </div>

          <div className="flex flex-col gap-2">
            <button
              type="button"
              onClick={() => handleDemoLogin('user@quicktix.com', 'password123')}
              className="btn btn-secondary btn-sm flex items-center justify-between"
              style={{ width: '100%', fontSize: '12px', padding: '8px 12px' }}
            >
              <div className="flex items-center gap-2">
                <Users size={14} color="var(--primary)" />
                <span style={{ fontWeight: 600 }}>Attendee / Customer</span>
              </div>
              <span style={{ fontSize: '11px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>user@quicktix.com</span>
            </button>

            <button
              type="button"
              onClick={() => handleDemoLogin('organizer@quicktix.com', 'password123')}
              className="btn btn-secondary btn-sm flex items-center justify-between"
              style={{ width: '100%', fontSize: '12px', padding: '8px 12px' }}
            >
              <div className="flex items-center gap-2">
                <Ticket size={14} color="var(--warning)" />
                <span style={{ fontWeight: 600 }}>Event Organizer</span>
              </div>
              <span style={{ fontSize: '11px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>organizer@quicktix.com</span>
            </button>

            <button
              type="button"
              onClick={() => handleDemoLogin('admin@quicktix.com', 'password123')}
              className="btn btn-secondary btn-sm flex items-center justify-between"
              style={{ width: '100%', fontSize: '12px', padding: '8px 12px', borderColor: 'rgba(139, 92, 246, 0.3)' }}
            >
              <div className="flex items-center gap-2">
                <Shield size={14} color="var(--accent-purple)" />
                <span style={{ fontWeight: 600, color: '#a78bfa' }}>Platform Admin</span>
              </div>
              <span style={{ fontSize: '11px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>admin@quicktix.com</span>
            </button>
          </div>
        </div>

        {error && (
          <div style={{ background: 'var(--danger-dim)', border: '1px solid var(--danger)', borderRadius: 'var(--radius-sm)', padding: '12px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px', color: '#fca5a5', fontSize: '13px' }}>
            <AlertCircle size={16} color="var(--danger)" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
              Email Address
            </label>
            <input
              type="email"
              required
              className="input"
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
              Password
            </label>
            <input
              type="password"
              required
              className="input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', padding: '12px', marginTop: '8px' }}
          >
            <LogIn size={16} />
            <span>{loading ? 'Authenticating...' : 'Sign In'}</span>
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '13px', color: 'var(--text-muted)' }}>
          Don't have an account?{' '}
          <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 600 }}>
            Create one
          </Link>
        </div>
      </div>
    </div>
  );
}
