import React from 'react';
import { ShieldCheck, Zap, Database, Cpu } from 'lucide-react';

export default function Footer() {
  return (
    <footer style={{ borderTop: '1px solid var(--panel-border)', background: 'var(--panel)', padding: '48px 0 32px', marginTop: '80px' }}>
      <div className="container">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '32px', marginBottom: '40px' }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: '18px', letterSpacing: '-0.3px', color: 'var(--text-main)', marginBottom: '10px' }}>
              Quick<span style={{ color: 'var(--primary)' }}>Tix</span>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.6', maxWidth: '320px', marginBottom: '16px' }}>
              High-throughput event booking platform featuring optimistic concurrency control, sub-millisecond Redis caching, and real-time seat isolation.
            </p>
            <div className="flex items-center gap-2">
              <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--success)' }}></span>
                System Operational
              </span>
            </div>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.8px', color: 'var(--text-dim)', marginBottom: '14px' }}>
              Platform
            </h4>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13.5px', color: 'var(--text-muted)' }}>
              <li><a href="/#events" className="footer-link">Live Event Catalog</a></li>
              <li><a href="/#architecture" className="footer-link">System Architecture</a></li>
              <li><a href="/#how-it-works" className="footer-link">How It Works</a></li>
              <li><a href="/login" className="footer-link">Demo Login</a></li>
            </ul>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.8px', color: 'var(--text-dim)', marginBottom: '14px' }}>
              Engineering Stack
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12.5px', color: 'var(--text-muted)' }}>
              <div className="flex items-center gap-2">
                <Cpu size={14} color="var(--primary)" />
                <span>Spring Boot 3.3.4 (Java 21 Virtual Threads)</span>
              </div>
              <div className="flex items-center gap-2">
                <Database size={14} color="var(--primary)" />
                <span>PostgreSQL 16 with Hibernate @Version</span>
              </div>
              <div className="flex items-center gap-2">
                <Zap size={14} color="var(--primary)" />
                <span>Redis 7 Distributed Cache Layer</span>
              </div>
              <div className="flex items-center gap-2">
                <ShieldCheck size={14} color="var(--primary)" />
                <span>Dual Token Auth with HttpOnly Rotation</span>
              </div>
            </div>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', fontSize: '12.5px', color: 'var(--text-dim)' }}>
          <span>&copy; {new Date().getFullYear()} QuickTix. Concurrency-Safe Live Event Ticketing.</span>
          <span style={{ fontFamily: 'var(--font-mono)' }}>p99 &lt; 20ms Redis Reads • Zero Overbooking</span>
        </div>
      </div>
    </footer>
  );
}

