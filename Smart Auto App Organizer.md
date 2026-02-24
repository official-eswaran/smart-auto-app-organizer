# Smart Auto App Organizer – Production Level System

## Project Goal

Build a **production-grade Android Launcher application** that automatically scans installed apps, classifies them using AI, and groups them into intelligent folders (Games, Payments, Social, Shopping, etc.) dynamically.

This system must:
- Work fully offline
- Use on-device ML classification
- Auto-create folders
- Update dynamically on new app installs
- Allow manual override
- Support search, dock, widgets, and notification badges
- Offer theme and wallpaper customization
- Back up and restore folder layout
- Improve classification via user correction feedback
- Be scalable and production ready

---

# System Architecture

User Device
    ↓
Custom Launcher App
    ↓
App Scanner Module
    ↓
Metadata Extractor (name, package, permissions, Play Store category, usage stats)
    ↓
AI Classification Engine (Rule-based + ML + Confidence Threshold + Multi-label)
    ↓
User Feedback Loop (correction signals → local dataset)
    ↓
Folder Management System (color, lock, rename, badge count)
    ↓
Dynamic UI Renderer (home grid, dock, search, widgets, theme)

---

# Technology Stack

## Mobile App
- Kotlin
- Jetpack Compose
- MVVM Architecture
- Hilt (Dependency Injection)
- Room Database
- DataStore Preferences (theme, dock, onboarding state)
- Coroutines & Flow
- WorkManager

## Machine Learning
- Python (Model Training)
- Scikit-learn / TensorFlow
- TensorFlow Lite (On-device inference)

## Testing
- JUnit
- Espresso
- MockK

---

# Project Folder Structure

smart-auto-app-organizer/

app/
│
├── data/
│   ├── database/
│   ├── models/
│   ├── repository/
│   └── backup/
│
├── domain/
│   └── usecases/
│
├── ui/
│   ├── home/
│   ├── folder/
│   ├── dock/
│   ├── search/
│   ├── onboarding/
│   ├── settings/
│   └── components/
│
├── classification/
│   ├── rule_engine/
│   ├── ml_engine/
│   ├── tflite_model/
│   └── feedback/
│
├── scanner/
├── notification/
├── theme/
│
└── MainActivity.kt

ml-training/
│
├── dataset/
├── user_corrections/
├── training.ipynb
├── retrain_with_feedback.ipynb
└── export_to_tflite.py

---

# MODULE 1 – Launcher Setup

1. Create Android project (Min SDK 26+, Kotlin, Compose enabled).
2. Add launcher intent in AndroidManifest.xml:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN"/>
    <category android:name="android.intent.category.HOME"/>
    <category android:name="android.intent.category.DEFAULT"/>
</intent-filter>
```

3. Handle default launcher selection.
4. Implement home screen grid using LazyVerticalGrid.
5. Add fixed Dock bar at the bottom (pinned apps row).
6. Add Search bar at the top of the home screen.
7. Support wallpaper binding via WallpaperManager.
8. Respect system dark/light mode; support manual override in settings.

---

# MODULE 2 – App Scanner

Goal: Retrieve installed user apps.

Use PackageManager:

- Fetch installed apps
- Exclude system apps
- Include launchable apps only

Extract:
- App Name
- Package Name
- Icon
- Install Time
- Permissions
- Play Store Category (via ApplicationInfo.category — available API 26+)

Play Store Category values to map:

```
ApplicationInfo.CATEGORY_GAME       → Games
ApplicationInfo.CATEGORY_AUDIO      → Music
ApplicationInfo.CATEGORY_VIDEO      → Video
ApplicationInfo.CATEGORY_IMAGE      → Photography
ApplicationInfo.CATEGORY_SOCIAL     → Social
ApplicationInfo.CATEGORY_NEWS       → News
ApplicationInfo.CATEGORY_MAPS       → Navigation
ApplicationInfo.CATEGORY_PRODUCTIVITY → Productivity
ApplicationInfo.CATEGORY_UNDEFINED  → fallback to rule/ML engine
```

Store results in Room database.

---

# MODULE 3 – Metadata Enrichment

Enhance classification inputs using:
- App name
- Package name
- Permissions
- Play Store category (from ApplicationInfo)
- Usage stats (UsageStatsManager) — launch count, last used time

Usage Frequency Scoring:

```
Score = (launch_count * 0.7) + (recency_weight * 0.3)
```

- Sort apps inside each folder by score descending.
- Surface top-3 most-used apps per folder as folder preview icons.
- Pin highest-scored apps across all folders to Dock automatically (overridable).

---

# MODULE 4 – Classification Engine

## Option A: Rule-Based (MVP)

Keyword mapping example:

Payments  → pay, upi, bank, wallet, money, finance, gpay, phonepe, paytm
Games     → game, battle, racing, puzzle, arena, squad, clash, craft
Social    → chat, message, social, friend, meet, talk, whatsapp, telegram
Shopping  → shop, store, buy, market, deal, cart, amazon, flipkart
Music     → music, song, audio, beat, radio, spotify, gaana
Health    → health, fit, doctor, med, pharmacy, workout, calorie
Travel    → travel, cab, ride, flight, hotel, train, ola, uber
News      → news, daily, times, feed, headlines

Algorithm:
1. Convert app name + package name to lowercase
2. Match against keyword lists
3. If Play Store category is defined and not UNDEFINED → use it as primary signal
4. Assign category with highest keyword match score
5. Default to "Others" if no match

---

## Option B: Machine Learning (Production)

### Step 1: Dataset Creation

Create dataset format:

App Name | Package Name | Play Store Category | Permissions | Description | Label

Collect 5,000+ app samples.
Include Play Store category and top permissions as additional features.

---

### Step 2: Model Training (Python)

Pipeline:

- Feature: TF-IDF on (app name + package name tokens)
- Feature: One-hot encoded Play Store category
- Feature: Binary permission flags (CAMERA, LOCATION, CONTACTS, etc.)
- Classifier: Logistic Regression (multiclass, one-vs-rest)
- Output: Category label + confidence score (probability)

Train model → Evaluate (target accuracy > 90%) → Save

---

### Step 3: Confidence Threshold

After inference, apply a confidence gate:

```
IF confidence >= 0.75 → assign predicted category
IF confidence >= 0.50 → assign category but mark as "uncertain" (user can confirm)
IF confidence < 0.50  → assign to "Others", trigger user prompt to categorize manually
```

This prevents overconfident wrong classifications.

---

### Step 4: Multi-label Classification

Some apps belong to 2 categories (e.g., Paytm = Payments + Shopping).

- Store top-2 predicted labels if both confidence scores > 0.40.
- Primary label → folder placement.
- Secondary label → shown as a tag on the app card in folder view.
- User can switch the active folder assignment from the long-press menu.

---

### Step 5: Convert to TensorFlow Lite

Export trained model to .tflite
Place model in Android assets folder.

---

### Step 6: Integrate TFLite

- Load model at app startup
- Convert app metadata to feature vector
- Run inference
- Apply confidence threshold
- Map output to category label

Ensure inference < 50ms per app.

---

### Step 7: User Correction Feedback Loop

When a user manually moves an app to a different folder:

1. Record correction: { package_name, predicted_label, correct_label, timestamp }
2. Store in local corrections database (Room).
3. Export corrections as CSV via WorkManager periodically to ml-training/user_corrections/.
4. Retrain model offline using retrain_with_feedback.ipynb.
5. Ship improved .tflite via in-app model update (see MODULE 17).

This closes the ML improvement loop without requiring internet access.

---

# MODULE 5 – Folder Management

Database Tables:

Apps:
- id
- name
- packageName
- category
- secondaryCategory
- confidenceScore
- isManualOverride
- usageScore
- lastUsed

Folders:
- id
- name
- icon
- colorHex
- isLocked
- sortOrder

Logic:

```
IF category folder exists → add app
ELSE → create folder → add app
```

Allow:
- Manual move between folders
- Folder rename
- Folder color customization (preset palette + custom hex)
- Folder lock (prevents auto-reclassification from moving apps out)
- Disable auto-classification per app
- Sort apps within folder by: usage score / install date / alphabetical

Notification Badge:

- Each folder aggregates unread notification counts from apps inside it.
- Display badge number on folder card icon.
- Requires Notification Listener Service (user grants permission during onboarding).

---

# MODULE 6 – UI System

## Home Screen
- Grid layout with folder cards
- Tap folder → Expand bottom sheet showing apps inside
- Folder card shows: name, color, top-3 app icons, notification badge count
- Search bar pinned at top
- Dock row pinned at bottom

## App Interactions
- Tap → Launch app
- Long press → Move / Remove / Override category / Pin to Dock

## Search
- Real-time search across all installed apps (not just folder names)
- Fuzzy match on app name
- Show app icon + category tag in results
- Tap result → launch app directly

## Dock
- Fixed row of 4–5 pinned apps at the bottom
- Drag and drop to reorder
- Long press → Remove from dock
- Auto-suggest dock apps based on usage score (overridable)

## Theme & Wallpaper
- Dark mode / Light mode toggle (follows system by default)
- Accent color picker for UI elements
- Wallpaper picker (from gallery or built-in presets)
- Folder card blur/transparency style option

## Widgets
- Support standard Android widget placement on home screen
- Long press empty area → Add Widget flow
- Widgets resize by drag

## Notification Badges
- Listen via NotificationListenerService
- Count active notifications per package
- Update folder badge in real time via Flow

## Animations
- Compose animations for folder expand/collapse
- Smooth 60fps rendering
- Lazy loading icons

---

# MODULE 7 – Onboarding Flow

Trigger: First launch after install.

Steps:

1. Welcome screen — explain AI grouping concept (2 slides).
2. Request permissions:
   - QUERY_ALL_PACKAGES (app scanning)
   - PACKAGE_USAGE_STATS (usage frequency)
   - BIND_NOTIFICATION_LISTENER_SERVICE (badge counts)
3. Set as default launcher prompt.
4. Initial scan + classification progress screen (animated).
5. Show result preview — "Your apps have been organized!"
6. Quick tip: "Long press any app to move it manually."

Store onboarding completion state in DataStore.
Skip onboarding on subsequent launches.

---

# MODULE 8 – Dynamic Updates

Use BroadcastReceiver:

Trigger on:
- ACTION_PACKAGE_ADDED
- ACTION_PACKAGE_REMOVED
- ACTION_PACKAGE_CHANGED

When triggered:
1. Re-scan app
2. Classify (rule + ML with confidence threshold)
3. Update database
4. Refresh UI
5. Re-evaluate dock suggestions if usage scores shift

Run classification using WorkManager in background.

---

# MODULE 9 – Performance Optimization

- Cache classification results; only re-run on app change events
- Use background threads for all DB and ML operations
- Avoid blocking UI thread
- Lazy icon loading with Coil
- Debounce bulk updates (e.g., 10+ apps installed at once)
- Notification badge aggregation runs on IO dispatcher

Target:
- Cold start < 1.5 seconds
- Smooth scrolling
- Memory optimized for 200+ apps

---

# MODULE 10 – Backup & Restore

Goal: Let users keep their folder organization across reinstalls or device changes.

Backup format: JSON file

```json
{
  "version": 1,
  "exported_at": "2025-01-01T00:00:00Z",
  "folders": [
    { "name": "Payments", "color": "#4CAF50", "locked": false, "apps": ["com.google.android.apps.nbu.paisa.user"] }
  ],
  "dock": ["com.whatsapp", "com.google.android.dialer"],
  "theme": { "mode": "dark", "accent": "#6200EE" }
}
```

Export:
- Save to Downloads folder or share via intent.
- Trigger from Settings → Backup.

Restore:
- Read JSON, match packageNames to installed apps.
- Rebuild folder structure; skip uninstalled apps gracefully.
- Trigger from Settings → Restore.

---

# MODULE 11 – ML Model Update Mechanism

Goal: Push improved .tflite models without a full app release.

Strategy (fully offline-safe):

1. Ship initial model inside app APK (assets/classifier.tflite).
2. On each release, bundle improved model in APK update.
3. For faster iteration, support sideload via Settings → Developer → Load custom model (debug builds only).
4. On model load, run a validation check against a local golden test set (50 known app→category pairs) to confirm accuracy before activating.
5. Keep previous model as fallback; if new model accuracy < threshold, revert automatically.

Note: No remote download required — keeps privacy guarantee intact.

---

# MODULE 12 – Privacy & Security

- No internet required
- No app content accessed
- No personal data stored or transmitted
- On-device processing only
- Notification Listener only counts badges — no notification content is read or stored
- Usage stats used only for sorting — not sent anywhere
- Backup file contains only package names and UI preferences, no personal content

---

# MODULE 13 – Testing Strategy

Unit Tests:
- Classification accuracy (rule engine keyword matching)
- ML confidence threshold logic
- Multi-label assignment logic
- Folder creation and deduplication
- Database integrity (CRUD for apps, folders, dock)
- Backup serialization / deserialization
- Usage score calculation

UI Tests:
- Folder expansion bottom sheet
- App launching from folder and search
- Manual override and folder reassignment
- Dock add / remove / reorder
- Onboarding flow completion
- Theme switching (dark/light)

Stress Tests:
- 200+ apps scenario
- Bulk install simulation
- Notification badge aggregation under high notification load
- Search performance with 200+ apps

---

# Deployment Plan

1. Internal testing across Android 8–14.
2. Complete onboarding flow with permission explanations.
3. Prepare privacy policy (emphasize fully offline, no data collection).
4. Prepare Play Store screenshots showing before/after folder organization.
5. Publish under "Launcher" category in Play Store.
6. Collect user corrections and retrain ML model for v1.1 update.
7. Ship improved .tflite model bundled in v1.1 APK.

---

# Example Output

Before:

GPay | BGMI | Instagram | PhonePe | Spotify | Amazon | Paytm | Ola | Times of India

After:

Dock (auto-suggested by usage):  GPay | WhatsApp | Chrome | Camera

Folders:
Payments   → GPay, PhonePe, Paytm
Games      → BGMI
Social     → Instagram
Shopping   → Amazon, Paytm (secondary label)
Music      → Spotify
Travel     → Ola
News       → Times of India

Search: type "spo" → Spotify appears instantly

---

# Production Checklist

## Core
- [ ] Launcher intent working
- [ ] App scanner stable (excludes system apps)
- [ ] Play Store category extracted from ApplicationInfo
- [ ] Room database integrated
- [ ] Rule-based classification engine working
- [ ] TFLite integrated with confidence threshold
- [ ] Multi-label classification working
- [ ] Folder creation automatic
- [ ] Folder color customization working
- [ ] Manual override functional
- [ ] User correction feedback recorded to DB
- [ ] Background updates working (BroadcastReceiver + WorkManager)
- [ ] Performance optimized (cold start < 1.5s)

## UX
- [ ] Search bar working (fuzzy match)
- [ ] Dock bar working (auto-suggest + manual pin)
- [ ] Notification badges aggregated per folder
- [ ] Dark / Light mode working
- [ ] Wallpaper picker working
- [ ] Widget support working
- [ ] Onboarding flow complete (permissions, default launcher, initial scan)

## Data
- [ ] Backup export to JSON working
- [ ] Restore from JSON working
- [ ] Usage frequency sorting working inside folders

## Quality
- [ ] Unit tests passing
- [ ] UI tests passing
- [ ] Stress tested with 200+ apps
- [ ] Tested across Android 8–14

---

# Final Deliverable

A fully functional intelligent Android launcher that:

- Automatically groups apps using rule-based + ML classification
- Uses Play Store category as a primary classification signal
- Applies confidence thresholds to avoid wrong classifications
- Supports multi-label apps appearing across relevant folders
- Updates dynamically on install/uninstall
- Improves classification accuracy via user correction feedback loop
- Provides real-time search across all apps
- Shows notification badge counts per folder
- Offers dock with auto-suggested pinned apps
- Supports dark/light theme, accent colors, and wallpaper
- Backs up and restores folder layout to/from JSON
- Works fully offline with no data collection
- Is scalable and production ready

---

END OF FILE
