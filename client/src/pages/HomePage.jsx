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
  CheckCircle2,
  Database,
  Cpu,
  Radio,
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
        <div className="hero-content">
          <div className="hero-badge">
            <span className="hero-pulse-dot" />
            <span>High Concurrency • Transactional Integrity • Real-Time State</span>
          </div>

          <h1 id="hero-heading" className="hero-title">
            Distributed Event Ticketing & <span className="gradient-text">Reservation System</span>
          </h1>

          <p className="hero-subtitle">
            Engineered to eliminate race conditions and overselling during high-demand booking surges.
            Powered by Spring Boot, PostgreSQL optimistic concurrency control, Redis distributed cache-aside,
            and STOMP WebSocket seat synchronization.
          </p>

          <div className="hero-actions">
            <a href="#events" className="btn-hero-primary" id="hero-cta-explore">
              <span>Browse Events</span>
              <ArrowRight size={15} />
            </a>
            <a href="#architecture" className="btn-hero-secondary" id="hero-cta-architecture">
              <span>System Architecture</span>
            </a>
          </div>

          {/* System Technical Specifications Grid */}
          <div className="stats-grid" role="region" aria-label="System Architectural Highlights">
            <div className="stat-card">
              <div className="flex items-center gap-2" style={{ marginBottom: '8px' }}>
                <ShieldCheck size={18} color="var(--primary)" />
                <span className="stat-title">Optimistic Locking</span>
              </div>
              <div className="stat-desc">Hibernate @Version column ensures zero race collisions without database deadlocks.</div>
            </div>

            <div className="stat-card">
              <div className="flex items-center gap-2" style={{ marginBottom: '8px' }}>
                <Zap size={18} color="var(--primary)" />
                <span className="stat-title">Redis Cache-Aside</span>
              </div>
              <div className="stat-desc">In-memory caching for event catalog reads with automated @CacheEvict triggers.</div>
            </div>

            <div className="stat-card">
              <div className="flex items-center gap-2" style={{ marginBottom: '8px' }}>
                <Radio size={18} color="var(--primary)" />
                <span className="stat-title">Live WebSocket Sync</span>
              </div>
              <div className="stat-desc">STOMP broker broadcasts seat transitions (AVAILABLE, HELD, BOOKED) in real time.</div>
            </div>

            <div className="stat-card">
              <div className="flex items-center gap-2" style={{ marginBottom: '8px' }}>
                <Lock size={18} color="var(--primary)" />
                <span className="stat-title">Rotating JWT Auth</span>
              </div>
              <div className="stat-desc">Stateless access tokens paired with rotating HttpOnly refresh tokens in PostgreSQL.</div>
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
            <Layers size={13} />
            <span>Core Architecture</span>
          </div>
          <h2 id="arch-heading" className="section-title">
            Engineered for Concurrency & Consistency
          </h2>
          <p className="section-desc">
            High-volume ticket sales create severe read and write contention. QuickTix addresses this with
            explicit database versioning, in-memory catalog caching, and pub/sub seat state broadcasting.
          </p>
        </div>

        <div className="arch-grid">
          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <ShieldCheck size={20} />
              </div>
              <h3 className="arch-title">Optimistic Concurrency Control</h3>
              <p className="arch-text">
                Every Seat entity maintains an explicit version number. During checkout, Hibernate validates that
                the seat's version has not changed. If concurrent requests target the same seat, exactly one transaction
                commits while conflicting requests fail gracefully with an ObjectOptimisticLockingFailureException.
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
                <Zap size={20} />
              </div>
              <h3 className="arch-title">Distributed Cache-Aside Strategy</h3>
              <p className="arch-text">
                Event catalog queries are served directly from an in-memory Redis cluster with a 5-minute safety TTL.
                Whenever an organizer creates, updates, or deletes an event, Spring executes selective @CacheEvict
                rules to guarantee data freshness across replicas without serving stale data.
              </p>
            </div>
            <div className="arch-pill">
              <Zap size={12} />
              <span>Redis 7 • Spring Cache</span>
            </div>
          </div>

          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <Lock size={20} />
              </div>
              <h3 className="arch-title">Secure Dual-Token Authentication</h3>
              <p className="arch-text">
                Short-lived JWT access tokens reside strictly in frontend JavaScript memory to protect against XSS.
                Refresh tokens are delivered via secure HttpOnly cookies and tracked in PostgreSQL with session
                family identification and automatic invalidation upon reuse detection.
              </p>
            </div>
            <div className="arch-pill">
              <Lock size={12} />
              <span>HttpOnly Cookies • Family Rotation</span>
            </div>
          </div>

          <div className="arch-card">
            <div>
              <div className="arch-icon-wrap">
                <Radio size={20} />
              </div>
              <h3 className="arch-title">STOMP WebSocket Seat Broadcasting</h3>
              <p className="arch-text">
                When a seat changes status during booking or cancellation, the service publishes a lightweight payload
                to /topic/event.{'{id}'}. Connected clients update their local seat map immediately, eliminating
                unnecessary polling and preventing stale selections.
              </p>
            </div>
            <div className="arch-pill">
              <Cpu size={12} />
              <span>Spring WebSocket • STOMP Protocol</span>
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================================
          Concurrency Flow Section
          ==================================================================== */}
      <section id="concurrency-flow" className="landing-section" aria-labelledby="flow-heading">
        <div className="section-head">
          <div className="section-tag">
            <CheckCircle2 size={13} />
            <span>Transaction Lifecycle</span>
          </div>
          <h2 id="flow-heading" className="section-title">
            Booking Concurrency Execution Flow
          </h2>
          <p className="section-desc">
            How a seat reservation progresses through database transaction boundaries and conflict detection.
          </p>
        </div>

        <div className="step-grid">
          <div className="step-card">
            <div className="step-num">01</div>
            <h3 className="step-title">Seat Selection & Payload Submission</h3>
            <p className="step-desc">
              The attendee selects available seats on the interactive venue grid. The client dispatches a validated
              payload containing event and seat identifiers to the booking service endpoint.
            </p>
          </div>

          <div className="step-card">
            <div className="step-num">02</div>
            <h3 className="step-title">Transactional Boundary & Version Check</h3>
            <p className="step-desc">
              Within a @Transactional boundary, the backend validates seat availability and applies status changes.
              PostgreSQL executes atomic conditional updates matching the current version column.
            </p>
          </div>

          <div className="step-card">
            <div className="step-num">03</div>
            <h3 className="step-title">Commit, Broadcast & Async Dispatch</h3>
            <p className="step-desc">
              On commit, version numbers increment atomically. The STOMP broker immediately broadcasts updated seat
              statuses to all active clients, and an asynchronous confirmation is dispatched.
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
            <span>Event Catalog</span>
          </div>
          <h2 id="events-heading" className="section-title">
            Available Events
          </h2>
          <p className="section-desc">
            Select an event to inspect venue layouts and reserve seats with live availability synchronization.
          </p>
        </div>

        {/* Search & Filter Toolbar */}
        <div className="catalog-search-wrap">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <div style={{ position: 'relative', flex: 1, minWidth: '260px' }}>
                <Search
                  size={15}
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
                  style={{ paddingLeft: '38px', height: '40px' }}
                />
              </div>
              <button type="submit" className="btn btn-primary" id="event-search-btn" style={{ height: '40px', padding: '0 18px' }}>
                <Search size={14} />
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
                      padding: '5px 12px',
                    }}
                  >
                    {cat}
                  </button>
                ))}
              </div>

              <span style={{ fontSize: '12px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                {events.length} event{events.length === 1 ? '' : 's'} loaded
              </span>
            </div>
          </div>
        </div>

        {/* Events Grid */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', fontSize: '13.5px' }}>
              <span className="hero-pulse-dot" />
              <span>Loading event catalog...</span>
            </div>
          </div>
        ) : events.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '6px' }}>No events found</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '13.5px', maxWidth: '420px', margin: '0 auto' }}>
              Try adjusting your search keyword or switching category filters to view other events.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {events.map((event) => (
              <div key={event.id} className="event-card">
                <div>
                  <div className="flex items-center justify-between gap-2" style={{ marginBottom: '12px' }}>
                    <span className="badge badge-primary">{event.category}</span>
                    <span style={{ fontSize: '16px', fontWeight: 700, fontFamily: 'var(--font-mono)', color: 'var(--text-main)' }}>
                      ${Number(event.basePrice).toFixed(2)}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px', lineHeight: '1.3' }}>
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
          Interview & Demo Reference Card
          ==================================================================== */}
      <section className="landing-cta" aria-labelledby="cta-heading">
        <div style={{ position: 'relative', zIndex: 1, maxWidth: '680px', margin: '0 auto' }}>
          <div className="section-tag" style={{ marginBottom: '14px' }}>
            <ShieldCheck size={13} />
            <span>Demonstration & Evaluation Reference</span>
          </div>
          <h2 id="cta-heading" style={{ fontSize: 'clamp(20px, 3vw, 28px)', fontWeight: 700, letterSpacing: '-0.5px', marginBottom: '12px', lineHeight: '1.25' }}>
            Pre-Configured Test Accounts
          </h2>
          <p style={{ fontSize: '14px', color: 'var(--text-muted)', lineHeight: '1.6', marginBottom: '24px' }}>
            The database is initialized with three distinct roles to test and demonstrate end-to-end functionality,
            optimistic lock conflict resolution, and venue management:
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', marginBottom: '24px', textAlign: 'left' }}>
            <div style={{ background: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: 'var(--radius-sm)', padding: '14px' }}>
              <div style={{ fontWeight: 600, fontSize: '13px', color: 'var(--text-main)', marginBottom: '4px' }}>Attendee</div>
              <div style={{ fontSize: '11.5px', fontFamily: 'var(--font-mono)', color: 'var(--primary)', marginBottom: '6px' }}>user@quicktix.com</div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Browse events, book seats, test concurrency collisions, and view personal bookings.</div>
            </div>

            <div style={{ background: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: 'var(--radius-sm)', padding: '14px' }}>
              <div style={{ fontWeight: 600, fontSize: '13px', color: 'var(--text-main)', marginBottom: '4px' }}>Organizer</div>
              <div style={{ fontSize: '11.5px', fontFamily: 'var(--font-mono)', color: 'var(--primary)', marginBottom: '6px' }}>organizer@quicktix.com</div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Create venues, publish events, and auto-generate configurable seat layouts.</div>
            </div>

            <div style={{ background: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: 'var(--radius-sm)', padding: '14px' }}>
              <div style={{ fontWeight: 600, fontSize: '13px', color: 'var(--text-main)', marginBottom: '4px' }}>Administrator</div>
              <div style={{ fontSize: '11.5px', fontFamily: 'var(--font-mono)', color: 'var(--primary)', marginBottom: '6px' }}>admin@quicktix.com</div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Access platform audit metrics, gross revenue stats, user registry, and all bookings.</div>
            </div>
          </div>

          <div className="flex items-center justify-center gap-3" style={{ flexWrap: 'wrap' }}>
            <Link to="/login" className="btn-hero-primary" style={{ padding: '10px 20px', fontSize: '13.5px' }}>
              <span>Sign In with Demo Account</span>
              <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
