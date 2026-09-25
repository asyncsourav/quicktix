import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { Ticket, Calendar, MapPin, AlertCircle, Trash2, ArrowRight, QrCode } from 'lucide-react';

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
    if (!window.confirm('Are you sure you want to cancel this booking? Reserved seats will be immediately released back to the event inventory.')) {
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
    <div className="container" style={{ padding: '40px 0 80px', maxWidth: '840px' }}>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>My Event Passes & Bookings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
          Access your confirmed ticket passes or cancel upcoming reservations to release seats.
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
          Retrieving your ticket passes...
        </div>
      ) : bookings.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <Ticket size={48} color="var(--text-dim)" style={{ margin: '0 auto 16px' }} />
          <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '8px' }}>No Active Bookings</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '14px', maxWidth: '420px', margin: '0 auto 24px' }}>
            You haven't reserved any tickets yet. Explore upcoming stadium concerts, sports games, and tech summits!
          </p>
          <Link to="/" className="btn btn-primary">
            <span>Browse Events</span>
            <ArrowRight size={15} />
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {bookings.map((b) => {
            const isConfirmed = b.status === 'CONFIRMED';
            return (
              <div key={b.id} className="ticket-card">
                {/* Ticket Top Header */}
                <div className="ticket-header">
                  <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-2">
                    <div>
                      <div className="flex items-center gap-2" style={{ marginBottom: '6px' }}>
                        <span className={`badge ${isConfirmed ? 'badge-success' : 'badge-danger'}`}>
                          {b.status} PASS
                        </span>
                        <span style={{ fontSize: '12px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                          REF #{b.id}
                        </span>
                      </div>
                      <h2 style={{ fontSize: '20px', fontWeight: 800, lineHeight: 1.3 }}>{b.eventTitle}</h2>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '22px', fontWeight: 800, color: isConfirmed ? 'var(--success)' : 'var(--text-dim)' }}>
                        ${Number(b.totalAmount).toFixed(2)}
                      </div>
                      <div style={{ fontSize: '11px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                        Purchased {new Date(b.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Ticket Body */}
                <div className="ticket-body">
                  <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                    <div>
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', fontFamily: 'var(--font-mono)', marginBottom: '8px' }}>
                        Assigned Seats ({b.seats?.length || 0}):
                      </div>
                      <div className="flex items-center gap-2" style={{ flexWrap: 'wrap' }}>
                        {b.seats?.map((seat) => (
                          <span key={seat.id} className="badge badge-primary" style={{ padding: '6px 12px', fontSize: '12.5px' }}>
                            Seat {seat.seatLabel} · ${Number(seat.price).toFixed(2)}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* QR Code Mock */}
                    <div className="flex items-center gap-3" style={{ background: 'var(--bg-subtle)', padding: '8px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--panel-border)' }}>
                      <QrCode size={32} color="var(--primary)" />
                      <div>
                        <div style={{ fontSize: '11px', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>PASS CODE</div>
                        <div style={{ fontSize: '10px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>QT-{b.id}-{b.userId}</div>
                      </div>
                    </div>
                  </div>

                  {/* Cancel Button */}
                  {isConfirmed && (
                    <div style={{ borderTop: '1px solid var(--panel-border)', marginTop: '20px', paddingTop: '14px', display: 'flex', justifyContent: 'flex-end' }}>
                      <button
                        onClick={() => handleCancelBooking(b.id)}
                        disabled={cancelLoadingId === b.id}
                        className="btn btn-danger btn-sm"
                      >
                        <Trash2 size={13} />
                        <span>{cancelLoadingId === b.id ? 'Releasing Seats...' : 'Cancel Reservation'}</span>
                      </button>
                    </div>
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
