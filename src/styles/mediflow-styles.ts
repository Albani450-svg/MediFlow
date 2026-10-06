import { css } from 'lit';

export const mediflowStyles = css`
  :host {
    --primary: #059669;
    --primary-dark: #047857;
    --primary-light: #10b981;
    --primary-soft: #ecfdf5;
    --primary-border: #a7f3d0;
    --bg: #f8fafc;
    --card: #ffffff;
    --text-main: #0f172a;
    --text-muted: #64748b;
    --warning: #f59e0b;
    --warning-soft: #fef3c7;
    --danger: #ef4444;
    --danger-soft: #fef2f2;
    display: block;
    min-height: 100%;
    color: var(--text-main);
    font-family: 'Plus Jakarta Sans', sans-serif;
  }

  * { box-sizing: border-box; }

  button, input, select, textarea { font-family: inherit; }

  .login-screen {
    min-height: 100vh;
    background: linear-gradient(160deg, #059669 0%, #10b981 42%, #f8fafc 42%);
    display: flex;
    justify-content: center;
    padding: 32px 18px 24px;
  }

  .login-panel { width: min(440px, 100%); }

  .login-brand { text-align: center; color: #ffffff; margin-bottom: 20px; }
  .login-logo {
    width: 56px; height: 56px; margin: 0 auto 10px;
    background: rgba(255, 255, 255, 0.22);
    border: 2px solid rgba(255, 255, 255, 0.4);
    border-radius: 18px;
    display: flex; align-items: center; justify-content: center;
    font-size: 28px; font-weight: 800;
  }
  .login-brand h1 { font-size: 22px; font-weight: 800; margin: 0; }
  .login-brand p { font-size: 12px; opacity: 0.92; margin: 3px 0 0; }

  .login-card, .card {
    background: #ffffff;
    border-radius: 16px;
    padding: 14px;
    border: 1px solid #f1f5f9;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
  }
  .login-card { border-radius: 24px; padding: 20px 16px; box-shadow: 0 14px 34px rgba(15, 23, 42, 0.09); }
  .login-card h2 { font-size: 16px; font-weight: 800; text-align: center; margin: 0 0 4px; }
  .sub { font-size: 11px; color: var(--text-muted); text-align: center; margin: 0 0 16px; }

  .role-option {
    display: flex; align-items: center; gap: 12px;
    padding: 12px; border-radius: 16px; border: 2px solid #e2e8f0;
    margin-bottom: 10px; cursor: pointer; background: #fff; width: 100%; text-align: left;
  }
  .role-option.selected { border-color: var(--primary); background: var(--primary-soft); }
  .role-icon {
    width: 44px; height: 44px; border-radius: 13px; background: #f1f5f9;
    display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0;
  }
  .role-option.selected .role-icon { background: var(--primary); color: #fff; }
  .role-info h3 { font-size: 13px; font-weight: 800; margin: 0; }
  .role-info p { font-size: 11px; color: var(--text-muted); margin: 2px 0 0; line-height: 1.35; }

  .form-group { margin-bottom: 12px; }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .form-row-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
  .form-label {
    display: block; font-size: 11px; font-weight: 700; color: var(--text-muted);
    margin-bottom: 5px; text-transform: uppercase; letter-spacing: 0.3px;
  }
  .input-field, .select-field, .textarea-field {
    width: 100%; padding: 11px 12px; border-radius: 12px; border: 1.5px solid #cbd5e1;
    font-size: 12px; font-weight: 600; color: var(--text-main); background: #fff; outline: none;
  }
  .textarea-field { resize: vertical; min-height: 68px; line-height: 1.4; }
  .input-field:focus, .select-field:focus, .textarea-field:focus { border-color: var(--primary); background: #fcfffe; }

  .app {
    width: min(480px, 100%);
    margin: 0 auto;
    min-height: 100vh;
    background: var(--bg);
    padding-bottom: 96px;
    box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.04);
  }

  .app-header {
    background: linear-gradient(135deg, #059669 0%, #10b981 100%);
    color: #fff; padding: 16px 16px 22px;
    border-bottom-left-radius: 24px; border-bottom-right-radius: 24px;
    box-shadow: 0 8px 24px rgba(5, 150, 105, 0.18);
    position: sticky; top: 0; z-index: 50;
  }
  .header-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; gap: 8px; }
  .brand { display: flex; align-items: center; gap: 10px; }
  .brand-icon {
    width: 34px; height: 34px; background: rgba(255,255,255,.22); border-radius: 10px;
    display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 18px;
  }
  .brand-text h1 { font-size: 15px; font-weight: 800; margin: 0; }
  .brand-text p { font-size: 11px; opacity: 0.92; margin: 0; }
  .logout-btn {
    background: rgba(255,255,255,.2); border: 1px solid rgba(255,255,255,.35); color: #fff;
    padding: 6px 11px; border-radius: 10px; font-size: 11px; font-weight: 700; cursor: pointer;
  }
  .queue-banner {
    background: #fff; color: var(--text-main); border-radius: 16px; padding: 12px 14px;
    display: flex; justify-content: space-between; align-items: center; gap: 8px;
    box-shadow: 0 8px 20px rgba(0,0,0,.07);
  }
  .queue-left span { font-size: 10px; font-weight: 800; color: var(--primary); text-transform: uppercase; }
  .queue-left h2 { font-size: 24px; font-weight: 800; margin: 1px 0; }
  .queue-left p, .queue-left small { font-size: 11px; color: var(--text-muted); font-weight: 600; }
  .queue-badge {
    background: var(--primary-soft); color: var(--primary); border: 1px solid var(--primary-border);
    padding: 7px 11px; border-radius: 12px; text-align: center; min-width: 62px;
  }
  .queue-badge strong { display: block; font-size: 14px; font-weight: 800; }
  .queue-badge small { font-size: 10px; font-weight: 600; }

  .container { padding: 14px 16px; }
  .role-banner {
    background: var(--primary-soft); border: 1px solid var(--primary-border); color: var(--primary-dark);
    padding: 9px 12px; border-radius: 12px; font-size: 11px; font-weight: 700; margin-bottom: 14px;
    display: flex; justify-content: space-between; align-items: center; gap: 8px;
  }
  .section-title {
    font-size: 13px; font-weight: 800; margin: 12px 0 8px;
    display: flex; justify-content: space-between; align-items: center; gap: 8px;
  }
  .section-title span { font-size: 11px; color: var(--primary); font-weight: 700; }
  .card { margin-bottom: 12px; }
  .info-grid {
    display: grid; grid-template-columns: 1fr 1fr; gap: 8px; background: #f8fafc;
    padding: 10px; border-radius: 12px; border: 1px solid #e2e8f0; margin-bottom: 10px;
  }
  .info-item small { display: block; font-size: 10px; color: var(--text-muted); font-weight: 600; }
  .info-item strong { font-size: 12px; font-weight: 700; color: var(--text-main); }
  .allergy-alert {
    background: var(--danger-soft); border: 1px solid #fecaca; color: #b91c1c;
    padding: 8px 10px; border-radius: 10px; font-size: 11px; font-weight: 700; margin-bottom: 10px;
  }
  .pill, .pill-warn, .pill-danger {
    padding: 4px 9px; border-radius: 20px; font-size: 10px; font-weight: 700; white-space: nowrap;
  }
  .pill { background: var(--primary-soft); color: var(--primary); }
  .pill-warn { background: var(--warning-soft); color: #b45309; }
  .pill-danger { background: var(--danger-soft); color: #b91c1c; }

  .step-item { display: flex; gap: 11px; padding-bottom: 13px; position: relative; align-items: flex-start; }
  .step-item:not(:last-child)::after {
    content: ''; position: absolute; left: 12px; top: 26px; bottom: 0; width: 2px; background: #e2e8f0;
  }
  .step-item.done:not(:last-child)::after { background: var(--primary-light); }
  .step-dot {
    width: 26px; height: 26px; border-radius: 50%; background: #f1f5f9; color: var(--text-muted);
    display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 800; z-index: 2; flex-shrink: 0;
  }
  .step-item.done .step-dot { background: var(--primary); color: #fff; }
  .step-item.current .step-dot { background: var(--primary-soft); color: var(--primary); border: 2px solid var(--primary); }
  .step-body h4 { font-size: 12px; font-weight: 700; margin: 0; }
  .step-body p { font-size: 11px; color: var(--text-muted); margin: 2px 0 0; }
  .owner-tag {
    display: inline-block; font-size: 9px; font-weight: 700; padding: 2px 6px; border-radius: 5px;
    margin-top: 4px; background: #f1f5f9; color: var(--text-muted);
  }

  .btn-primary, .btn-outline, .btn-small { cursor: pointer; font-weight: 700; }
  .btn-primary {
    width: 100%; background: var(--primary); color: #fff; border: none; padding: 12px; border-radius: 12px;
    font-size: 12px; box-shadow: 0 5px 14px rgba(5, 150, 105, 0.2);
  }
  .btn-primary:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; }
  .btn-outline {
    width: 100%; background: #fff; color: var(--primary); border: 1.5px solid var(--primary);
    padding: 10px; border-radius: 12px; font-size: 11px; margin-top: 8px;
  }
  .btn-outline.danger { color: var(--danger); border-color: #fecaca; }
  .btn-small {
    background: var(--primary); color: #fff; border: none; padding: 5px 9px; border-radius: 8px; font-size: 10px;
  }
  .btn-small.danger { background: var(--danger); }
  .btn-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .btn-row .btn-outline, .btn-row .btn-primary { margin-top: 0; }

  .med-item {
    display: flex; justify-content: space-between; align-items: center; gap: 8px;
    padding: 9px 0; border-bottom: 1px solid #f1f5f9;
  }
  .med-item:last-child { border-bottom: none; }
  .med-item h4 { font-size: 12px; font-weight: 700; margin: 0; }
  .med-item p { font-size: 11px; color: var(--text-muted); margin: 2px 0 0; }
  .med-side { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

  .bottom-nav {
    position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
    width: min(480px, 100%); background: #fff; display: flex; justify-content: space-around;
    padding: 9px 8px calc(12px + env(safe-area-inset-bottom));
    border-top: 1px solid #f1f5f9; box-shadow: 0 -4px 18px rgba(15, 23, 42, 0.05); z-index: 100;
  }
  .nav-item {
    display: flex; flex-direction: column; align-items: center; gap: 3px; color: var(--text-muted);
    font-size: 10px; font-weight: 700; cursor: pointer; padding: 4px 14px; border-radius: 10px;
    background: transparent; border: none;
  }
  .nav-item.active { color: var(--primary); background: var(--primary-soft); }

  .notice {
    background: #ecfdf5; color: var(--primary-dark); border: 1px solid var(--primary-border);
    border-radius: 12px; padding: 8px 10px; font-size: 11px; font-weight: 700; margin-bottom: 10px;
  }
  .notice.error { background: var(--danger-soft); color: #b91c1c; border-color: #fecaca; }
  .hint { font-size: 11px; color: var(--text-muted); margin: 8px 0 0; line-height: 1.4; }
  .qr-box { text-align: center; }
  .qr-box img { width: 180px; height: 180px; }
  .mono { font-family: ui-monospace, monospace; letter-spacing: 0.4px; }
  .stack { display: flex; flex-direction: column; gap: 8px; }
  hr.line { border: none; border-top: 1px solid #f1f5f9; margin: 12px 0; }
`;
