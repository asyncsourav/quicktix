import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { AlertCircle } from 'lucide-react';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [activeTab, setActiveTab] = useState('bookings');
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
      setError(err.message || 'Failed to load admin data.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
        Loading platform stats...
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '36px 0 60px' }}>
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4" style={{ marginBottom: '28px' }}>
        <div>
          <h1 style={{ fontSize: '22px', fontWeight: 700 }}>Admin Dashboard</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '13.5px' }}>
            Platform metrics, booking history, and registered users.
          </p>
        </div>

        <button onClick={fetchAdminData} className="btn btn-secondary btn-sm">
          Refresh
        </button>
      </div>

      {error && (
        <div className="card" style={{ background: 'var(--danger-dim)', borderColor: 'var(--danger)', marginBottom: '20px', padding: '12px 16px' }}>
          <div className="flex items-center gap-2" style={{ color: 'var(--danger)', fontSize: '13px' }}>
            <AlertCircle size={15} />
            <span>{error}</span>
          </div>
        </div>
      )}

      {/* Stats Grid */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4" style={{ marginBottom: '28px' }}>
          <div className="card-stat">
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginBottom: '4px', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Gross Revenue</div>
            <div style={{ fontSize: '22px', fontWeight: 700, color: 'var(--success)' }}>
              ${Number(stats.totalRevenue).toFixed(2)}
            </div>
            <div style={{ fontSize: '11.5px', color: 'var(--text-dim)', marginTop: '2px' }}>
              {stats.confirmedBookings} confirmed bookings
            </div>
          </div>

          <div className="card-stat">
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginBottom: '4px', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Total Bookings</div>
            <div style={{ fontSize: '22px', fontWeight: 700, color: 'var(--text-main)' }}>{stats.totalBookings}</div>
            <div style={{ fontSize: '11.5px', color: 'var(--text-dim)', marginTop: '2px' }}>
              {stats.cancelledBookings} cancelled
            </div>
          </div>

          <div className="card-stat">
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginBottom: '4px', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Events</div>
            <div style={{ fontSize: '22px', fontWeight: 700, color: 'var(--text-main)' }}>{stats.totalEvents}</div>
            <div style={{ fontSize: '11.5px', color: 'var(--text-dim)', marginTop: '2px' }}>
              Across {stats.totalVenues} venues
            </div>
          </div>

          <div className="card-stat">
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginBottom: '4px', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Users</div>
            <div style={{ fontSize: '22px', fontWeight: 700, color: 'var(--text-main)' }}>{stats.totalUsers}</div>
            <div style={{ fontSize: '11.5px', color: 'var(--text-dim)', marginTop: '2px' }}>
              Registered accounts
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-2" style={{ borderBottom: '1px solid var(--panel-border)', marginBottom: '20px' }}>
        <button
          onClick={() => setActiveTab('bookings')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'bookings' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'bookings' ? 'var(--text-main)' : 'var(--text-muted)',
            padding: '8px 14px',
            fontSize: '13.5px',
            fontWeight: activeTab === 'bookings' ? 600 : 500,
            cursor: 'pointer',
          }}
        >
          Bookings ({bookings.length})
        </button>
        <button
          onClick={() => setActiveTab('users')}
          style={{
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'users' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'users' ? 'var(--text-main)' : 'var(--text-muted)',
            padding: '8px 14px',
            fontSize: '13.5px',
            fontWeight: activeTab === 'users' ? 600 : 500,
            cursor: 'pointer',
          }}
        >
          Users ({users.length})
        </button>
      </div>

      {/* Bookings Table */}
      {activeTab === 'bookings' && (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>User</th>
                <th>Event</th>
                <th>Seats</th>
                <th>Total</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {bookings.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '24px' }}>
                    No bookings found.
                  </td>
                </tr>
              ) : (
                bookings.map((b) => (
                  <tr key={b.id}>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>#{b.id}</td>
                    <td>{b.userName || `User #${b.userId}`}</td>
                    <td>{b.eventTitle}</td>
                    <td>
                      <div className="flex items-center gap-1" style={{ flexWrap: 'wrap' }}>
                        {b.seats?.map((s) => (
                          <span key={s.id} className="badge badge-muted" style={{ fontSize: '10px' }}>
                            {s.seatLabel}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td style={{ fontWeight: 600, color: 'var(--text-main)' }}>${Number(b.totalAmount).toFixed(2)}</td>
                    <td>
                      <span className={`badge ${b.status === 'CONFIRMED' ? 'badge-success' : 'badge-danger'}`}>
                        {b.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-dim)' }}>
                      {new Date(b.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Users Table */}
      {activeTab === 'users' && (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>#{u.id}</td>
                  <td style={{ color: 'var(--text-main)', fontWeight: 500 }}>{u.name}</td>
                  <td style={{ color: 'var(--text-muted)' }}>{u.email}</td>
                  <td>
                    <span className="badge badge-muted">{u.role}</span>
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
