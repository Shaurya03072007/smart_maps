# Smart Maps 🗺️⚡

An AI-first local service discovery and navigation platform for Android, connecting consumers with verified local service providers (plumbers, electricians, AC technicians, mechanics, etc.) in real time.

Powered by a **Google ADK (`google-adk`)** Agentic Concierge on the backend, a **sub-15ms Beast Hybrid Search Engine**, and **keyless vector maps (MapLibre + OpenFreeMap)**.

---

## 🌟 Key Features

### 1. 🤖 Google ADK Agentic Concierge Backend (`backend/`)
- Built with **Google ADK (`google-adk`)** and Gemini (`gemini-2.5-flash`).
- **Stateful Multi-Turn Sessions**: Remembers active service context, price comparisons, and user location across conversational turns.
- **Chat-to-Map Integration**: Every AI recommendation generates live `mapContext` pins rendered directly in the chat stream.
- **Offline / Zero-Key Fallback**: Deterministic local fallback keeps ranking, recommendations, and pricing working even without external network access.

### 2. ⚡ Beast-Level Unified Search (<15ms)
- Ultra-low latency hybrid search across **Verified Vendors**, **Service Catalogs**, and **Map Places**.
- Contextual intent classification mapping problem descriptions (e.g. *"water leaking from AC"* -> `ac_repair`) to verified providers with fair ₹ labour estimates.
- Multi-factor **Smart Vendor Ranking** combining semantic relevance, distance proximity, ratings, and real-time availability.

### 3. 🗺️ Keyless Vector Maps & Live GPS Radar
- **Zero API Keys Needed**: Integrated with MapLibre Native and OpenFreeMap Liberty vector tiles.
- **Live User GPS**: Real-time pulsing GPS radar marker (`📍 YOU - Live GPS`).
- **Interactive Vendor Pointers**: Direct map callouts (`🛠️ Vendor ★4.9`) with an interactive bottom carousel to point out and fly to providers.
- **In-Map Search**: Search and filter vendors and places directly inside the map view.

### 4. 🤝 2-Way Vendor–Consumer Connection
- **Live Vendor Chat**: Direct 2-way messaging for quotes, arrival ETAs, and status updates.
- **Vendor Hub**: Provider dashboard with active jobs, earnings overview, status progression (Pending → Accepted → On The Way → Arrived → Completed), and an Online/Offline toggle.
- **AI Match Badges**: Clear % Match score, estimated labour cost range (₹), and verified pro badges.

---

## 🏗️ Architecture

```
smartmaps/
├── app/                        # Android Client (Jetpack Compose, Hilt, Room, Retrofit, MapLibre)
│   ├── src/main/java/com/smartmaps/app/
│   │   ├── data/               # Local Room DB, repositories, backend API interfaces
│   │   ├── di/                 # Dependency injection (Hilt modules)
│   │   ├── ui/
│   │   │   ├── screens/        # Compose screens (Home, Map, Chat, AiSearch, Explore, Detail, Hub)
│   │   │   ├── components/     # Reusable UI cards, chips, rating bars
│   │   │   └── viewmodel/      # MVVM state management
│   │   └── SmartMapsApplication.kt
│   └── src/main/res/           # Keyless resources & styling
│
├── backend/                    # Agentic Platform Server (FastAPI + Google ADK)
│   ├── adk_agent.py            # Google ADK agent concierge & tools
│   ├── search_engine.py        # Beast Hybrid Search Engine
│   ├── database.py             # SQLite + dynamic GPS vendor projection
│   ├── main.py                 # REST endpoints (/api/chat, /api/search, /api/vendors, etc.)
│   ├── .env.example            # Environment template (NO secrets checked into git)
│   └── requirements.txt        # Python backend dependencies
└── README.md
```

---

## 🚀 Getting Started

### 1. Backend Setup
```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt

# Copy example environment file
cp .env.example .env
# Add your Gemini API key (from Google AI Studio) to GOOGLE_API_KEY in .env

# Start the server
python main.py
```
The backend starts at `http://0.0.0.0:8000`.

### 2. Android Client Setup
- Open the project in **Android Studio**.
- Ensure Android SDK 35 is installed.
- For physical device testing over USB:
  ```bash
  adb reverse tcp:8000 tcp:8000
  ```
- Build and run:
  ```bash
  ./gradlew assembleDebug
  ```

---

## 🔒 Security
- **Zero API keys in Android Studio client code**: All Gemini AI, MapTiler, and database secrets live strictly on the backend.
- `.env` and local database files are excluded from Git version control.
