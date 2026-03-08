# TASK.md — RapidReach Android App
## For GitHub Copilot: Read this entire file before writing any code.

---

## PROJECT OVERVIEW

**App Name:** RapidReach  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Purpose:** Personal safety app — one-tap SOS with live tracking, offline fallback, audio recording, and emergency contact alerts.  
**Architecture:** MVVM + Repository Pattern + Offline-First  
**Backend:** Firebase Auth + Firestore + Firebase Storage  
**Local DB:** Room (offline-first, synced via WorkManager)

---

## PACKAGE NAME

```
com.example.rapidreach
```

---

## COMPLETE FILE STRUCTURE

```
app/src/main/java/com/example/rapidreach/
│
├── MainActivity.kt
├── RapidReachApp.kt                          ← Application class
│
├── data/
│   ├── model/
│   │   └── User.kt                           ← All data models
│   ├── local/
│   │   ├── RapidReachDatabase.kt             ← Room DB singleton
│   │   ├── entity/
│   │   │   └── SosLogEntity.kt               ← Room table: sos_logs
│   │   └── dao/
│   │       └── SosLogDao.kt                  ← insert, getUnsynced, markSynced
│   └── repository/
│       ├── AuthRepository.kt                 ← Firebase Auth + Firestore
│       └── SosRepository.kt                  ← SMS, Room, Firestore SOS logic
│
├── navigation/
│   ├── Routes.kt                             ← Route constants
│   └── NavGraph.kt                           ← NavHost, auth-aware start destination
│
├── viewmodel/
│   ├── AuthViewModel.kt                      ← Login/signup/logout state
│   └── SosViewModel.kt                       ← SOS state machine
│
├── services/
│   └── SosService.kt                         ← Foreground Service: GPS + audio
│
├── workers/
│   └── SyncWorker.kt                         ← WorkManager: offline → cloud sync
│
└── screens/
    ├── auth/
    │   ├── LoginScreen.kt
    │   └── SignupScreen.kt
    ├── dashboard/
    │   └── DashboardScreen.kt
    ├── helpline/
    │   └── HelplineScreen.kt
    ├── map/
    │   └── NearbyMapScreen.kt                ← Google Maps + nearby police/hospitals
    └── profile/
        └── ProfileScreen.kt
```

---

## DATA MODELS — `data/model/User.kt`

```kotlin
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val age: Int = 0,
    val gender: String = "",           // Male | Female | Other | Prefer not to say
    val userType: String = "",         // Student | Working Professional | Elderly | Child
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val medicalInfo: MedicalInfo? = null,
    val safetyPreferences: SafetyPreferences = SafetyPreferences()
) : Serializable

data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relation: String = ""           // Family | Friend | Colleague
) : Serializable

data class MedicalInfo(
    val bloodGroup: String = "",
    val allergies: String = "",
    val medications: String = "",
    val medicalConditions: String = ""
) : Serializable

data class SafetyPreferences(
    val autoSOSEnabled: Boolean = false,
    val locationSharingEnabled: Boolean = true,
    val offlineTrackingEnabled: Boolean = true,
    val geofencingEnabled: Boolean = false,
    val checkInReminders: Boolean = false
) : Serializable

// SOS state machine states
sealed class SosState {
    object Idle : SosState()
    object ConfirmDialog : SosState()
    data class Active(val isOnline: Boolean, val officialService: OfficialService? = null) : SosState()
    object Cancelled : SosState()
}

enum class OfficialService { POLICE, AMBULANCE }
```

---

## ROOM DATABASE

### `SosLogEntity.kt`
```kotlin
@Entity(tableName = "sos_logs")
data class SosLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String = "",
    val synced: Boolean = false,
    val officialService: String = ""
)
```

### `SosLogDao.kt`
```kotlin
@Dao
interface SosLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SosLogEntity): Long

    @Query("SELECT * FROM sos_logs WHERE synced = 0")
    suspend fun getUnsynced(): List<SosLogEntity>

    @Query("UPDATE sos_logs SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM sos_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SosLogEntity>>
}
```

### `RapidReachDatabase.kt`
```kotlin
@Database(entities = [SosLogEntity::class], version = 1, exportSchema = false)
abstract class RapidReachDatabase : RoomDatabase() {
    abstract fun sosLogDao(): SosLogDao
    companion object {
        @Volatile private var INSTANCE: RapidReachDatabase? = null
        fun getInstance(context: Context): RapidReachDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext,
                    RapidReachDatabase::class.java, "rapidreach_db").build().also { INSTANCE = it }
            }
    }
}
```

---

## REPOSITORIES

### `AuthRepository.kt`
- `suspend fun signup(email, password, user: User): Result<User>` — Firebase createUser → Firestore set
- `suspend fun login(email, password): Result<User>` — Firebase signIn → Firestore get
- `fun logout()` — Firebase signOut
- `fun isLoggedIn(): Boolean` — checks FirebaseAuth.currentUser != null
- `fun getCurrentUserId(): String?`
- `suspend fun getUserData(userId): Result<User>`
- `suspend fun updateUser(user: User): Result<Unit>`

### `SosRepository.kt`
- `suspend fun saveLocalLog(userId, lat, lng, audioPath, officialService): Long` — Room insert, offline-first
- `fun sendSmsFallback(contacts: List<EmergencyContact>, lat, lng)` — SmsManager, Google Maps link in message
- `suspend fun uploadAndMarkSynced(log: SosLogEntity)` — Firestore upload then Room markSynced
- `suspend fun syncPendingLogs()` — loops getUnsynced() → uploadAndMarkSynced each
- `suspend fun pushLiveLocation(userId, lat, lng)` — Firestore: live_tracking/{userId}.set(lat, lng, timestamp)

---

## VIEWMODELS

### `AuthViewModel` (AndroidViewModel)
State: `AuthUiState` sealed class → `Idle | Loading | Success(user) | Error(message)`
Also exposes: `isLoggedIn: StateFlow<Boolean>`, `currentUser: StateFlow<User?>`

Functions:
- `fun login(email, password)` — calls repo, updates states
- `fun signup(name, email, phone, password, confirmPassword, userType, gender)` — validates passwords match, calls repo
- `fun logout()` — calls repo, resets all states
- `fun resetState()` — sets uiState back to Idle

### `SosViewModel` (AndroidViewModel)
State: `sosState: StateFlow<SosState>` — sealed class transitions

Functions:
- `fun onSosPressed()` — `_sosState = ConfirmDialog`
- `fun onSosDismissed()` — `_sosState = Idle`
- `fun onSosConfirmed(userId, contacts, lat, lng, officialService?)`:
  1. Set state to Active
  2. Start SosService (startForegroundService)
  3. Save to Room (SosRepository.saveLocalLog)
  4. Check network: if online → pushLiveLocation; if offline → sendSmsFallback
  5. Schedule SyncWorker
- `fun onSosCancelled()` — Idle + stopService(SosService)
- `private fun isNetworkAvailable(): Boolean` — ConnectivityManager + NetworkCapabilities

---

## SERVICES

### `SosService.kt` (extends Service)
**Type:** Foreground Service  
**Manifest entry required:**
```xml
<service
    android:name=".services.SosService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

`onStartCommand`:
1. `startForeground(NOTIF_ID, buildNotification())` — persistent notification required
2. Launch coroutine 1: `startLocationTracking()` using FusedLocationProviderClient, PRIORITY_HIGH_ACCURACY, interval 4000ms
3. Launch coroutine 2: `startAudioRecording()` using MediaRecorder → saves to `filesDir/sos_audio_{timestamp}.mp4`

`onDestroy`:
- Cancel coroutine scope
- `MediaRecorder.stop()` + `.release()`
- `FusedLocationClient.removeLocationUpdates(callback)`

Return `START_STICKY` from `onStartCommand`.

---

## WORKERS

### `SyncWorker.kt` (extends CoroutineWorker)
- `doWork()`: calls `SosRepository.syncPendingLogs()`, returns `Result.success()` or `Result.retry()` (max 3 attempts)
- `companion object fun schedule(context)`: OneTimeWorkRequest with `NetworkType.CONNECTED` constraint, `ExistingWorkPolicy.REPLACE`, exponential backoff

---

## NAVIGATION

### `Routes.kt`
```kotlin
object Routes {
    const val LOGIN     = "login"
    const val SIGNUP    = "signup"
    const val DASHBOARD = "dashboard"
    const val PROFILE   = "profile"
    const val HELPLINE  = "helpline"
    const val MAP       = "map"
}
```

### `NavGraph.kt`
- Start destination determined by `AuthViewModel.isLoggedIn` at composition time
- LOGIN → on success popUpTo(LOGIN, inclusive=true) → DASHBOARD
- SIGNUP → on success popUpTo(LOGIN, inclusive=true) → DASHBOARD
- DASHBOARD → can navigate to PROFILE, HELPLINE, MAP; logout clears entire back stack → LOGIN
- All ViewModels shared via `viewModel()` — same instance across recompositions

---

## SCREENS

### `LoginScreen.kt`
- Fields: Email (OutlinedTextField), Password (OutlinedTextField with visibility toggle)
- Button: "Login" → calls `AuthViewModel.login()`; shows CircularProgressIndicator while Loading
- Error text shown below password field when `AuthUiState.Error`
- `LaunchedEffect(uiState)` navigates on `AuthUiState.Success`
- TextButton: "Don't have an account? Sign Up" → `onSignupClick()`
- Primary color: `Color(0xFF650927)`

### `SignupScreen.kt`
- Fields: Full Name, Email, Phone Number, Password, Confirm Password
- Dropdowns (ExposedDropdownMenuBox):
  - "I am a..." → options: Student | Working Professional | Elderly | Child
  - "Gender" → options: Male | Female | Other | Prefer not to say
- Button: "Create Account" → calls `AuthViewModel.signup(...)`; loading spinner during Loading
- Error text on mismatch or Firebase error
- Back arrow in TopAppBar → `onBackToLogin()`

### `DashboardScreen.kt`
- TopAppBar: "RapidReach" title, subtitle "Hello, {firstName} · {userType}", Profile icon, Logout icon
- **SOS Button**: Large circular Button (200dp), color `Color(0xFF650927)`
  - When `SosState.Idle`: label "SOS", presses → `sosViewModel.onSosPressed()`
  - When `SosState.Active`: label "STOP", color `Color(0xFF8B0000)`, pulsing scale animation, press → `sosViewModel.onSosCancelled()`
- Status chip when Active: "🆘 SOS ACTIVE — Help is on the way"
- Feature buttons row (ElevatedButton, CircleShape, 68dp): Fake Call, Live Share, Helplines, Nearby
- Role-based safety card (Card) at bottom — different emoji+tip per userType
- `OfficialServiceDialog` shown when `SosState.ConfirmDialog`:
  - Title: "Would you like to alert official services?"
  - Body: voice-friendly instructions
  - Buttons: "👮 Yes — Alert Police" (dark blue), "🚑 Yes — Alert Ambulance" (green), "No — Contacts Only" (outlined), Cancel (text)
  - On Police/Ambulance: also fires `Intent.ACTION_CALL` to "tel:100" or "tel:108"

### `HelplineScreen.kt`
- List of helplines with emoji, name, number, category
- Sorted by relevance to `currentUser.userType`:
  - Student → student mental health first
  - Elderly → senior citizen (14567) first
  - Child → child helpline (1098) first
  - Working Professional → women's helpline first
- Each card has a Call button → `Intent.ACTION_DIAL`
- Full helpline list:
  - Police: 100 | Ambulance: 108 | Fire: 101 | National Emergency: 112
  - Women: 1091 | Women DV: 181 | Child: 1098 | Senior Citizen: 14567
  - Disaster: 1078 | Road Accident: 1073 | Cyber Crime: 1930
  - Student Mental Health: iCall 9152987821

### `NearbyMapScreen.kt` ← **NEEDS TO BE BUILT**
- Google Maps Compose (`maps-compose` library)
- On load: get current location via FusedLocationProviderClient
- Call Google Places API Nearby Search:
  - type = "police" → show blue markers
  - type = "hospital" → show red markers
- Bottom sheet shows list of results with name, distance, "Navigate" button
- "Navigate" button → Intent to Google Maps with destination lat/lng
- Library: `com.google.maps.android:maps-compose:4.3.0`

### `ProfileScreen.kt`
- Avatar placeholder (Person icon in circle)
- User name (large), userType (subtitle)
- Info card: Email, Phone, Gender, Blood Group
- Emergency contacts section — list of cards with name, phone, relation
- Edit button (optional for now)

---

## PERMISSIONS (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## DEPENDENCIES (app/build.gradle.kts)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.8.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Permissions (Accompanist)
implementation("com.google.accompanist:accompanist-permissions:0.34.0")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Location
implementation("com.google.android.gms:play-services-location:21.2.0")

// Maps
implementation("com.google.maps.android:maps-compose:4.3.0")
implementation("com.google.android.gms:play-services-maps:18.2.0")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Encrypted preferences
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

---

## SOS LOGIC FLOW (for Copilot context)

```
User taps SOS
    → SosViewModel.onSosPressed() → state = ConfirmDialog
    → OfficialServiceDialog appears
    → User picks Police / Ambulance / No
    → SosViewModel.onSosConfirmed(userId, contacts, lat, lng, officialService?)
        → state = Active
        → startForegroundService(SosService)
            → SosService: GPS every 4s + MediaRecorder audio
        → SosRepository.saveLocalLog() → Room (synced=false)
        → if (isNetworkAvailable)
              SosRepository.pushLiveLocation() → Firestore live_tracking/{uid}
          else
              SosRepository.sendSmsFallback() → SmsManager → all contacts
        → if (officialService != null)
              Intent.ACTION_CALL → "tel:100" (police) or "tel:108" (ambulance)
        → SyncWorker.schedule() → queued for when network returns

Network returns:
    SyncWorker.doWork()
        → SosRepository.syncPendingLogs()
            → Room.getUnsynced() → for each: Firestore.add() → Room.markSynced()

User taps STOP:
    → SosViewModel.onSosCancelled()
        → state = Idle
        → stopService(SosService)
            → MediaRecorder.stop/release
            → FusedLocation.removeLocationUpdates
```

---

## ROLE-BASED BEHAVIOR

| userType | HelplineScreen priority | Dashboard safety tip | Future feature |
|----------|------------------------|----------------------|----------------|
| Student | iCall mental health, Police | Awareness for late-night commutes | Auto-SOS after 10pm |
| Working Professional | Women helpline, Police | Enable SOS for commutes | Scheduled check-ins |
| Elderly | Senior citizen 14567, Medical | Geofencing, family alerts | Fall detection hook |
| Child | Child helpline 1098, Police | Always with trusted adult | Guardian instant notification |

---

## WHAT COPILOT SHOULD BUILD / COMPLETE

1. **`NearbyMapScreen.kt`** — Full Google Maps screen with police (blue markers) and hospital (red markers) using Places API Nearby Search. Bottom sheet listing results. Navigate button.

2. **`SosViewModel.kt`** — Wire real FusedLocationProviderClient inside ViewModel (not hardcoded 0.0, 0.0). Use `getCurrentLocation()` before calling `onSosConfirmed`.

3. **`DashboardScreen.kt`** — Add Speech Recognition to `OfficialServiceDialog`. When dialog opens, start `SpeechRecognizer`. Listen for keywords: "police" → trigger POLICE, "ambulance" → trigger AMBULANCE, "no" → trigger null. Show listening indicator in dialog.

4. **`ProfileScreen.kt`** — Add editable form for emergency contacts. User should be able to add/remove up to 5 contacts (name, phone, relation). Save changes via `AuthRepository.updateUser()`.

5. **`SosService.kt`** — After `stopAudioRecording()`, pass the audio file path back to `SosRepository` and call `uploadAudioFile()` to Firebase Storage at path `audio/{userId}/{filename}`.

6. **`SyncWorker.kt`** — After syncing logs, send a confirmation SMS to all emergency contacts: "✅ RapidReach: Your contact's emergency data has been synced. They are safe."

7. **Fake Call feature** in `DashboardScreen.kt` — Tapping "Fake Call" should show a full-screen composable mimicking an incoming call UI (caller name configurable in profile), with ringtone via `MediaPlayer`, Accept/Decline buttons.

---

## DESIGN SYSTEM

- Primary color: `Color(0xFF650927)` (deep maroon)
- Background: `Color(0xFFFDFDFD)` (near white)
- SOS Active card background: `Color(0xFFFFF3F5)`
- Font weights: ExtraBold for brand, Bold for headings, Medium for labels
- Shape: `MaterialTheme.shapes.medium` for cards and buttons
- SOS button: 200dp CircleShape, shadow 16dp, pulsing InfiniteTransition when active
- All icons from `androidx.compose.material.icons.filled.*`

---

## IMPORTANT IMPLEMENTATION NOTES FOR COPILOT

- Never access Room on the main thread — always use `Dispatchers.IO`
- `SosService` must return `START_STICKY` so Android restarts it if killed
- `MediaRecorder` must call `prepare()` before `start()`
- `FusedLocationProviderClient.requestLocationUpdates()` requires `@SuppressLint("MissingPermission")` — permissions are requested from UI before SOS is triggered
- `WorkManager` `enqueueUniqueWork` with `ExistingWorkPolicy.REPLACE` prevents duplicate sync jobs
- `ExposedDropdownMenuBox` requires `Modifier.menuAnchor()` on the TextField inside it (Material3)
- NavGraph: use `popUpTo(Routes.LOGIN) { inclusive = true }` on login/signup success to prevent back-navigation to auth screens
- Firebase Firestore `.toObject(User::class.java)` requires all fields to have default values — already done in the model
- `startForegroundService()` requires the notification channel to be created BEFORE calling `startForeground()` — done in `onCreate()`

---

*End of TASK.md — Copilot: read everything above before generating any code.*
