import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api from '../api/client';
import { Search, Calendar, MapPin, ArrowRight, X, Filter, SearchX } from 'lucide-react';

const CATEGORIES = ['All', 'Concert', 'Sports', 'Theater', 'Conference', 'Comedy'];

export default function EventsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialCategory = searchParams.get('category') || 'All';
  const initialQuery = searchParams.get('search') || '';

  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState(initialCategory);
  const [searchQuery, setSearchQuery] = useState(initialQuery);

  useEffect(() => {
    const cat = searchParams.get('category') || 'All';
    const query = searchParams.get('search') || '';
    setSelectedCategory(cat);
    setSearchQuery(query);
    fetchEvents(cat, query);
  }, [searchParams]);

  const fetchEvents = async (category = selectedCategory, query = searchQuery) => {
    setLoading(true);
    try {
      const params = {};
      if (category && category !== 'All') params.category = category;
      if (query && query.trim()) params.search = query.trim();

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
    updateUrlParams(selectedCategory, searchQuery);
  };

  const handleCategorySelect = (cat) => {
    setSelectedCategory(cat);
    updateUrlParams(cat, searchQuery);
  };

  const clearSearch = () => {
    setSearchQuery('');
    updateUrlParams(selectedCategory, '');
  };

  const resetAllFilters = () => {
    setSelectedCategory('All');
    setSearchQuery('');
    setSearchParams({});
  };

  const updateUrlParams = (cat, query) => {
    const nextParams = {};
    if (cat && cat !== 'All') nextParams.category = cat;
    if (query && query.trim()) nextParams.search = query.trim();
    setSearchParams(nextParams);
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

  const hasActiveFilters = searchQuery.trim() !== '' || selectedCategory !== 'All';

  return (
    <div className="container" style={{ paddingTop: '32px', paddingBottom: '60px' }}>
      {/* Header section */}
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-main)', marginBottom: '8px' }}>
          Explore Events
        </h1>
        <p style={{ fontSize: '15px', color: 'var(--text-muted)' }}>
          Browse upcoming performances, sports matches, and shows with live interactive seat selection.
        </p>
      </div>

      {/* Improved Search Bar & Filter Controls */}
      <div
        style={{
          background: 'var(--panel)',
          border: '1px solid var(--panel-border)',
          borderRadius: 'var(--radius)',
          padding: '16px 20px',
          marginBottom: '28px',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Main Search Input Form */}
          <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
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
                placeholder="Search by event title, artist, or venue..."
                className="input"
                style={{
                  width: '100%',
                  paddingLeft: '42px',
                  paddingRight: searchQuery ? '38px' : '14px',
                  fontSize: '14px',
                  height: '42px',
                  borderRadius: 'var(--radius-sm)',
                }}
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
                    padding: '4px',
                  }}
                  title="Clear search"
                  aria-label="Clear search input"
                >
                  <X size={16} />
                </button>
              )}
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{
                height: '42px',
                padding: '0 22px',
                whiteSpace: 'nowrap',
                fontWeight: 600,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <Search size={15} />
              <span>Search</span>
            </button>
          </form>

          {/* Category Chips Bar */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '12.5px', color: 'var(--text-dim)', marginRight: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Filter size={13} /> Genre:
            </span>
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => handleCategorySelect(cat)}
                style={{
                  fontSize: '13px',
                  padding: '5px 14px',
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

            {hasActiveFilters && (
              <button
                type="button"
                onClick={resetAllFilters}
                style={{
                  marginLeft: 'auto',
                  background: 'none',
                  border: 'none',
                  color: 'var(--primary)',
                  fontSize: '12.5px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <X size={13} />
                <span>Reset Filters</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Events Results Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)' }}>
          {selectedCategory === 'All' ? 'All Events' : `${selectedCategory} Events`}
          {searchQuery && <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}> matching &ldquo;{searchQuery}&rdquo;</span>}
        </h2>
        <span style={{ fontSize: '13px', color: 'var(--text-dim)' }}>
          {loading ? 'Loading...' : `${events.length} ${events.length === 1 ? 'event' : 'events'}`}
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
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              background: 'var(--bg-subtle)',
              color: 'var(--text-dim)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
            }}
          >
            <SearchX size={24} />
          </div>
          <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '8px' }}>
            No events found
          </h3>
          <p style={{ fontSize: '14px', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto 20px', lineHeight: '1.5' }}>
            {searchQuery
              ? `We couldn't find any events matching "${searchQuery}". Try searching for another artist or venue.`
              : `There are currently no events listed under "${selectedCategory}". Check back soon or browse all categories.`}
          </p>
          {hasActiveFilters && (
            <button
              onClick={resetAllFilters}
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
