import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { AlertCircle } from 'lucide-react';

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelLoadingId, setCancelLoadingId] = useState(null);

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/bookings/me');
      setBookings(res.data || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch bookings.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Cancel this booking and release seats back to the event?')) {
      return;
    }

    setCancelLoadingId(bookingId);
    try {
      await api.delete(`/bookings/${bookingId}`);
      await fetchBookings();
    } catch (err) {
      alert(err.message || 'Failed to cancel booking.');
    } finally {
      setCancelLoadingId(null);
    }
  };

  return (
    <div className="container" style={{ padding: '36px 0 60px', maxWidth: '800px' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>My Bookings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '13.5px' }}>
          Manage your confirmed event tickets.
        </p>
      </div>

      {error && (
        <div className="card" style={{ background: 'var(--danger-dim)', borderColor: 'var(--danger)', marginBottom: '20px', padding: '12px 16px' }}>
          <div className="flex items-center gap-2" style={{ color: '#f87171', fontSize: '13px' }}>
            <AlertCircle size={15} />
            <span>{error}</span>
          </div>
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)', fontSize: '14px' }}>
          Loading your bookings...
        </div>
      ) : bookings.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '6px' }}>No bookings found</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '13.5px', marginBottom: '18px' }}>
            You have not booked any tickets yet.
          </p>
          <Link to="/" className="btn btn-primary btn-sm">
            Browse Events
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {bookings.map((b) => {
            const isConfirmed = b.status === 'CONFIRMED';
            return (
              <div key={b.id} className="card">
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3" style={{ borderBottom: '1px solid var(--panel-border)', paddingBottom: '12px', marginBottom: '12px' }}>
                  <div>
                    <div className="flex items-center gap-2" style={{ marginBottom: '4px' }}>
                      <span className={`badge ${isConfirmed ? 'badge-success' : 'badge-danger'}`}>
                        {b.status}
                      </span>
                      <span style={{ fontSize: '12px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                        #{b.id}
                      </span>
                    </div>
                    <h2 style={{ fontSize: '17px', fontWeight: 700 }}>{b.eventTitle}</h2>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '18px', fontWeight: 700 }}>
                      ${Number(b.totalAmount).toFixed(2)}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                      {new Date(b.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                {/* Seats List */}
                <div className="flex items-center justify-between" style={{ flexWrap: 'wrap', gap: '12px' }}>
                  <div>
                    <span style={{ fontSize: '12px', color: 'var(--text-dim)', marginRight: '8px' }}>Seats:</span>
                    <span className="flex items-center gap-1.5" style={{ display: 'inline-flex' }}>
                      {b.seats?.map((seat) => (
                        <span key={seat.id} className="badge badge-muted">
                          {seat.seatLabel}
                        </span>
                      ))}
                    </span>
                  </div>

                  {isConfirmed && (
                    <button
                      onClick={() => handleCancelBooking(b.id)}
                      disabled={cancelLoadingId === b.id}
                      className="btn btn-danger btn-sm"
                    >
                      {cancelLoadingId === b.id ? 'Cancelling...' : 'Cancel Booking'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
