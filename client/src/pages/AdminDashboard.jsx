import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { ShieldCheck, DollarSign, Ticket, Users, Calendar, AlertCircle, CheckCircle2, XCircle } from 'lucide-react';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [activeTab, setActiveTab] = useState('bookings'); // 'bookings' | 'users'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchAdminData();
  }, []);

  const fetchAdminData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, usersRes, bookingsRes] = await Promise.all([
        api.get('/admin/stats'),
        api.get('/admin/users'),
        api.get('/admin/bookings'),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data || []);
      setBookings(bookingsRes.data || []);
    } catch (err) {
      setError(err.message || 'Failed to load admin dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ padding: '80px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
        Loading platform telemetry and metrics...
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 0 80px' }}>
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4" style={{ marginBottom: '32px' }}>
        <div>
          <div className="flex items-center gap-2" style={{ marginBottom: '4px' }}>
            <span className="badge badge-purple">
              <ShieldCheck size={12} /> System Administrator
            </span>
          </div>
          <h1 style={{ fontSize: '26px', fontWeight: 800 }}>Platform Administration</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
            Real-time platform analytics, global booking records, and user management.
          </p>
        </div>

        <button onClick={fetchAdminData} className="btn btn-secondary btn-sm">
          Refresh Telemetry
        </button>
      </div>

      {error && (
        <div className="card" style={{ background: 'var(--danger-dim)', borderColor: 'var(--danger)', marginBottom: '24px', padding: '16px' }}>
          <div className="flex items-center gap-2">
            <AlertCircle size={18} color="var(--danger)" />
            <span style={{ fontSize: '14px', color: '#fca5a5' }}>{error}</span>
          </div>
        </div>
      )}

      {/* Stats Cards Grid */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4" style={{ marginBottom: '36px' }}>
          <div className="card-stat">
            <div className="flex items-center justify-between" style={{ marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Gross Revenue</span>
              <DollarSign size={18} color="var(--success)" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: 'var(--success)' }}>
              ${Number(stats.totalRevenue).toFixed(2)}
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginTop: '4px' }}>
              From {stats.confirmedBookings} confirmed bookings
            </div>
          </div>

          <div className="card-stat">
            <div className="flex items-center justify-between" style={{ marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Total Bookings</span>
              <Ticket size={18} color="var(--primary)" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800 }}>{stats.totalBookings}</div>
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginTop: '4px' }}>
              {stats.cancelledBookings} cancelled / released
            </div>
          </div>

          <div className="card-stat">
            <div className="flex items-center justify-between" style={{ marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Published Events</span>
              <Calendar size={18} color="var(--warning)" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800 }}>{stats.totalEvents}</div>
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginTop: '4px' }}>
              Across {stats.totalVenues} registered venues
            </div>
          </div>

          <div className="card-stat">
            <div className="flex items-center justify-between" style={{ marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Total Users</span>
              <Users size={18} color="var(--accent-purple)" />
            </div>
            <div style={{ fontSize: '26px', fontWeight: 800 }}>{stats.totalUsers}</div>
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginTop: '4px' }}>
              Customers, Organizers & Admins
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-2" style={{ borderBottom: '1px solid var(--panel-border)', marginBottom: '24px' }}>
        <button
          onClick={() => setActiveTab('bookings')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'bookings' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'bookings' ? 'var(--primary)' : 'var(--text-muted)',
            padding: '10px 16px',
            fontSize: '14px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          All Platform Bookings ({bookings.length})
        </button>
        <button
          onClick={() => setActiveTab('users')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'users' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'users' ? 'var(--primary)' : 'var(--text-muted)',
            padding: '10px 16px',
            fontSize: '14px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          User Directory ({users.length})
        </button>
      </div>

      {/* Tab: Bookings Table */}
      {activeTab === 'bookings' && (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Booking ID</th>
                <th>Customer</th>
                <th>Event</th>
                <th>Seats</th>
                <th>Total</th>
                <th>Status</th>
                <th>Created At</th>
              </tr>
            </thead>
            <tbody>
              {bookings.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '32px' }}>
                    No bookings recorded on the platform yet.
                  </td>
                </tr>
              ) : (
                bookings.map((b) => (
                  <tr key={b.id}>
                    <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>#{b.id}</td>
                    <td>{b.userName || `User ID: ${b.userId}`}</td>
                    <td style={{ fontWeight: 600 }}>{b.eventTitle}</td>
                    <td>
                      <div className="flex items-center gap-1" style={{ flexWrap: 'wrap' }}>
                        {b.seats?.map((s) => (
                          <span key={s.id} className="badge badge-muted" style={{ fontSize: '10px' }}>
                            {s.seatLabel}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td style={{ fontWeight: 700, color: b.status === 'CONFIRMED' ? 'var(--success)' : 'var(--text-dim)' }}>
                      ${Number(b.totalAmount).toFixed(2)}
                    </td>
                    <td>
                      <span className={`badge ${b.status === 'CONFIRMED' ? 'badge-success' : 'badge-danger'}`}>
                        {b.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                      {new Date(b.createdAt).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'short' })}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Tab: Users Table */}
      {activeTab === 'users' && (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>User ID</th>
                <th>Name</th>
                <th>Email Address</th>
                <th>Role Tier</th>
                <th>Registered Date</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>#{u.id}</td>
                  <td style={{ fontWeight: 600 }}>{u.name}</td>
                  <td style={{ color: 'var(--text-muted)' }}>{u.email}</td>
                  <td>
                    <span className={`badge ${u.role === 'ADMIN' ? 'badge-purple' : u.role === 'ORGANIZER' ? 'badge-primary' : 'badge-muted'}`}>
                      {u.role}
                    </span>
                  </td>
                  <td style={{ fontSize: '12px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Initial Seed'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
