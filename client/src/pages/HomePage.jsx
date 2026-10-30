import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import {
  Calendar,
  MapPin,
  ArrowRight,
  ShieldCheck,
  Zap,
  Ticket,
  Users,
  Search,
} from 'lucide-react';

export default function HomePage() {
  const { user, isAuthenticated, isOrganizer, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [featuredEvents, setFeaturedEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [heroSearch, setHeroSearch] = useState('');
  const [heroCategory, setHeroCategory] = useState('All');

  useEffect(() => {
    fetchFeaturedEvents();
  }, []);

  const fetchFeaturedEvents = async () => {
    try {
      const res = await api.get('/events');
      setFeaturedEvents((res.data || []).slice(0, 3));
    } catch (err) {
      console.error('Failed to load featured events:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleHeroSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (heroSearch.trim()) params.set('search', heroSearch.trim());
    if (heroCategory !== 'All') params.set('category', heroCategory);
    const queryString = params.toString();
    navigate(`/events${queryString ? `?${queryString}` : ''}`);
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
    <div className="container" style={{ paddingTop: '24px', paddingBottom: '60px' }}>
      {/* ====================================================================
          Personalized Welcome Bar (Only visible when user is logged in)
          ==================================================================== */}
      {isAuthenticated && (
        <div
          style={{
            background: 'var(--panel)',
            border: '1px solid var(--panel-border)',
            borderRadius: 'var(--radius)',
            padding: '16px 20px',
            marginBottom: '32px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '50%',
                background: 'var(--primary-dim)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '14px',
              }}
            >
              {user?.name?.charAt(0) || 'U'}
            </div>
            <div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-main)' }}>
                Welcome back, {user?.name}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                Signed in as <span style={{ textTransform: 'capitalize' }}>{user?.role?.toLowerCase()}</span> ({user?.email})
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Link to="/events" className="btn btn-secondary btn-sm" style={{ textDecoration: 'none' }}>
              Browse Events
            </Link>
            {isOrganizer && (
              <Link to="/organizer/create" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>
                Create Event
              </Link>
            )}
            {isAdmin && (
              <Link to="/admin" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>
                Admin Panel
              </Link>
            )}
            {!isOrganizer && !isAdmin && (
              <Link to="/my-bookings" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>
                My Bookings
              </Link>
            )}
          </div>
        </div>
      )}

      {/* ====================================================================
          Hero Section (Clean, Professional, No Gimmick Icons)
          ==================================================================== */}
      <section
        style={{
          padding: '40px 0 32px',
          textAlign: 'center',
          maxWidth: '820px',
          margin: '0 auto',
        }}
      >
        <h1
          style={{
            fontSize: 'clamp(32px, 5vw, 48px)',
            fontWeight: 800,
            letterSpacing: '-1px',
            lineHeight: 1.15,
            color: 'var(--text-main)',
            marginBottom: '16px',
          }}
        >
          Book live event tickets with <span style={{ color: 'var(--primary)' }}>real-time seat selection</span>
        </h1>

        <p
          style={{
            fontSize: '16px',
            lineHeight: 1.6,
            color: 'var(--text-muted)',
            marginBottom: '32px',
            maxWidth: '620px',
            marginLeft: 'auto',
            marginRight: 'auto',
          }}
        >
          Choose your exact seats on interactive venue maps. Zero double-booking, live availability updates, and instant digital confirmations.
        </p>

        {/* Hero Interactive Search Bar */}
        <form
          onSubmit={handleHeroSearch}
          style={{
            background: 'var(--panel)',
            border: '1px solid var(--panel-border)',
            borderRadius: 'var(--radius)',
            padding: '8px',
            boxShadow: '0 8px 30px rgba(0, 0, 0, 0.08)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            maxWidth: '640px',
            margin: '0 auto 36px',
            flexWrap: 'wrap',
          }}
        >
          <div style={{ position: 'relative', flex: 1, minWidth: '220px' }}>
            <Search
              size={18}
              color="var(--text-dim)"
              style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none' }}
            />
            <input
              type="text"
              value={heroSearch}
              onChange={(e) => setHeroSearch(e.target.value)}
              placeholder="Search by artist, concert, or venue..."
              className="input"
              style={{
                paddingLeft: '40px',
                border: 'none',
                background: 'transparent',
                fontSize: '14px',
              }}
            />
          </div>

          <select
            value={heroCategory}
            onChange={(e) => setHeroCategory(e.target.value)}
            className="input"
            style={{
              width: 'auto',
              minWidth: '130px',
              border: 'none',
              background: 'var(--bg-subtle)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '13px',
              cursor: 'pointer',
              color: 'var(--text-main)',
            }}
          >
            <option value="All">All Genres</option>
            <option value="Concert">Concerts</option>
            <option value="Sports">Sports</option>
            <option value="Theater">Theater</option>
            <option value="Conference">Conferences</option>
            <option value="Comedy">Comedy</option>
          </select>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ padding: '10px 20px', whiteSpace: 'nowrap', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
          >
            <Search size={15} />
            <span>Find Tickets</span>
          </button>
        </form>

        {/* 3 Core Highlights (No Gimmick Icons) */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '16px',
            textAlign: 'left',
          }}
        >
          <div
            style={{
              background: 'var(--panel)',
              border: '1px solid var(--panel-border)',
              borderRadius: 'var(--radius)',
              padding: '18px 20px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: 'var(--primary)', fontWeight: 600, fontSize: '14px' }}>
              <Zap size={18} />
              <span>Live Seat Sync</span>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.5', margin: 0 }}>
              Seats update dynamically on your screen in real time as tickets are selected and purchased.
            </p>
          </div>

          <div
            style={{
              background: 'var(--panel)',
              border: '1px solid var(--panel-border)',
              borderRadius: 'var(--radius)',
              padding: '18px 20px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: 'var(--primary)', fontWeight: 600, fontSize: '14px' }}>
              <ShieldCheck size={18} />
              <span>Zero Double-Booking</span>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.5', margin: 0 }}>
              Concurrency-safe transactions guarantee that seats cannot be purchased twice during simultaneous rushes.
            </p>
          </div>

          <div
            style={{
              background: 'var(--panel)',
              border: '1px solid var(--panel-border)',
              borderRadius: 'var(--radius)',
              padding: '18px 20px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: 'var(--primary)', fontWeight: 600, fontSize: '14px' }}>
              <Ticket size={18} />
              <span>Instant Confirmation</span>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.5', margin: 0 }}>
              Completed bookings are securely confirmed immediately in your account and sent to your email.
            </p>
          </div>
        </div>
      </section>

      {/* ====================================================================
          Featured Events Preview
          ==================================================================== */}
      <section style={{ marginTop: '48px', marginBottom: '64px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '24px' }}>
          <div>
            <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.3px', marginBottom: '4px' }}>
              Featured Events
            </h2>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
              Upcoming performances with live interactive seating available now
            </p>
          </div>
          <Link
            to="/events"
            style={{
              fontSize: '13.5px',
              fontWeight: 600,
              color: 'var(--primary)',
              textDecoration: 'none',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <span>View all events</span>
            <ArrowRight size={14} />
          </Link>
        </div>

        {loading ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                style={{
                  background: 'var(--panel)',
                  border: '1px solid var(--panel-border)',
                  borderRadius: 'var(--radius)',
                  padding: '24px',
                  minHeight: '200px',
                  opacity: 0.6,
                }}
              />
            ))}
          </div>
        ) : featuredEvents.length === 0 ? (
          <div
            style={{
              background: 'var(--panel)',
              border: '1px solid var(--panel-border)',
              borderRadius: 'var(--radius)',
              padding: '40px 20px',
              textAlign: 'center',
            }}
          >
            <p style={{ color: 'var(--text-muted)', marginBottom: '16px' }}>No upcoming events scheduled at the moment.</p>
            <Link to="/events" className="btn btn-secondary btn-sm" style={{ textDecoration: 'none' }}>
              Explore Event Catalog
            </Link>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
            {featuredEvents.map((event) => (
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
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                    <span className="badge badge-muted" style={{ fontSize: '11px', textTransform: 'uppercase', fontWeight: 600 }}>
                      {event.category || 'Event'}
                    </span>
                    <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--primary)' }}>
                      From ${Number(event.basePrice || 0).toFixed(2)}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '8px' }}>
                    {event.title}
                  </h3>

                  <p
                    style={{
                      fontSize: '13px',
                      color: 'var(--text-muted)',
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

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)' }}>
                      <Calendar size={14} color="var(--primary)" />
                      <span>{formatDateTime(event.startTime)}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)' }}>
                      <MapPin size={14} color="var(--primary)" />
                      <span>{event.venueName || event.venue?.name || 'Main Stage'}</span>
                    </div>
                  </div>
                </div>

                <Link
                  to={`/events/${event.id}`}
                  className="btn btn-primary"
                  style={{ width: '100%', justifyContent: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  <span>Pick Seats & Book</span>
                  <ArrowRight size={14} />
                </Link>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ====================================================================
          How It Works Section
          ==================================================================== */}
      <section
        id="how-it-works"
        style={{
          background: 'var(--panel)',
          border: '1px solid var(--panel-border)',
          borderRadius: 'var(--radius)',
          padding: '40px 32px',
          marginBottom: '64px',
        }}
      >
        <div style={{ textAlign: 'center', maxWidth: '580px', margin: '0 auto 36px' }}>
          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--text-main)', marginBottom: '8px' }}>
            How QuickTix Works
          </h2>
          <p style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
            Transparent, reliable, and instant ticketing from search to reservation.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '24px' }}>
          <div style={{ display: 'flex', gap: '16px' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                background: 'var(--primary-dim)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '14px',
                flexShrink: 0,
              }}
            >
              1
            </div>
            <div>
              <h3 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                Browse & Filter Events
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.5' }}>
                Explore concerts, sports tournaments, comedy, and conferences. Filter by genre or search by performer.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                background: 'var(--primary-dim)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '14px',
                flexShrink: 0,
              }}
            >
              2
            </div>
            <div>
              <h3 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                Pick Your Exact Seat
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.5' }}>
                Open the interactive seat map. Check rows, seat labels, and live pricing. Seat availability updates continuously.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                background: 'var(--primary-dim)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '14px',
                flexShrink: 0,
              }}
            >
              3
            </div>
            <div>
              <h3 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                Instant Booking Guarantee
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.5' }}>
                Confirm your booking in seconds. Your selected seats are locked, safe from race collisions, and saved to your account.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================================
          Organizer Section
          ==================================================================== */}
      <section
        id="organizers"
        style={{
          background: 'var(--panel)',
          border: '1px solid var(--panel-border)',
          borderRadius: 'var(--radius)',
          padding: '40px 32px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '24px',
        }}
      >
        <div style={{ maxWidth: '540px' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              color: 'var(--primary)',
              fontSize: '12px',
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.8px',
              marginBottom: '10px',
            }}
          >
            <Users size={14} />
            <span>For Event Creators</span>
          </div>
          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--text-main)', marginBottom: '10px' }}>
            Host your next event with QuickTix
          </h2>
          <p style={{ fontSize: '14px', color: 'var(--text-muted)', lineHeight: '1.6' }}>
            Register venues, generate seating charts automatically based on capacity, configure ticket pricing tiers, and monitor real-time ticket sales.
          </p>
        </div>

        <div>
          {isOrganizer ? (
            <Link to="/organizer/create" className="btn btn-primary" style={{ padding: '12px 24px', fontSize: '14px', textDecoration: 'none' }}>
              Create an Event Now
            </Link>
          ) : !isAuthenticated ? (
            <Link to="/register" className="btn btn-primary" style={{ padding: '12px 24px', fontSize: '14px', textDecoration: 'none' }}>
              Sign Up as Organizer
            </Link>
          ) : (
            <Link to="/events" className="btn btn-secondary" style={{ padding: '12px 24px', fontSize: '14px', textDecoration: 'none' }}>
              Explore Events
            </Link>
          )}
        </div>
      </section>
    </div>
  );
}
