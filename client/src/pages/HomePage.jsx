import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { Search, Calendar, MapPin } from 'lucide-react';

const CATEGORIES = ['All', 'Concert', 'Sports', 'Theater', 'Conference', 'Comedy'];

export default function HomePage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchEvents();
  }, [selectedCategory]);

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedCategory !== 'All') params.category = selectedCategory;
      if (searchQuery.trim()) params.search = searchQuery.trim();

      const res = await api.get('/events', { params });
      setEvents(res.data || []);
    } catch (err) {
      console.error('Failed to load events:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchEvents();
  };

  return (
    <div className="container" style={{ paddingBottom: '60px' }}>
      {/* Header / Search Section */}
      <section style={{ padding: '40px 0 28px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, marginBottom: '8px', letterSpacing: '-0.5px' }}>
          Discover & Book Live Events
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '15px', marginBottom: '24px' }}>
          Browse upcoming concerts, games, and conferences with instant seat availability.
        </p>

        {/* Search & Category Filter */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '8px', maxWidth: '600px' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }} />
              <input
                type="text"
                className="input"
                placeholder="Search events by title or keyword..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ paddingLeft: '38px' }}
              />
            </div>
            <button type="submit" className="btn btn-primary">
              Search
            </button>
          </form>

          {/* Category Chips */}
          <div className="flex items-center gap-2" style={{ flexWrap: 'wrap' }}>
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className="btn btn-sm"
                style={{
                  background: selectedCategory === cat ? 'var(--primary)' : 'var(--panel)',
                  color: selectedCategory === cat ? '#ffffff' : 'var(--text-muted)',
                  borderColor: selectedCategory === cat ? 'var(--primary)' : 'var(--panel-border)',
                }}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Events Grid */}
      <section style={{ marginTop: '12px' }}>
        <div className="flex items-center justify-between" style={{ marginBottom: '18px' }}>
          <h2 style={{ fontSize: '17px', fontWeight: 700 }}>Upcoming Events</h2>
          <span style={{ fontSize: '13px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
            {events.length} available
          </span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)', fontSize: '14px' }}>
            Loading events...
          </div>
        ) : events.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '4px' }}>No events found</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '13.5px' }}>
              Try changing your search query or category filter.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {events.map((event) => (
              <div key={event.id} className="card flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between gap-2" style={{ marginBottom: '10px' }}>
                    <span className="badge badge-primary">{event.category}</span>
                    <span style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-main)' }}>
                      ${Number(event.basePrice).toFixed(2)}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '6px' }}>
                    {event.title}
                  </h3>

                  {event.description && (
                    <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginBottom: '14px', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {event.description}
                    </p>
                  )}

                  <div style={{ fontSize: '12.5px', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <div className="flex items-center gap-1.5">
                      <MapPin size={14} color="var(--text-dim)" />
                      <span>{event.venueName} · {event.venueAddress}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Calendar size={14} color="var(--text-dim)" />
                      <span>{new Date(event.startTime).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: '18px', paddingTop: '14px', borderTop: '1px solid var(--panel-border)' }}>
                  <Link to={`/events/${event.id}`} className="btn btn-secondary" style={{ width: '100%' }}>
                    Select Seats
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
