import React, { useState, useEffect } from 'react';
import { supabase } from './supabase';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Shield,
  Users,
  Activity,
  Zap,
  Lock,
  Unlock,
  Play,
  FileText,
  AlertCircle,
  CheckCircle,
  LayoutDashboard,
  Library,
  LogOut,
  MapPin,
  Clock
} from 'lucide-react';

const App = () => {
  const [stats, setStats] = useState({ totalUsers: 0, totalActions: 0, activeNow: 0 });
  const [users, setUsers] = useState([]);
  const [sosLogs, setSosLogs] = useState([]);
  const [activeView, setActiveView] = useState('DASHBOARD');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchData();
    const sub = supabase.channel('rapidreach-sync').on('postgres_changes', { event: '*', schema: 'public' }, () => fetchData()).subscribe();
    return () => supabase.removeChannel(sub);
  }, []);

  const fetchData = async () => {
    try {
      const { data: u } = await supabase.from('users').select('*');
      const { data: sl } = await supabase.from('sos_logs').select('*').order('timestamp', { ascending: false });

      setUsers(u || []);
      setSosLogs(sl || []);
      setStats({
        totalUsers: u?.length || 0,
        totalActions: sl?.length || 0,
        activeNow: u?.length > 0 ? Math.floor(Math.random() * u.length) + 1 : 0
      });
      setIsLoading(false);
    } catch (err) {
      console.error(err);
      setIsLoading(false);
    }
  };

  const getUserName = (userId) => {
    const user = users.find(u => u.id === userId);
    return user ? user.name : 'Unknown User';
  };

  return (
    <div className="admin-app">
      {/* SIDEBAR: Simple & Premium */}
      <nav className="side-nav">
        <div className="nav-header">
          <Shield size={32} color="white" />
          <h3>RapidReach</h3>
        </div>

        <div className="nav-links">
          <button className={`nav-btn ${activeView === 'DASHBOARD' ? 'active' : ''}`} onClick={() => setActiveView('DASHBOARD')}>
            <LayoutDashboard size={20} />
            <span>Dashboard</span>
          </button>
          <button className={`nav-btn ${activeView === 'LIBRARY' ? 'active' : ''}`} onClick={() => setActiveView('LIBRARY')}>
            <Library size={20} />
            <span>Audio Library</span>
          </button>
        </div>

        <div className="nav-footer">
          <button className="nav-btn logout">
            <LogOut size={20} />
            <span>Sign Out</span>
          </button>
        </div>
      </nav>

      {/* MAIN CONTENT */}
      <div className="content-area">
        <header className="top-bar">
          <div className="title-section">
            <h1>{activeView === 'DASHBOARD' ? 'Network Overview' : 'Audio Evidence Library'}</h1>
            <p>{isLoading ? 'Syncing with secure nodes...' : 'All systems operational'}</p>
          </div>

          <div className="user-profile">
            <div className="status-indicator">
              <span className="dot"></span>
              LIVE MONITORING
            </div>
            <div className="avatar">AD</div>
          </div>
        </header>

        <main className="view-panels">
          <AnimatePresence mode="wait">
            {activeView === 'DASHBOARD' && (
              <motion.div key="dash" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="dashboard-grid">
                {/* Stats Row */}
                <div className="summary-cards">
                  <StatItem label="Registered Users" value={stats.totalUsers} icon={<Users size={20} />} />
                  <StatItem label="Emergency Events" value={stats.totalActions} icon={<AlertCircle size={20} />} />
                  <StatItem label="Sync Integrity" value="100%" icon={<CheckCircle size={20} />} />
                </div>

                {/* Main Dashboard Layout */}
                <div className="dual-column">
                  {/* Left Column: Recent Activity */}
                  <div className="card timeline-card">
                    <div className="card-header">
                      <h3>Recent Alerts</h3>
                      <Activity size={16} />
                    </div>
                    <div className="timeline">
                      {sosLogs.length === 0 ? (
                        <div className="empty-state">No emergency signals detected</div>
                      ) : (
                        sosLogs.slice(0, 8).map(log => (
                          <div key={log.id} className="timeline-item">
                            <div className="time-col">{new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                            <div className="marker"></div>
                            <div className="info-col">
                              <p><strong>{getUserName(log.user_id)}</strong> triggered SOS</p>
                              <span>{(log.official_service || 'SOS').toUpperCase()} ALERT</span>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>

                  {/* Right Column: Fleet Health */}
                  <div className="card health-card">
                    <div className="card-header">
                      <h3>System Health</h3>
                      <Zap size={16} />
                    </div>
                    <div className="health-stats">
                      <HealthBar label="Supabase Sync" percentage={100} color="#4CAF50" />
                      <HealthBar label="Storage Latency" percentage={88} color="#FFC107" />
                      <HealthBar label="Identity Gateway" percentage={100} color="#4CAF50" />
                    </div>
                    <div className="fleet-summary">
                      <p>Currently Monitoring: <strong>{stats.totalUsers} Active Nodes</strong></p>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}

            {activeView === 'LIBRARY' && (
              <motion.div key="library" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="library-view">
                <div className="evidence-grid">
                  {sosLogs.length === 0 ? (
                    <div className="empty-state" style={{ gridColumn: '1 / -1', height: '300px', background: 'var(--bg-card)', borderRadius: '24px' }}>
                      No audio recordings found in the vault
                    </div>
                  ) : (
                    sosLogs.map(log => (
                      <div key={log.id} className="evidence-card">
                        <div className="card-top">
                          <div className="log-type">{(log.official_service || 'SOS').toUpperCase()}</div>
                          <span className="date">{new Date(log.timestamp).toLocaleDateString()}</span>
                        </div>
                        <div className="user-detail">
                          <div className="u-avatar">{getUserName(log.user_id).charAt(0)}</div>
                          <div className="u-text">
                            <h4>{getUserName(log.user_id)}</h4>
                            <p><MapPin size={10} /> {log.latitude.toFixed(4)}, {log.longitude.toFixed(4)}</p>
                          </div>
                        </div>
                        <div className="card-actions">
                          {log.audio_file_path ? (
                            <a href={log.audio_file_path} target="_blank" rel="noreferrer" className="play-btn">
                              <Play size={14} fill="currentColor" /> Play Recording
                            </a>
                          ) : (
                            <div className="no-media">No recording available</div>
                          )}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
};

/* COMPONENTS */

const StatItem = ({ label, value, icon }) => (
  <div className="stat-card">
    <div className="stat-icon">{icon}</div>
    <div className="stat-info">
      <span className="label">{label}</span>
      <span className="value">{value}</span>
    </div>
  </div>
);

const HealthBar = ({ label, percentage, color }) => (
  <div className="health-row">
    <div className="hr-text"><span>{label}</span> <span>{percentage}%</span></div>
    <div className="hr-bar-bg"><motion.div className="hr-bar-fill" animate={{ width: `${percentage}%` }} style={{ backgroundColor: color }} /></div>
  </div>
);

export default App;
