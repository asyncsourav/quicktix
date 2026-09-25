import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('USER');
  const [error, setError] = useState(null);
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await register(name, email, password, role);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Registration failed.');
    }
  };

  return (
    <div className="container" style={{ minHeight: 'calc(100vh - 180px)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 0' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '28px' }}>
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <h1 style={{ fontSize: '20px', fontWeight: 700 }}>Create Account</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginTop: '4px' }}>
            Register on QuickTix
          </p>
        </div>

        {error && (
          <div style={{ background: 'var(--danger-dim)', border: '1px solid var(--danger)', borderRadius: 'var(--radius-sm)', padding: '10px 12px', marginBottom: '16px', color: '#f87171', fontSize: '13px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
              Full Name
            </label>
            <input
              type="text"
              required
              className="input"
              placeholder="e.g. Sourav Kumar"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

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
              Password (min 6 chars)
            </label>
            <input
              type="password"
              required
              minLength={6}
              className="input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
              Account Role
            </label>
            <select
              className="input"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              style={{ cursor: 'pointer' }}
            >
              <option value="USER">Customer (Book & Attend Events)</option>
              <option value="ORGANIZER">Event Organizer (Manage Venues & Events)</option>
            </select>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '6px' }}
          >
            {loading ? 'Registering...' : 'Create Account'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '13px', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 500 }}>
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}
