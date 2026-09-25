import React from 'react';
import { ShieldCheck, Zap, Lock } from 'lucide-react';

export default function Footer() {
  return (
    <footer style={{ borderTop: '1px solid var(--panel-border)', background: 'var(--panel)', padding: '40px 0 30px', marginTop: '80px' }}>
      <div className="container">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6" style={{ paddingBottom: '24px', borderBottom: '1px solid var(--border)' }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: '16px', color: 'var(--text-main)', marginBottom: '4px' }}>
              QuickTix High-Concurrency Engine
            </div>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>
              Built with Spring Boot, Optimistic Locking (<code style={{ color: 'var(--primary)' }}>@Version</code>), and PostgreSQL.
            </p>
          </div>

          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              <Lock size={14} color="var(--primary)" />
              <span>Zero Double-Booking</span>
            </div>
            <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              <Zap size={14} color="var(--warning)" />
              <span>ACID Transactions</span>
            </div>
            <div className="flex items-center gap-2" style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              <ShieldCheck size={14} color="var(--success)" />
              <span>JWT RBAC Auth</span>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between" style={{ marginTop: '20px', fontSize: '12px', color: 'var(--text-dim)' }}>
          <span>© 2026 QuickTix Platform. All rights reserved.</span>
          <span>Engineered by Sourav Kumar</span>
        </div>
      </div>
    </footer>
  );
}
