# 🛡️ RapidReach Admin Dashboard

A premium, privacy-focused web application to monitor the RapidReach safety ecosystem.

## ✨ Features

- **Real-time Monitoring**: Tracks SOS alerts as they happen.
- **Privacy First**: Built-in masking for PII (Personally Identifiable Information). Admin-only toggle to unmask data.
- **System Health**: Connectivity status with the Supabase infrastructure.
- **Analytics**: Visual representation of alert frequency and user distribution.
- **Stunning UI**: Modern dark glassmorphism design.

## 🚀 Getting Started

1. **Navigate to the directory**:
   ```bash
   cd admin-dashboard
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Run the dashboard**:
   ```bash
   npm run dev
   ```

4. **Access the URL**:
   Open [http://localhost:5173](http://localhost:5173) in your browser.

## 🔐 Privacy & Security

- **Data Masking**: Emails and phone numbers are blurred by default (`Privacy: ON`).
- **Authorization**: Ensure you configure Supabase RLS (Row Level Security) to allow only admins to read sensitive data.
- **Real-time**: Leverages Supabase Realtime for instant updates without page refreshes.
