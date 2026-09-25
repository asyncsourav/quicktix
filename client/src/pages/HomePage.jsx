import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { Search, Calendar, MapPin, Tag, ArrowRight, Sparkles } from 'lucide-react';

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
      {/* Hero Banner */}
      <section style={{ padding: '56px 0 40px', textAlign: 'center' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', background: 'var(--primary-dim)', color: 'var(--primary)', padding: '6px 14px', borderRadius: '999px', fontSize: '13px', fontWeight: 600, marginBottom: '20px', border: '1px solid rgba(59, 130, 246, 0.2)' }}>
          <Sparkles size={14} />
          <span>Real-Time Seat Locking Engine Enabled</span>
        </div>
        <h1 style={{ fontSize: 'clamp(28px, 5vw, 44px)', fontWeight: 800, lineHeight: 1.2, letterSpacing: '-1px', maxWidth: '800px', margin: '0 auto 16px' }}>
          Experience Live Events with Guaranteed Instant Booking
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '16px', maxWidth: '620px', margin: '0 auto 32px' }}>
          Zero overselling. Zero double-booking collisions. High-speed concurrency powered by Spring Data JPA optimistic locking.
        </p>

        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} style={{ maxWidth: '580px', margin: '0 auto', display: 'flex', gap: '10px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={18} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }} />
            <input
              type="text"
              className="input"
              placeholder="Search by event title, artist, or team..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ paddingLeft: '42px' }}
            />
          </div>
          <button type="submit" className="btn btn-primary">
            Search
          </button>
        </form>

        {/* Category Pills */}
        <div className="flex items-center justify-center gap-2" style={{ flexWrap: 'wrap', marginTop: '24px' }}>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className="btn btn-sm"
              style={{
                background: selectedCategory === cat ? 'var(--primary)' : 'var(--panel)',
                color: selectedCategory === cat ? '#fff' : 'var(--text-muted)',
                borderColor: selectedCategory === cat ? 'var(--primary)' : 'var(--panel-border)',
              }}
            >
              {cat}
            </button>
          ))}
        </div>
      </section>

      {/* Events Grid */}
      <section style={{ marginTop: '20px' }}>
        <div className="flex items-center justify-between" style={{ marginBottom: '24px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700 }}>Upcoming Events</h2>
          <span style={{ fontSize: '13px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
            {events.length} {events.length === 1 ? 'event' : 'events'} found
          </span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
            Loading available events...
          </div>
        ) : events.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '50px 20px' }}>
            <Calendar size={40} color="var(--text-dim)" style={{ margin: '0 auto 12px' }} />
            <h3 style={{ fontSize: '17px', fontWeight: 600, marginBottom: '6px' }}>No events found</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', maxWidth: '400px', margin: '0 auto 20px' }}>
              No events match your current search or category filter. Check back soon or create one!
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {events.map((event) => (
              <div key={event.id} className="card flex flex-col justify-between" style={{ height: '100%' }}>
                <div>
                  <div className="flex items-center justify-between gap-2" style={{ marginBottom: '12px' }}>
                    <span className="badge badge-primary">{event.category}</span>
                    <span style={{ fontSize: '18px', fontWeight: 800, color: 'var(--success)' }}>
                      ${Number(event.basePrice).toFixed(2)}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '8px', lineHeight: 1.3 }}>
                    {event.title}
                  </h3>

                  {event.description && (
                    <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginBottom: '16px', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {event.description}
                    </p>
                  )}

                  <div style={{ fontSize: '13px', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '14px' }}>
                    <div className="flex items-center gap-2">
                      <MapPin size={15} color="var(--text-dim)" />
                      <span>{event.venueName} · {event.venueAddress}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Calendar size={15} color="var(--text-dim)" />
                      <span>{new Date(event.startTime).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: '24px', paddingTop: '16px', borderTop: '1px solid var(--panel-border)' }}>
                  <Link to={`/events/${event.id}`} className="btn btn-primary" style={{ width: '100%' }}>
                    <span>Select Seats</span>
                    <ArrowRight size={15} />
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
