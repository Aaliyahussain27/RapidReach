# 🛡️ RapidReach: Safety at Your Fingertips

RapidReach is a specialized personal safety application for Android, designed to provide immediate assistance and peace of mind. Built with **Kotlin** and **Jetpack Compose**, it offers a robust set of features to handle emergency situations, track location in real-time, and provide quick access to essential services.

---

## 🚀 Key Features

- **🚨 SOS Emergency System**
  - **Manual Trigger:** One-tap activation to send alerts to emergency contacts.
  - **Live Tracking:** Real-time location sharing with trusted contacts during an active SOS.
  - **Recording:** Automatically captures audio snippets for evidence during emergencies.
  - **Biometric Protection:** Securely stop SOS alerts using your fingerprint or face unlock.

- **📍 Nearby Helpline (Overpass API)**
  - Instantly locate the nearest **Hospitals, Police Stations, and Fire Brigades**.
  - Powered by **OpenStreetMap** for cross-platform reliability without expensive API costs.

- **📞 Fake Call Simulation**
  - Exit uncomfortable social situations by scheduling a realistic-looking incoming call.
  - Customizable caller names and wait times.

- **🗺️ Live Share & Maps**
  - Integrated **OpenStreetMap (OSMDroid)** for live location visualization.
  - Efficient background tracking with optimized battery usage.

- **🔐 Secure Authentication**
  - Powered by **Supabase Auth** for high-performance and secure user identity management.

- **🔄 Data Sync & Offline Reliability**
  - **Room Database:** Local caching of SOS logs and user profile.
  - **WorkManager:** Guaranteed synchronization of emergency data even after network loss.

---

## 🛠️ Tech Stack

- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Kotlin)
- **Backend-as-a-Service:** [Supabase](https://supabase.com/) (Auth, Postgrest, Realtime, Storage)
- **Maps & Location:** [OSMDroid](https://github.com/osmdroid/osmdroid) & Overpass API
- **Local Persistence:** [Room](https://developer.android.com/training/data-storage/room)
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Networking:** Ktor Client & Kotlinx Serialization
- **Security:** Biometric Prompt API & Jetpack Security Crypto

---

## 🏗️ Getting Started

### Prerequisites

- Android Studio Koala+
- JDK 11 or higher
- A [Supabase](https://supabase.com/) project

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Aaliyahussain27/RapidReach.git
   ```

2. **Configure Backend:**
   - Create a file named `local.properties` in the root directory (if it doesn't exist).
   - Add your Supabase credentials (optional if managed via code constants):
     ```properties
     SUPABASE_URL="https://your-project-id.supabase.co"
     SUPABASE_KEY="your-anon-key"
     ```

3. **Build & Run:**
   - Open the project in Android Studio.
   - Sync the Gradle files.
   - Run on an emulator or physical device (API Level 24+).

---

## 📷 Screenshots

| Dashboard | SOS Active | Nearby Helpline |
| :---: | :---: | :---: |
| ![Dashboard](https://via.placeholder.com/200x400?text=Dashboard) | ![SOS](https://via.placeholder.com/200x400?text=SOS+Active) | ![Helpline](https://via.placeholder.com/200x400?text=Helpline) |

---

## 🧩 Project Structure

```text
app/src/main/java/com/example/rapidreach/
├── data/           # Repositories & Supabase Models
├── navigation/     # NavHost & Route Definitions
├── screens/        # Compose Screens (Dashboard, Map, SOS, etc.)
├── services/       # Foreground Services (SosService)
├── ui/             # Theme & Components
├── utils/          # Location & Security Helpers
├── viewmodel/      # Business Logic (AuthViewModel, SosViewModel)
└── workers/        # Background Sync Workers
```

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue for any bugs/feature requests.

---
*Stay Safe, Stay Connected with RapidReach.*
