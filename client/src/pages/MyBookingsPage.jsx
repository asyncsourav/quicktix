import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { Ticket, Calendar, MapPin, AlertCircle, Trash2, ArrowRight } from 'lucide-react';

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
    if (!window.confirm('Are you sure you want to cancel this booking? All seats will be immediately released.')) {
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
    <div className="container" style={{ padding: '40px 0 80px' }}>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>My Bookings & Tickets</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
          View your confirmed ticket reservations or cancel upcoming bookings to release seats.
        </p>
      </div>

      {error && (
        <div className="card" style={{ background: 'var(--danger-dim)', borderColor: 'var(--danger)', marginBottom: '24px', padding: '16px' }}>
          <div className="flex items-center gap-2">
            <AlertCircle size={18} color="var(--danger)" />
            <span style={{ fontSize: '14px', color: '#fca5a5' }}>{error}</span>
          </div>
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
          Loading your tickets...
        </div>
      ) : bookings.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <Ticket size={48} color="var(--text-dim)" style={{ margin: '0 auto 16px' }} />
          <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '8px' }}>No Bookings Yet</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '14px', maxWidth: '420px', margin: '0 auto 24px' }}>
            You haven't booked any event tickets yet. Explore upcoming concerts, sports, and theater shows!
          </p>
          <Link to="/" className="btn btn-primary">
            <span>Browse Events</span>
            <ArrowRight size={15} />
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {bookings.map((b) => {
            const isConfirmed = b.status === 'CONFIRMED';
            return (
              <div key={b.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3" style={{ borderBottom: '1px solid var(--panel-border)', paddingBottom: '14px' }}>
                  <div>
                    <div className="flex items-center gap-2" style={{ marginBottom: '4px' }}>
                      <span className={`badge ${isConfirmed ? 'badge-success' : 'badge-danger'}`}>
                        {b.status}
                      </span>
                      <span style={{ fontSize: '13px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                        Booking #{b.id}
                      </span>
                    </div>
                    <h2 style={{ fontSize: '20px', fontWeight: 700 }}>{b.eventTitle}</h2>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '20px', fontWeight: 800, color: isConfirmed ? 'var(--success)' : 'var(--text-dim)' }}>
                      ${Number(b.totalAmount).toFixed(2)}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                      Booked on {new Date(b.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                {/* Seat Badges */}
                <div>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>
                    Reserved Seats ({b.seats?.length || 0}):
                  </div>
                  <div className="flex items-center gap-2" style={{ flexWrap: 'wrap' }}>
                    {b.seats?.map((seat) => (
                      <span key={seat.id} className="badge badge-primary" style={{ padding: '6px 12px', fontSize: '12px' }}>
                        Seat {seat.seatLabel} (${Number(seat.price).toFixed(2)})
                      </span>
                    ))}
                  </div>
                </div>

                {/* Actions */}
                {isConfirmed && (
                  <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '12px', display: 'flex', justifyContent: 'flex-end' }}>
                    <button
                      onClick={() => handleCancelBooking(b.id)}
                      disabled={cancelLoadingId === b.id}
                      className="btn btn-danger btn-sm"
                    >
                      <Trash2 size={14} />
                      <span>{cancelLoadingId === b.id ? 'Releasing Seats...' : 'Cancel Booking & Release Seats'}</span>
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
