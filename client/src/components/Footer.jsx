import React from 'react';
import { ShieldCheck, Zap, Database, Cpu } from 'lucide-react';

export default function Footer() {
  return (
    <footer style={{ borderTop: '1px solid var(--panel-border)', background: 'var(--panel)', padding: '48px 0 32px', marginTop: '80px' }}>
      <div className="container">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '32px', marginBottom: '40px' }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: '17px', letterSpacing: '-0.3px', color: 'var(--text-main)', marginBottom: '10px' }}>
              Quick<span style={{ color: 'var(--primary)' }}>Tix</span>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: '1.6', maxWidth: '320px', marginBottom: '14px' }}>
              Distributed event ticketing and reservation platform demonstrating concurrency control, in-memory caching, and real-time seat synchronization.
            </p>
            <div style={{ fontSize: '12px', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
              Java 21 • Spring Boot 3.3.4 • PostgreSQL 16 • Redis 7
            </div>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.8px', color: 'var(--text-dim)', marginBottom: '14px' }}>
              Navigation
            </h4>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px', color: 'var(--text-muted)' }}>
              <li><a href="/#events" className="footer-link">Event Catalog</a></li>
              <li><a href="/#architecture" className="footer-link">Technical Architecture</a></li>
              <li><a href="/#concurrency-flow" className="footer-link">Concurrency Execution Flow</a></li>
              <li><a href="/login" className="footer-link">Account Sign In</a></li>
            </ul>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.8px', color: 'var(--text-dim)', marginBottom: '14px' }}>
              Technical Specifications
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12.5px', color: 'var(--text-muted)' }}>
              <div className="flex items-center gap-2">
                <Cpu size={14} color="var(--primary)" />
                <span>Spring Boot 3.3.4 (Virtual Threads & REST API)</span>
              </div>
              <div className="flex items-center gap-2">
                <Database size={14} color="var(--primary)" />
                <span>PostgreSQL 16 with Hibernate @Version Locking</span>
              </div>
              <div className="flex items-center gap-2">
                <Zap size={14} color="var(--primary)" />
                <span>Redis 7 Distributed Cache-Aside Layer</span>
              </div>
              <div className="flex items-center gap-2">
                <ShieldCheck size={14} color="var(--primary)" />
                <span>Dual Token Auth with Rotating HttpOnly Refresh</span>
              </div>
            </div>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', fontSize: '12.5px', color: 'var(--text-dim)' }}>
          <span>&copy; {new Date().getFullYear()} QuickTix. Distributed Systems & Concurrency Demonstration.</span>
          <span style={{ fontFamily: 'var(--font-mono)' }}>ACID Compliant • Zero Race Conditions</span>
        </div>
      </div>
    </footer>
  );
}
