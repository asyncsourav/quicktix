import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { MapPin, Calendar, Check, AlertCircle, ArrowLeft } from 'lucide-react';

export default function EventDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [successBooking, setSuccessBooking] = useState(null);

  useEffect(() => {
    fetchEventAndSeats();
  }, [id]);

  const fetchEventAndSeats = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const [eventRes, seatsRes] = await Promise.all([
        api.get(`/events/${id}`),
        api.get(`/events/${id}/seats`),
      ]);
      setEvent(eventRes.data);
      setSeats(seatsRes.data || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load event details.');
    } finally {
      setLoading(false);
    }
  };

  const toggleSeatSelection = (seat) => {
    if (seat.status !== 'AVAILABLE') return;

    setSelectedSeatIds((prev) =>
      prev.includes(seat.id)
        ? prev.filter((sId) => sId !== seat.id)
        : [...prev, seat.id]
    );
  };

  const selectedSeats = seats.filter((s) => selectedSeatIds.includes(s.id));
  const totalPrice = selectedSeats.reduce((sum, s) => sum + Number(s.price), 0);

  const handleConfirmBooking = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (selectedSeatIds.length === 0) return;

    setBookingLoading(true);
    setErrorMessage(null);

    try {
      const res = await api.post('/bookings', {
        eventId: Number(id),
        seatIds: selectedSeatIds,
      });

      setSuccessBooking(res.data);
      await fetchEventAndSeats();
      setSelectedSeatIds([]);
    } catch (err) {
      setErrorMessage(err.message || 'Seat unavailable or collision detected. Please choose available seats.');
      await fetchEventAndSeats();
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
        Loading seat map...
      </div>
    );
  }

  if (!event) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <h2 style={{ marginBottom: '14px', fontSize: '18px' }}>Event Not Found</h2>
        <button onClick={() => navigate('/')} className="btn btn-secondary">
          <ArrowLeft size={15} /> Back to Events
        </button>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '30px 0 60px' }}>
      {/* Back button */}
      <button onClick={() => navigate('/')} className="btn btn-secondary btn-sm" style={{ marginBottom: '16px' }}>
        <ArrowLeft size={13} /> Back to Events
      </button>

      {/* Event Header Card */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
          <div>
            <div className="flex items-center gap-2" style={{ marginBottom: '6px' }}>
              <span className="badge badge-primary">{event.category}</span>
            </div>
            <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '6px' }}>{event.title}</h1>
            <div className="flex items-center gap-5" style={{ fontSize: '13px', color: 'var(--text-muted)', flexWrap: 'wrap' }}>
              <span className="flex items-center gap-1.5"><MapPin size={14} color="var(--text-dim)" /> {event.venueName} · {event.venueAddress}</span>
              <span className="flex items-center gap-1.5"><Calendar size={14} color="var(--text-dim)" /> {new Date(event.startTime).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '11px', color: 'var(--text-dim)', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Price per seat</div>
            <div style={{ fontSize: '20px', fontWeight: 700, color: 'var(--text-main)' }}>${Number(event.basePrice).toFixed(2)}</div>
          </div>
        </div>
      </div>

      {/* Success Notification */}
      {successBooking && (
        <div className="card" style={{ background: 'var(--success-dim)', borderColor: 'var(--success)', marginBottom: '20px', padding: '16px' }}>
          <div className="flex items-center justify-between">
            <div>
              <div style={{ fontWeight: 600, color: 'var(--success)', fontSize: '14px' }}>Booking Confirmed (ID #{successBooking.id})</div>
              <div style={{ fontSize: '12.5px', color: 'var(--text-muted)' }}>Total: ${Number(successBooking.totalAmount).toFixed(2)}</div>
            </div>
            <button onClick={() => navigate('/my-bookings')} className="btn btn-primary btn-sm">
              View My Bookings
            </button>
          </div>
        </div>
      )}

      {/* Error Alert */}
      {errorMessage && (
        <div className="card" style={{ background: 'var(--danger-dim)', borderColor: 'var(--danger)', marginBottom: '20px', padding: '14px' }}>
          <div className="flex items-center gap-2" style={{ color: 'var(--danger)', fontSize: '13px' }}>
            <AlertCircle size={16} />
            <span>{errorMessage}</span>
          </div>
        </div>
      )}

      {/* Main Grid: Seat Map + Summary Sidebar */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Seat Map */}
        <div style={{ gridColumn: 'span 2' }}>
          <div className="seat-map-container">
            <div className="screen-indicator"></div>
            <div className="screen-text">STAGE / SCREEN</div>

            {/* Legend with clean spacing */}
            <div className="flex items-center justify-center gap-6" style={{ margin: '20px 0 24px', flexWrap: 'wrap' }}>
              <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                <div className="seat seat-available" style={{ width: '16px', height: '16px', cursor: 'default' }}></div>
                <span>Available</span>
              </div>
              <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                <div className="seat seat-selected" style={{ width: '16px', height: '16px', cursor: 'default' }}></div>
                <span>Selected</span>
              </div>
              <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                <div className="seat seat-held" style={{ width: '16px', height: '16px', cursor: 'default' }}></div>
                <span>Held</span>
              </div>
              <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                <div className="seat seat-booked" style={{ width: '16px', height: '16px', cursor: 'default' }}></div>
                <span>Booked</span>
              </div>
            </div>

            {/* Seat Grid */}
            <div className="seats-grid">
              {seats.map((seat) => {
                const isSelected = selectedSeatIds.includes(seat.id);
                let seatClass = 'seat-available';

                if (isSelected) {
                  seatClass = 'seat-selected';
                } else if (seat.status === 'HELD') {
                  seatClass = 'seat-held';
                } else if (seat.status === 'BOOKED') {
                  seatClass = 'seat-booked';
                }

                return (
                  <button
                    key={seat.id}
                    onClick={() => toggleSeatSelection(seat)}
                    disabled={seat.status !== 'AVAILABLE'}
                    className={`seat ${seatClass}`}
                    title={`Seat ${seat.seatLabel} - $${seat.price}`}
                  >
                    {isSelected ? <Check size={11} /> : seat.seatLabel}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div>
          <div className="card" style={{ position: 'sticky', top: '80px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: 600, marginBottom: '14px' }}>
              Booking Summary
            </h3>

            {selectedSeats.length === 0 ? (
              <div style={{ padding: '20px 0', textAlign: 'center', color: 'var(--text-dim)', fontSize: '13px' }}>
                Select one or more available seats on the map.
              </div>
            ) : (
              <div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                  {selectedSeats.map((seat) => (
                    <div key={seat.id} className="flex items-center justify-between" style={{ fontSize: '12.5px', background: 'var(--bg)', padding: '6px 10px', borderRadius: 'var(--radius-sm)' }}>
                      <span style={{ fontFamily: 'var(--font-mono)' }}>Seat {seat.seatLabel}</span>
                      <span style={{ color: 'var(--text-muted)' }}>${Number(seat.price).toFixed(2)}</span>
                    </div>
                  ))}
                </div>

                <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '12px', marginBottom: '16px' }}>
                  <div className="flex items-center justify-between" style={{ fontSize: '13px', marginBottom: '4px', color: 'var(--text-muted)' }}>
                    <span>Selected Seats:</span>
                    <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>{selectedSeats.length}</span>
                  </div>
                  <div className="flex items-center justify-between" style={{ fontSize: '16px', fontWeight: 700 }}>
                    <span>Total:</span>
                    <span>${totalPrice.toFixed(2)}</span>
                  </div>
                </div>

                <button
                  onClick={handleConfirmBooking}
                  disabled={bookingLoading}
                  className="btn btn-primary"
                  style={{ width: '100%' }}
                >
                  {bookingLoading ? 'Processing...' : isAuthenticated ? 'Confirm Booking' : 'Sign In to Book'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
