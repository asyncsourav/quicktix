import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { AlertCircle } from 'lucide-react';

export default function CreateEventPage() {
  const { isOrganizer } = useAuth();
  const navigate = useNavigate();

  const [venues, setVenues] = useState([]);
  const [loadingVenues, setLoadingVenues] = useState(true);

  // Venue form modal
  const [showVenueModal, setShowVenueModal] = useState(false);
  const [venueName, setVenueName] = useState('');
  const [venueAddress, setVenueAddress] = useState('');
  const [venueCapacity, setVenueCapacity] = useState(50);
  const [venueLoading, setVenueLoading] = useState(false);

  // Event form
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
    <div className="container" style={{ padding: '36px 0 60px', maxWidth: '640px' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 700 }}>Create New Event</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '13.5px' }}>
          Publish an event and generate seat layout automatically.
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

      {/* Event Form */}
      <div className="card" style={{ padding: '24px' }}>
        <form onSubmit={handleCreateEvent} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
              Event Title
            </label>
            <input
              type="text"
              required
              className="input"
              placeholder="e.g. Symphony Live in Concert"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
              Description
            </label>
            <textarea
              className="input"
              rows={3}
              placeholder="Event overview, artist lineup, schedule..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div>
            <div className="flex items-center justify-between" style={{ marginBottom: '4px' }}>
              <label style={{ fontSize: '12.5px', fontWeight: 500, color: 'var(--text-muted)' }}>
                Venue Location
              </label>
              <button
                type="button"
                onClick={() => setShowVenueModal(true)}
                style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '12px', fontWeight: 500, cursor: 'pointer' }}
              >
                + Add New Venue
              </button>
            </div>

            {venues.length === 0 ? (
              <div style={{ background: 'var(--bg)', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px dashed var(--panel-border)', textAlign: 'center', fontSize: '13px', color: 'var(--text-dim)' }}>
                No venues created yet. Click "+ Add New Venue" above.
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
                    {v.name} ({v.address}) — {v.totalCapacity} seats
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
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
              <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
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

          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
              Date & Time
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
            style={{ marginTop: '8px' }}
          >
            {eventLoading ? 'Publishing...' : 'Publish Event'}
          </button>
        </form>
      </div>

      {/* Venue Modal */}
      {showVenueModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '20px' }}>
          <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '24px', background: 'var(--panel)' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '14px' }}>
              Create Venue
            </h3>

            <form onSubmit={handleCreateVenue} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
                  Venue Name
                </label>
                <input
                  type="text"
                  required
                  className="input"
                  placeholder="e.g. City Concert Hall"
                  value={venueName}
                  onChange={(e) => setVenueName(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
                  Address
                </label>
                <input
                  type="text"
                  required
                  className="input"
                  placeholder="e.g. 100 Main St, Chicago"
                  value={venueAddress}
                  onChange={(e) => setVenueAddress(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, marginBottom: '4px', color: 'var(--text-muted)' }}>
                  Capacity (Seat Count)
                </label>
                <input
                  type="number"
                  min="10"
                  max="500"
                  required
                  className="input"
                  value={venueCapacity}
                  onChange={(e) => setVenueCapacity(e.target.value)}
                />
              </div>

              <div className="flex items-center justify-end gap-2" style={{ marginTop: '8px' }}>
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
                  {venueLoading ? 'Saving...' : 'Save Venue'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
