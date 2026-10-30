import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { Search, Calendar, MapPin, ArrowRight, Tag, X, Filter } from 'lucide-react';

const CATEGORIES = ['All', 'Concert', 'Sports', 'Theater', 'Conference', 'Comedy'];

export default function EventsPage() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchEvents();
  }, [selectedCategory]);

  const fetchEvents = async (customSearch = null) => {
    setLoading(true);
    try {
      const params = {};
      if (selectedCategory !== 'All') params.category = selectedCategory;
      const query = customSearch !== null ? customSearch : searchQuery;
      if (query.trim()) params.search = query.trim();

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

  const clearSearch = () => {
    setSearchQuery('');
    fetchEvents('');
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="container" style={{ paddingTop: '32px', paddingBottom: '60px' }}>
      {/* Header section */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-main)', marginBottom: '8px' }}>
          Explore Events
        </h1>
        <p style={{ fontSize: '15px', color: 'var(--text-muted)' }}>
          Browse upcoming performances, sports matches, and shows with live interactive seat selection.
        </p>
      </div>

      {/* Search & Category Filter Controls */}
      <div
        style={{
          background: 'var(--panel)',
          border: '1px solid var(--panel-border)',
          borderRadius: 'var(--radius)',
          padding: '20px',
          marginBottom: '32px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Search form */}
          <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '10px' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search
                size={18}
                color="var(--text-dim)"
                style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none' }}
              />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by event title, description, or artist..."
                className="input-field"
                style={{ width: '100%', paddingLeft: '42px', paddingRight: searchQuery ? '36px' : '14px' }}
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={clearSearch}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-dim)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                  title="Clear search"
                >
                  <X size={16} />
                </button>
              )}
            </div>
            <button type="submit" className="btn btn-primary" style={{ padding: '0 20px', whiteSpace: 'nowrap' }}>
              Search
            </button>
          </form>

          {/* Category Chips */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '12.5px', color: 'var(--text-dim)', marginRight: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Filter size={14} /> Filter:
            </span>
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                className={`category-pill ${selectedCategory === cat ? 'active' : ''}`}
                style={{
                  fontSize: '13px',
                  padding: '6px 14px',
                  borderRadius: '20px',
                  cursor: 'pointer',
                  border: selectedCategory === cat ? '1px solid var(--primary)' : '1px solid var(--panel-border)',
                  background: selectedCategory === cat ? 'var(--primary)' : 'var(--bg-subtle)',
                  color: selectedCategory === cat ? '#ffffff' : 'var(--text-muted)',
                  fontWeight: selectedCategory === cat ? 600 : 500,
                  transition: 'all 0.15s ease',
                }}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Events Results Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)' }}>
          {selectedCategory === 'All' ? 'All Upcoming Events' : `${selectedCategory} Events`}
        </h2>
        <span style={{ fontSize: '13px', color: 'var(--text-dim)' }}>
          {loading ? 'Searching events...' : `${events.length} ${events.length === 1 ? 'event' : 'events'} available`}
        </span>
      </div>

      {/* Event Cards Grid */}
      {loading ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              style={{
                background: 'var(--panel)',
                border: '1px solid var(--panel-border)',
                borderRadius: 'var(--radius)',
                padding: '24px',
                minHeight: '220px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                opacity: 0.6,
              }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <div style={{ width: '80px', height: '18px', background: 'var(--bg-subtle)', borderRadius: '4px' }} />
                <div style={{ width: '70%', height: '22px', background: 'var(--bg-subtle)', borderRadius: '4px' }} />
                <div style={{ width: '90%', height: '14px', background: 'var(--bg-subtle)', borderRadius: '4px' }} />
              </div>
              <div style={{ width: '100%', height: '36px', background: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)' }} />
            </div>
          ))}
        </div>
      ) : events.length === 0 ? (
        <div
          style={{
            textAlign: 'center',
            padding: '60px 20px',
            background: 'var(--panel)',
            border: '1px solid var(--panel-border)',
            borderRadius: 'var(--radius)',
          }}
        >
          <div style={{ fontSize: '32px', marginBottom: '12px' }}>🎫</div>
          <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '8px' }}>
            No events found
          </h3>
          <p style={{ fontSize: '14px', color: 'var(--text-muted)', maxWidth: '400px', margin: '0 auto 20px' }}>
            {searchQuery
              ? `We couldn't find any events matching "${searchQuery}". Try a different keyword or category.`
              : `There are currently no events listed under the "${selectedCategory}" category.`}
          </p>
          {(searchQuery || selectedCategory !== 'All') && (
            <button
              onClick={() => {
                setSelectedCategory('All');
                clearSearch();
              }}
              className="btn btn-secondary"
            >
              Reset Filters
            </button>
          )}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
          {events.map((event) => (
            <div
              key={event.id}
              className="event-card"
              style={{
                background: 'var(--panel)',
                border: '1px solid var(--panel-border)',
                borderRadius: 'var(--radius)',
                padding: '24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'border-color 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease',
              }}
            >
              <div>
                {/* Category Pill & Price */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                  <span
                    className="badge badge-muted"
                    style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px', fontWeight: 600 }}
                  >
                    {event.category || 'General'}
                  </span>
                  <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--primary)' }}>
                    From ${Number(event.basePrice || 0).toFixed(2)}
                  </span>
                </div>

                {/* Event Title */}
                <h3
                  style={{
                    fontSize: '18px',
                    fontWeight: 700,
                    color: 'var(--text-main)',
                    lineHeight: '1.35',
                    marginBottom: '10px',
                  }}
                >
                  {event.title}
                </h3>

                {/* Event Description */}
                <p
                  style={{
                    fontSize: '13px',
                    color: 'var(--text-muted)',
                    lineHeight: '1.5',
                    marginBottom: '18px',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden',
                  }}
                >
                  {event.description}
                </p>

                {/* Date & Venue Meta */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '22px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)' }}>
                    <Calendar size={15} color="var(--primary)" />
                    <span>{formatDateTime(event.startTime)}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)' }}>
                    <MapPin size={15} color="var(--primary)" />
                    <span>{event.venueName || event.venue?.name || 'Main Stage'}</span>
                  </div>
                </div>
              </div>

              {/* Action Button */}
              <Link
                to={`/events/${event.id}`}
                className="btn btn-primary"
                style={{ width: '100%', justifyContent: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <span>Select Seats</span>
                <ArrowRight size={14} />
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
