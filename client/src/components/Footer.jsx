import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, Zap, Ticket } from 'lucide-react';

export default function Footer() {
  return (
    <footer style={{ borderTop: '1px solid var(--panel-border)', background: 'var(--panel)', padding: '48px 0 32px', marginTop: '80px' }}>
      <div className="container">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '32px', marginBottom: '40px' }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: '18px', letterSpacing: '-0.3px', color: 'var(--text-main)', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <div
                style={{
                  width: '24px',
                  height: '24px',
                  borderRadius: '5px',
                  background: 'var(--primary)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Ticket size={14} />
              </div>
              <span>Quick<span style={{ color: 'var(--primary)' }}>Tix</span></span>
            </div>
            <p style={{ fontSize: '13.5px', color: 'var(--text-muted)', lineHeight: '1.6', maxWidth: '340px', marginBottom: '14px' }}>
              Live event ticketing platform with real-time seat reservation, concurrency-safe transactions, and instant digital ticket confirmation.
            </p>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.8px', color: 'var(--text-dim)', marginBottom: '14px' }}>
              Platform
            </h4>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px', color: 'var(--text-muted)' }}>
              <li><Link to="/events" className="footer-link">Explore Events</Link></li>
              <li><a href="/#how-it-works" className="footer-link">How It Works</a></li>
              <li><a href="/#organizers" className="footer-link">Host an Event</a></li>
              <li><Link to="/my-bookings" className="footer-link">My Bookings</Link></li>
            </ul>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.8px', color: 'var(--text-dim)', marginBottom: '14px' }}>
              Platform Guarantees
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px', color: 'var(--text-muted)' }}>
              <div className="flex items-center gap-2">
                <ShieldCheck size={16} color="var(--primary)" />
                <span>Zero Double-Booking Guarantee</span>
              </div>
              <div className="flex items-center gap-2">
                <Zap size={16} color="var(--primary)" />
                <span>Live Seat Status via WebSockets</span>
              </div>
              <div className="flex items-center gap-2">
                <Ticket size={16} color="var(--primary)" />
                <span>Instant Digital Ticket Delivery</span>
              </div>
            </div>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', fontSize: '12.5px', color: 'var(--text-dim)' }}>
          <span>&copy; {new Date().getFullYear()} QuickTix. All rights reserved.</span>
          <span>Fast, reliable, real-time live ticketing.</span>
        </div>
      </div>
    </footer>
  );
}
