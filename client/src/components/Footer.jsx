import React from 'react';

export default function Footer() {
  return (
    <footer style={{ borderTop: '1px solid var(--panel-border)', background: 'var(--panel)', padding: '24px 0', marginTop: '60px' }}>
      <div className="container flex items-center justify-between" style={{ fontSize: '13px', color: 'var(--text-dim)' }}>
        <span>QuickTix Event Ticketing Platform</span>
        <span>Spring Boot & React</span>
      </div>
    </footer>
  );
}
