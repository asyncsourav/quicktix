import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

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
    <div className="container" style={{ minHeight: 'calc(100vh - 180px)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 0' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '28px' }}>
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <h1 style={{ fontSize: '20px', fontWeight: 700 }}>Sign In</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginTop: '4px' }}>
            Access your QuickTix account
          </p>
        </div>

        {/* Demo Login Quick-Select */}
        <div style={{ background: 'var(--bg)', border: '1px solid var(--panel-border)', borderRadius: 'var(--radius-sm)', padding: '12px', marginBottom: '20px' }}>
          <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-dim)', textTransform: 'uppercase', fontFamily: 'var(--font-mono)', marginBottom: '8px' }}>
            Quick Demo Accounts:
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '6px' }}>
            <button
              type="button"
              onClick={() => handleDemoLogin('user@quicktix.com', 'password123')}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '11px', padding: '6px 4px', textAlign: 'center' }}
            >
              Attendee
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('organizer@quicktix.com', 'password123')}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '11px', padding: '6px 4px', textAlign: 'center' }}
            >
              Organizer
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('admin@quicktix.com', 'password123')}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '11px', padding: '6px 4px', textAlign: 'center' }}
            >
              Admin
            </button>
          </div>
        </div>

        {error && (
          <div style={{ background: 'var(--danger-dim)', border: '1px solid var(--danger)', borderRadius: 'var(--radius-sm)', padding: '10px 12px', marginBottom: '16px', color: '#f87171', fontSize: '13px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
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
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
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
            style={{ width: '100%', marginTop: '6px' }}
          >
            {loading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '13px', color: 'var(--text-muted)' }}>
          Don't have an account?{' '}
          <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 500 }}>
            Register
          </Link>
        </div>
      </div>
    </div>
  );
}
