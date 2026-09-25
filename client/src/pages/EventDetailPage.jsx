import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { MapPin, Calendar, Check, AlertCircle, ShieldCheck, Ticket, ArrowLeft } from 'lucide-react';

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
      // Refresh seat map to show newly booked seats
      await fetchEventAndSeats();
      setSelectedSeatIds([]);
    } catch (err) {
      // Catch optimistic locking or seat unavailable errors from backend
      setErrorMessage(err.message || 'Booking collision detected. Please re-select available seats.');
      // Refresh seats to see what was taken
      await fetchEventAndSeats();
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ padding: '80px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
        Loading interactive seat map...
      </div>
    );
  }

  if (!event) {
    return (
      <div className="container" style={{ padding: '80px 0', textAlign: 'center' }}>
        <h2 style={{ marginBottom: '16px' }}>Event Not Found</h2>
        <button onClick={() => navigate('/')} className="btn btn-secondary">
          <ArrowLeft size={16} /> Back to Events
        </button>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '36px 0 80px' }}>
      {/* Back link & Event Header */}
      <button onClick={() => navigate('/')} className="btn btn-secondary btn-sm" style={{ marginBottom: '20px' }}>
        <ArrowLeft size={14} /> Back to Events
      </button>

      <div className="card" style={{ marginBottom: '32px' }}>
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <div className="flex items-center gap-2" style={{ marginBottom: '8px' }}>
              <span className="badge badge-primary">{event.category}</span>
              <span className="badge badge-success">Optimistic Locking Protected</span>
            </div>
            <h1 style={{ fontSize: '24px', fontWeight: 800, marginBottom: '6px' }}>{event.title}</h1>
            <div className="flex items-center gap-6" style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
              <span className="flex items-center gap-1.5"><MapPin size={15} color="var(--text-dim)" /> {event.venueName} · {event.venueAddress}</span>
              <span className="flex items-center gap-1.5"><Calendar size={15} color="var(--text-dim)" /> {new Date(event.startTime).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', textTransform: 'uppercase', fontFamily: 'var(--font-mono)' }}>Base Price</div>
            <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--success)' }}>${Number(event.basePrice).toFixed(2)}</div>
          </div>
        </div>
      </div>

      {/* Success Confirmation Modal/Banner */}
      {successBooking && (
        <div className="card" style={{ background: 'var(--success-dim)', borderColor: 'var(--success)', marginBottom: '24px', padding: '20px' }}>
          <div className="flex items-center gap-3">
            <div style={{ background: 'var(--success)', color: '#fff', padding: '6px', borderRadius: '50%', display: 'flex' }}>
              <Check size={18} />
            </div>
            <div style={{ flex: 1 }}>
              <h3 style={{ fontSize: '16px', fontWeight: 700, color: '#a7f3d0' }}>Booking Confirmed Successfully!</h3>
              <p style={{ fontSize: '13px', color: '#d1fae5' }}>
                Booking ID: #{successBooking.id} · Total: ${Number(successBooking.totalAmount).toFixed(2)} · Status: {successBooking.status}
              </p>
            </div>
            <button onClick={() => navigate('/my-bookings')} className="btn btn-primary btn-sm">
              View Ticket
            </button>
          </div>
        </div>
      )}

      {/* Error Banner */}
      {errorMessage && (
        <div className="card" style={{ background: 'var(--danger-dim)', borderColor: 'var(--danger)', marginBottom: '24px', padding: '16px 20px' }}>
          <div className="flex items-center gap-3">
            <AlertCircle size={20} color="var(--danger)" />
            <div style={{ fontSize: '14px', color: '#fca5a5' }}>{errorMessage}</div>
          </div>
        </div>
      )}

      {/* Main Grid: Seat Map + Booking Sidebar */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Seat Map View */}
        <div className="md:grid-cols-2" style={{ gridColumn: 'span 2' }}>
          <div className="seat-map-container">
            {/* Screen / Stage */}
            <div className="screen-indicator"></div>
            <div className="screen-text">STAGE / SCREEN</div>

            {/* Legend */}
            <div className="flex items-center justify-center gap-6" style={{ margin: '24px 0 28px', fontSize: '12px', color: 'var(--text-muted)' }}>
              <div className="flex items-center gap-2">
                <div className="seat seat-available" style={{ width: '18px', height: '18px', cursor: 'default' }}></div>
                <span>Available</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="seat seat-selected" style={{ width: '18px', height: '18px', cursor: 'default' }}></div>
                <span>Selected</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="seat seat-held" style={{ width: '18px', height: '18px', cursor: 'default' }}></div>
                <span>Held</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="seat seat-booked" style={{ width: '18px', height: '18px', cursor: 'default' }}></div>
                <span>Sold</span>
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
                    title={`Seat ${seat.seatLabel} - $${seat.price} (${seat.status})`}
                  >
                    {isSelected ? <Check size={12} /> : seat.seatLabel}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Booking Sidebar */}
        <div>
          <div className="card" style={{ position: 'sticky', top: '90px' }}>
            <h3 style={{ fontSize: '17px', fontWeight: 700, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Ticket size={18} color="var(--primary)" />
              <span>Booking Summary</span>
            </h3>

            {selectedSeats.length === 0 ? (
              <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text-dim)', fontSize: '13px' }}>
                Click on available seats on the map to add them to your reservation.
              </div>
            ) : (
              <div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
                  {selectedSeats.map((seat) => (
                    <div key={seat.id} className="flex items-center justify-between" style={{ fontSize: '13px', background: 'var(--panel-hover)', padding: '8px 12px', borderRadius: 'var(--radius-sm)' }}>
                      <span style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>Seat {seat.seatLabel}</span>
                      <span style={{ color: 'var(--text-muted)' }}>${Number(seat.price).toFixed(2)}</span>
                    </div>
                  ))}
                </div>

                <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '16px', marginBottom: '20px' }}>
                  <div className="flex items-center justify-between" style={{ fontSize: '14px', marginBottom: '6px', color: 'var(--text-muted)' }}>
                    <span>Seats Selected:</span>
                    <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>{selectedSeats.length}</span>
                  </div>
                  <div className="flex items-center justify-between" style={{ fontSize: '18px', fontWeight: 800 }}>
                    <span>Total Amount:</span>
                    <span style={{ color: 'var(--success)' }}>${totalPrice.toFixed(2)}</span>
                  </div>
                </div>

                <button
                  onClick={handleConfirmBooking}
                  disabled={bookingLoading}
                  className="btn btn-primary"
                  style={{ width: '100%', padding: '12px' }}
                >
                  <ShieldCheck size={16} />
                  <span>{bookingLoading ? 'Securing Seats...' : isAuthenticated ? 'Confirm & Purchase' : 'Sign In to Book'}</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
