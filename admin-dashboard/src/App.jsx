import React, { useState, useEffect, useRef } from 'react';
import { supabase } from './supabase';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Shield, 
  Users, 
  Wifi, 
  Activity, 
  Terminal, 
  Database, 
  Cpu, 
  Zap, 
  Globe, 
  Lock, 
  Unlock,
  Map,
  Layers,
  ChevronRight
} from 'lucide-react';

const App = () => {
  const [stats, setStats] = useState({ totalUsers: 0, activeSos: 0, systemLoad: '0.42' });
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [users, setUsers] = useState([]);
  const [logs, setLogs] = useState(["[SYSTEM] Initializing RapidReach Core...", "[SEC] Establishing Encrypted Tunnel..."]);
  const [privacyMode, setPrivacyMode] = useState(true);
  const [activeView, setActiveView] = useState('DASHBOARD');
  const terminalRef = useRef(null);

  useEffect(() => {
    fetchData();
    const sub = supabase.channel('realtime-core').on('postgres_changes', { event: '*', schema: 'public', table: 'sos_alerts' }, (p) => {
      addLog(`[SIGNAL] New incoming transmission detected: ${p.new?.from_name || 'ANON'}`);
      fetchData();
    }).subscribe();

    const logInterval = setInterval(() => {
      const pingLogs = ["[PING] Node-ALPHA: 12ms", "[PING] Node-BETA: 24ms", "[SYNC] Postgrest heartbeat active"];
      addLog(pingLogs[Math.floor(Math.random() * pingLogs.length)]);
    }, 8000);

    return () => {
      clearInterval(logInterval);
      supabase.removeChannel(sub);
    };
  }, []);

  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [logs]);

  const addLog = (msg) => {
    setLogs(prev => [...prev.slice(-15), `[${new Date().toLocaleTimeString()}] ${msg}`]);
  };

  const fetchData = async () => {
    try {
      const { data: u } = await supabase.from('users').select('*');
      const { data: a } = await supabase.from('sos_alerts').select('*').order('timestamp', { ascending: false });
      setUsers(u || []);
      setRecentAlerts(a || []);
      setStats(prev => ({ ...prev, totalUsers: u?.length || 0, activeSos: a?.length || 0 }));
      addLog("[DB] Metadata sync complete.");
    } catch (err) {
      addLog("[ERR] Database connection timeout.");
    }
  };

  return (
    <div className="dashboard-layout">
      {/* COLUMN 1: SLIM SIDEBAR */}
      <nav className="sidebar-slim">
        <Shield color="var(--emerald-neon)" size={28} />
        <div className={`nav-icon ${activeView === 'DASHBOARD' ? 'active' : ''}`} onClick={() => setActiveView('DASHBOARD')}>
          <Activity size={20} />
        </div>
        <div className={`nav-icon ${activeView === 'FLEET' ? 'active' : ''}`} onClick={() => setActiveView('FLEET')}>
          <Users size={20} />
        </div>
        <div className={`nav-icon ${activeView === 'MAP' ? 'active' : ''}`} onClick={() => setActiveView('MAP')}>
          <Map size={20} />
        </div>
        <div style={{ marginTop: 'auto' }} className="nav-icon" onClick={() => setPrivacyMode(!privacyMode)}>
          {privacyMode ? <Lock size={20} /> : <Unlock size={20} color="var(--amber-neon)" />}
        </div>
      </nav>

      {/* COLUMN 2: MAIN CORE */}
      <main className="main-core">
        <header className="header-meta">
          <div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>SYSTEM MONITOR // {activeView}</h2>
            <p className="system-time">TC: {new Date().toISOString().slice(11, 19)} UTC</p>
          </div>
          <div style={{ display: 'flex', gap: '15px' }}>
             <div className="system-time" style={{ color: 'var(--emerald-neon)' }}>● LIVE FEED</div>
          </div>
        </header>

        <section className="card-stack">
          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="obsidian-card">
            <div className="card-title">Authorized Entities</div>
            <div className="card-data">{stats.totalUsers}<span style={{ fontSize: '1rem', color: 'var(--text-low)' }}>/ ∞</span></div>
          </motion.div>
          
          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="obsidian-card">
            <div className="card-title">Secure Link Load</div>
            <div className="card-data">{stats.systemLoad}<span style={{ fontSize: '1rem', color: 'var(--emerald-neon)' }}>ms</span></div>
          </motion.div>

          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="obsidian-card">
            <div className="card-title">Active SOS Priority</div>
            <div className="card-data" style={{ color: stats.activeSos > 0 ? 'var(--crimson-neon)' : 'var(--text-high)' }}>
               {stats.activeSos}
            </div>
          </motion.div>
        </section>

        <section className={`signal-feed ${recentAlerts.length === 0 ? 'scanner-active' : ''}`}>
           <div style={{ padding: '1rem', borderBottom: '1px solid var(--border-subtle)', background: 'rgba(255,255,255,0.02)', display: 'flex', justifyContent: 'space-between' }}>
             <span style={{ fontSize: '0.7rem', letterSpacing: '2px', color: 'var(--text-mid)' }}>INCOMING SIGNAL BUFFER</span>
             <Terminal size={14} color="var(--text-low)" />
           </div>
           
           {recentAlerts.length === 0 ? (
             <div style={{ height: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-low)', fontSize: '0.8rem' }}>
                SCRUBBING AIRWAVES... NO SIGNALS DETECTED
             </div>
           ) : (
             recentAlerts.map((alert, i) => (
               <motion.div initial={{ opacity: 0, x: -5 }} animate={{ opacity: 1, x: 0 }} key={alert.id} className="signal-row">
                 <span style={{ color: 'var(--crimson-neon)', fontWeight: 700 }}>[ALERT]</span>
                 <span style={{ fontWeight: 600 }}>{alert.from_name}</span>
                 <span style={{ color: 'var(--text-mid)', fontFamily: 'JetBrains Mono' }}>{alert.latitude.toFixed(4)}, {alert.longitude.toFixed(4)}</span>
                 <span style={{ color: 'var(--text-low)' }}>{new Date(alert.timestamp).toLocaleTimeString()}</span>
               </motion.div>
             ))
           )}
        </section>

        <div className="terminal-console" ref={terminalRef}>
          {logs.map((log, i) => <div key={i}>{log}</div>)}
        </div>
      </main>

      {/* COLUMN 3: STATUS PANEL */}
      <aside className="status-panel">
        <div style={{ paddingBottom: '2rem', borderBottom: '1px solid var(--border-strong)' }}>
          <h3 style={{ fontSize: '0.8rem', letterSpacing: '2px', color: 'var(--text-mid)', marginBottom: '1.5rem' }}>INFRASTRUCTURE</h3>
          <div className="node-group">
             <div className="node-item">
               <span>Supabase Direct</span>
               <div className="status-dot" style={{ color: 'var(--emerald-neon)' }} />
             </div>
             <div className="node-item">
               <span>Auth Gateway</span>
               <div className="status-dot" style={{ color: 'var(--emerald-neon)' }} />
             </div>
             <div className="node-item">
               <span>Realtime Engine</span>
               <div className="status-dot" style={{ color: 'var(--emerald-neon)' }} />
             </div>
          </div>
        </div>

        <div>
          <h3 style={{ fontSize: '0.8rem', letterSpacing: '2px', color: 'var(--text-mid)', marginBottom: '1.5rem' }}>SYSTEM METRICS</h3>
          <div className="node-group">
             <div style={{ background: 'var(--bg-sheet)', padding: '1rem', borderRadius: '4px' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-low)', marginBottom: '5px' }}>CPU RESOURCE</div>
                <div style={{ height: '4px', background: '#222', borderRadius: '2px' }}>
                  <motion.div animate={{ width: '45%' }} style={{ height: '100%', background: 'var(--emerald-neon)', borderRadius: '2px' }} />
                </div>
             </div>
             <div style={{ background: 'var(--bg-sheet)', padding: '1rem', borderRadius: '4px' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-low)', marginBottom: '5px' }}>BANDWIDTH</div>
                <div style={{ height: '4px', background: '#222', borderRadius: '2px' }}>
                  <motion.div animate={{ width: '12%' }} style={{ height: '100%', background: 'var(--amber-neon)', borderRadius: '2px' }} />
                </div>
             </div>
          </div>
        </div>

        <div style={{ marginTop: 'auto', padding: '1.5rem', background: 'rgba(0, 255, 157, 0.05)', border: '1px solid rgba(0, 255, 157, 0.1)', borderRadius: '4px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.8rem', color: 'var(--emerald-neon)' }}>
             <Zap size={16} /> END-TO-END SECURE
          </div>
        </div>
      </aside>
    </div>
  );
};

export default App;
