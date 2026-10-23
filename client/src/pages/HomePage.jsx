import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import {
  Search,
  Calendar,
  MapPin,
  ArrowRight,
  ShieldCheck,
  Zap,
  Lock,
  Layers,
  Sparkles,
  CheckCircle2,
  Database,
  Ticket,
} from 'lucide-react';

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
    <div className="container" style={{ paddingBottom: '40px' }}>
      {/* ====================================================================
          Hero Section
          ==================================================================== */}
      <section className="hero-section" aria-labelledby="hero-heading">
        <div className="hero-glow" aria-hidden="true" />
        <div className="hero-content">
          <div className="hero-badge">
            <span className="hero-pulse-dot" />
            <Sparkles size={14} color="var(--primary)" />
            <span>Zero Double-Bookings Guaranteed • Redis 7 Speed</span>
          </div>

          <h1 id="hero-heading" className="hero-title">
            Ultra-Fast, <span className="gradient-text">Concurrency-Safe</span> Live Event Ticketing
          </h1>

          <p className="hero-subtitle">
            Engineered for high-volume ticket drops. Backed by PostgreSQL optimistic concurrency control,
            sub-20ms Redis distributed caching, and real-time interactive seat reservation.
          </p>

          <div className="hero-actions">
            <a href="#events" className="btn-hero-primary" id="hero-cta-explore">
              <span>Explore Live Events</span>
              <ArrowRight size={16} />
            </a>
            <Link to="/login" className="btn-hero-secondary" id="hero-cta-login">
              <Ticket size={16} />
              <span>Demo Access / Sign In</span>
            </Link>
          </div>

          {/* Stats Counters Strip */}
          <div className="stats-grid" role="region" aria-label="Key Platform Metrics">
            <div className="stat-card">
              <div className="stat-num" style={{ color: 'var(--success)' }}>0</div>
              <div className="stat-title">Double-Bookings</div>
              <div className="stat-desc">Hibernate @Version locking prevents race collisions</div>
            </div>

            <div className="stat-card">
              <div className="stat-num" style={{ color: 'var(--primary)' }}>&lt; 20ms</div>
              <div className="stat-title">Read Latency</div>
              <div className="stat-desc">In-memory Redis 7 cache with automatic @CacheEvict</div>
            </div>

            <div className="stat-card">
              <div className="stat-num">1,000+</div>
              <div className="stat-title">Live Events</div>
              <div className="stat-desc">Stress-tested dataset across multiple categories</div>
            </div>

            <div className="stat-card">
              <div className="stat-num" style={{ color: 'var(--warning)' }}>100%</div>
              <div className="stat-title">ACID Isolation</div>
              <div className="stat-desc">PostgreSQL transactional integrity with atomic claims</div>
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================================
          Architecture & Engineering Section
          ==================================================================== */}
      <section id="architecture" className="landing-section" aria-labelledby="arch-heading">
        <div className="section-head">
          <div className="section-tag">
            <ShieldCheck size={13} />
            <span>High-Concurrency Architecture</span>
          </div>
          <h2 id="arch-heading" className="section-title">
            Engineered to Eliminate Race Conditions
          </h2>
          <p className="section-desc">
            Traditional ticketing platforms collapse under simultaneous booking spikes. QuickTix combines
            versioned database isolation with distributed in-memory caching for zero overselling.
          </p>
        </div>

        <div className="arch-grid">
          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <ShieldCheck size={22} />
              </div>
              <h3 className="arch-title">Optimistic Concurrency Control</h3>
              <p className="arch-text">
                Every seat entity tracks an explicit version column. When multiple concurrent users attempt
                to purchase the same seat, only the first transaction commits while subsequent requests receive
                an instant, clean conflict rejection without database deadlocks.
              </p>
            </div>
            <div className="arch-pill">
              <Database size={12} />
              <span>PostgreSQL • @Version Locking</span>
            </div>
          </div>

          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <Zap size={22} />
              </div>
              <h3 className="arch-title">Sub-Millisecond Redis 7 Caching</h3>
              <p className="arch-text">
                High-throughput catalog queries are served from Redis with a 5-minute safety TTL. Whenever an
                event is created, updated, or removed, Spring Cache executes selective @CacheEvict triggers to
                guarantee data freshness without stale reads.
              </p>
            </div>
            <div className="arch-pill">
              <Zap size={12} />
              <span>Redis 7 • In-Memory Acceleration</span>
            </div>
          </div>

          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <Lock size={22} />
              </div>
              <h3 className="arch-title">Bank-Grade Token Security</h3>
              <p className="arch-text">
                Short-lived JWT access tokens reside exclusively in frontend JavaScript memory. Refresh tokens are
                transmitted via Secure HttpOnly cookies and tracked in PostgreSQL with family reuse detection and
                atomic token rotation statements.
              </p>
            </div>
            <div className="arch-pill">
              <Lock size={12} />
              <span>HttpOnly Cookies • Dual Rotating JWTs</span>
            </div>
          </div>

          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <Layers size={22} />
              </div>
              <h3 className="arch-title">Interactive Seat Selection Engine</h3>
              <p className="arch-text">
                Visual seat grids reflect live seat availability with distinct visual states: Available, Held,
                and Booked. Immediate real-time seat reservation guarantees zero surprises during checkout.
              </p>
            </div>
            <div className="arch-pill">
              <Layers size={12} />
              <span>Dynamic Venue Layouts • Live State</span>
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================================
          How It Works Section
          ==================================================================== */}
      <section id="how-it-works" className="landing-section" aria-labelledby="steps-heading">
        <div className="section-head">
          <div className="section-tag">
            <CheckCircle2 size={13} />
            <span>Frictionless Workflow</span>
          </div>
          <h2 id="steps-heading" className="section-title">
            How QuickTix Works in 3 Steps
          </h2>
          <p className="section-desc">
            From discovering events to securing your seats with transactional certainty.
          </p>
        </div>

        <div className="step-grid">
          <div className="step-card">
            <div className="step-num">01</div>
            <h3 className="step-title">Discover & Filter</h3>
            <p className="step-desc">
              Browse concerts, sports, theater, and conferences with instant keyword search and category filters.
              Catalog reads are cached in Redis for instantaneous page loads.
            </p>
          </div>

          <div className="step-card">
            <div className="step-num">02</div>
            <h3 className="step-title">Pick Your Seats</h3>
            <p className="step-desc">
              Inspect the venue seat layout. Choose your preferred seats with live seat availability indicators and
              instant dynamic price computation.
            </p>
          </div>

          <div className="step-card">
            <div className="step-num">03</div>
            <h3 className="step-title">Concurrency-Safe Booking</h3>
            <p className="step-desc">
              Confirm your reservation. Even if 100 concurrent requests target the same seat at the exact same
              millisecond, optimistic version locking ensures exactly one customer receives the ticket.
            </p>
          </div>
        </div>
      </section>

      {/* ====================================================================
          Live Event Catalog Section
          ==================================================================== */}
      <section id="events" className="landing-section" aria-labelledby="events-heading">
        <div className="section-head">
          <div className="section-tag">
            <Calendar size={13} />
            <span>Live Event Catalog</span>
          </div>
          <h2 id="events-heading" className="section-title">
            Upcoming Events & Live Shows
          </h2>
          <p className="section-desc">
            Explore 1,000+ live events. Click any event to inspect seat availability and reserve your spot.
          </p>
        </div>

        {/* Search & Filter Toolbar */}
        <div className="catalog-search-wrap">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <div style={{ position: 'relative', flex: 1, minWidth: '260px' }}>
                <Search
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--text-dim)',
                  }}
                />
                <input
                  id="event-search-input"
                  type="text"
                  className="input"
                  placeholder="Search events by artist, title, venue, or keyword..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  style={{ paddingLeft: '40px', height: '42px' }}
                />
              </div>
              <button type="submit" className="btn btn-primary" id="event-search-btn" style={{ height: '42px', padding: '0 20px' }}>
                <Search size={15} />
                <span>Search</span>
              </button>
            </form>

            {/* Category Chips */}
            <div className="flex items-center justify-between" style={{ flexWrap: 'wrap', gap: '12px' }}>
              <div className="flex items-center gap-2" style={{ flexWrap: 'wrap' }}>
                {CATEGORIES.map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat)}
                    className="btn btn-sm"
                    style={{
                      background: selectedCategory === cat ? 'var(--primary)' : 'var(--bg-subtle)',
                      color: selectedCategory === cat ? '#ffffff' : 'var(--text-muted)',
                      borderColor: selectedCategory === cat ? 'var(--primary)' : 'var(--panel-border)',
                      fontWeight: selectedCategory === cat ? 600 : 500,
                      padding: '6px 14px',
                    }}
                  >
                    {cat}
                  </button>
                ))}
              </div>

              <span style={{ fontSize: '12.5px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                {events.length} event{events.length === 1 ? '' : 's'} available
              </span>
            </div>
          </div>
        </div>

        {/* Events Grid */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '72px 0', color: 'var(--text-muted)' }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '10px', fontSize: '14px' }}>
              <Zap size={18} color="var(--primary)" />
              <span>Fetching events from Redis cache...</span>
            </div>
          </div>
        ) : events.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '56px 20px' }}>
            <h3 style={{ fontSize: '17px', fontWeight: 700, marginBottom: '6px' }}>No events found</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', maxWidth: '420px', margin: '0 auto' }}>
              Try adjusting your search keyword or switching category filters to view other upcoming events.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {events.map((event) => (
              <div key={event.id} className="event-card">
                <div>
                  <div className="flex items-center justify-between gap-2" style={{ marginBottom: '12px' }}>
                    <span className="badge badge-primary">{event.category}</span>
                    <span style={{ fontSize: '17px', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--text-main)' }}>
                      ${Number(event.basePrice).toFixed(2)}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '16.5px', fontWeight: 700, marginBottom: '8px', lineHeight: '1.3' }}>
                    {event.title}
                  </h3>

                  {event.description && (
                    <p
                      style={{
                        color: 'var(--text-muted)',
                        fontSize: '13px',
                        lineHeight: '1.5',
                        marginBottom: '16px',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                      }}
                    >
                      {event.description}
                    </p>
                  )}

                  <div style={{ fontSize: '12.5px', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <div className="flex items-center gap-2">
                      <MapPin size={14} color="var(--text-dim)" style={{ flexShrink: 0 }} />
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {event.venueName} · {event.venueAddress}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Calendar size={14} color="var(--text-dim)" style={{ flexShrink: 0 }} />
                      <span>{new Date(event.startTime).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid var(--panel-border)' }}>
                  <Link to={`/events/${event.id}`} className="event-action-btn">
                    <span>Select Seats</span>
                    <ArrowRight size={14} />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ====================================================================
          Bottom CTA Banner
          ==================================================================== */}
      <section className="landing-cta" aria-labelledby="cta-heading">
        <div className="landing-cta-glow" aria-hidden="true" />
        <div style={{ position: 'relative', zIndex: 1, maxWidth: '640px', margin: '0 auto' }}>
          <div className="section-tag" style={{ marginBottom: '16px' }}>
            <Sparkles size={13} />
            <span>Instant Demo Access</span>
          </div>
          <h2 id="cta-heading" style={{ fontSize: 'clamp(24px, 3.5vw, 36px)', fontWeight: 800, letterSpacing: '-0.8px', marginBottom: '14px', lineHeight: '1.2' }}>
            Test Concurrency-Safe Booking Today
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--text-muted)', lineHeight: '1.6', marginBottom: '28px' }}>
            Sign in with demo credentials (User, Organizer, or Admin) to test real-time seat reservation,
            or create your own events with automated venue seat layout generation.
          </p>
          <div className="flex items-center justify-center gap-3" style={{ flexWrap: 'wrap' }}>
            <Link to="/login" className="btn-hero-primary">
              <Ticket size={16} />
              <span>Launch Demo Experience</span>
            </Link>
            <a href="#events" className="btn-hero-secondary">
              <span>Browse Catalog</span>
            </a>
          </div>
        </div>
      </section>
    </div>
  );
}
