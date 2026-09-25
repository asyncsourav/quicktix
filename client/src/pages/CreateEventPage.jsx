import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { PlusCircle, Building2, Calendar, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function CreateEventPage() {
  const { user, isOrganizer } = useAuth();
  const navigate = useNavigate();

  const [venues, setVenues] = useState([]);
  const [loadingVenues, setLoadingVenues] = useState(true);

  // Venue form state
  const [showVenueModal, setShowVenueModal] = useState(false);
  const [venueName, setVenueName] = useState('');
  const [venueAddress, setVenueAddress] = useState('');
  const [venueCapacity, setVenueCapacity] = useState(50);
  const [venueLoading, setVenueLoading] = useState(false);

  // Event form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [venueId, setVenueId] = useState('');
  const [startTime, setStartTime] = useState('');
  const [category, setCategory] = useState('Concert');
  const [basePrice, setBasePrice] = useState('49.99');
  const [eventLoading, setEventLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isOrganizer) {
      navigate('/');
      return;
    }
    fetchVenues();
  }, [isOrganizer]);

  const fetchVenues = async () => {
    setLoadingVenues(true);
    try {
      const res = await api.get('/venues');
      setVenues(res.data || []);
      if (res.data?.length > 0 && !venueId) {
        setVenueId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load venues:', err);
    } finally {
      setLoadingVenues(false);
    }
  };

  const handleCreateVenue = async (e) => {
    e.preventDefault();
    setVenueLoading(true);
    setError(null);
    try {
      const res = await api.post('/venues', {
        name: venueName,
        address: venueAddress,
        totalCapacity: Number(venueCapacity),
      });
      setShowVenueModal(false);
      setVenueName('');
      setVenueAddress('');
      await fetchVenues();
      setVenueId(res.data.id);
    } catch (err) {
      setError(err.message || 'Failed to create venue.');
    } finally {
      setVenueLoading(false);
    }
  };

  const handleCreateEvent = async (e) => {
    e.preventDefault();
    if (!venueId) {
      setError('Please select or create a venue first.');
      return;
    }

    setEventLoading(true);
    setError(null);

    try {
      const res = await api.post('/events', {
        title,
        description,
        venueId: Number(venueId),
        startTime: new Date(startTime).toISOString().slice(0, 19),
        category,
        basePrice: parseFloat(basePrice),
      });

      navigate(`/events/${res.data.id}`);
    } catch (err) {
      setError(err.message || 'Failed to create event.');
    } finally {
      setEventLoading(false);
    }
  };

  return (
    <div className="container" style={{ padding: '40px 0 80px', maxWidth: '700px' }}>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>Host a New Event</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
          Schedule events and automatically generate venue seat layouts with optimistic locking.
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

      {/* Main Event Form */}
      <div className="card" style={{ padding: '32px' }}>
        <form onSubmit={handleCreateEvent} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
              Event Title
            </label>
            <input
              type="text"
              required
              className="input"
              placeholder="e.g. Symphony Under The Stars"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
              Description
            </label>
            <textarea
              className="input"
              rows={3}
              placeholder="Describe the experience, performers, or schedule..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {/* Venue Selection / Quick Add */}
          <div>
            <div className="flex items-center justify-between" style={{ marginBottom: '6px' }}>
              <label style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)' }}>
                Venue & Location
              </label>
              <button
                type="button"
                onClick={() => setShowVenueModal(!showVenueModal)}
                style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '12px', fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
              >
                <PlusCircle size={14} /> + New Venue
              </button>
            </div>

            {venues.length === 0 ? (
              <div style={{ background: 'var(--panel-hover)', padding: '14px', borderRadius: 'var(--radius-sm)', border: '1px dashed var(--border)', textAlign: 'center', fontSize: '13px', color: 'var(--text-muted)' }}>
                No venues registered yet.{' '}
                <button type="button" onClick={() => setShowVenueModal(true)} style={{ color: 'var(--primary)', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }}>
                  Create your first venue
                </button>
              </div>
            ) : (
              <select
                className="input"
                value={venueId}
                onChange={(e) => setVenueId(e.target.value)}
                required
              >
                {venues.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.name} ({v.address}) — Capacity: {v.totalCapacity} seats
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Category & Price */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
                Category
              </label>
              <select
                className="input"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                <option value="Concert">Concert</option>
                <option value="Sports">Sports</option>
                <option value="Theater">Theater</option>
                <option value="Conference">Conference</option>
                <option value="Comedy">Comedy</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
                Base Price per Seat ($)
              </label>
              <input
                type="number"
                step="0.01"
                min="1"
                required
                className="input"
                value={basePrice}
                onChange={(e) => setBasePrice(e.target.value)}
              />
            </div>
          </div>

          {/* Date & Time */}
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: 'var(--text-muted)' }}>
              Start Date & Time
            </label>
            <input
              type="datetime-local"
              required
              className="input"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
            />
          </div>

          <button
            type="submit"
            disabled={eventLoading || venues.length === 0}
            className="btn btn-primary"
            style={{ padding: '14px', marginTop: '10px' }}
          >
            <Calendar size={16} />
            <span>{eventLoading ? 'Generating Seat Layout...' : 'Publish Event & Generate Seats'}</span>
          </button>
        </form>
      </div>

      {/* New Venue Modal */}
      {showVenueModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '20px' }}>
          <div className="card" style={{ width: '100%', maxWidth: '440px', padding: '28px', background: 'var(--panel)', boxShadow: 'var(--shadow-lg)' }}>
            <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Building2 size={18} color="var(--primary)" />
              <span>Create New Venue</span>
            </h3>

            <form onSubmit={handleCreateVenue} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: 'var(--text-muted)' }}>
                  Venue Name
                </label>
                <input
                  type="text"
                  required
                  className="input"
                  placeholder="e.g. Grand Arena"
                  value={venueName}
                  onChange={(e) => setVenueName(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: 'var(--text-muted)' }}>
                  Address / City
                </label>
                <input
                  type="text"
                  required
                  className="input"
                  placeholder="e.g. 100 Park Ave, New York"
                  value={venueAddress}
                  onChange={(e) => setVenueAddress(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px', color: 'var(--text-muted)' }}>
                  Total Seating Capacity
                </label>
                <input
                  type="number"
                  min="10"
                  max="1000"
                  required
                  className="input"
                  value={venueCapacity}
                  onChange={(e) => setVenueCapacity(e.target.value)}
                />
                <span style={{ fontSize: '11px', color: 'var(--text-dim)', marginTop: '4px', display: 'block' }}>
                  Auto-generates individual seat rows (A1..A10, B1..B10) on event creation.
                </span>
              </div>

              <div className="flex items-center justify-end gap-2" style={{ marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setShowVenueModal(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={venueLoading}
                  className="btn btn-primary btn-sm"
                >
                  {venueLoading ? 'Creating...' : 'Save Venue'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
