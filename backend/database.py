import os
import json
import sqlite3
import time
import uuid
import math
from typing import List, Dict, Any, Optional
import httpx
from dotenv import load_dotenv

load_dotenv()

DB_PATH = os.path.join(os.path.dirname(__file__), "smartmaps_backend.db")
SUPABASE_URL = os.getenv("SUPABASE_URL", "").rstrip("/")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY") or os.getenv("SUPABASE_ANON_KEY", "")


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return r * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


# Comprehensive service catalog with synonyms, symptoms, price ranges, and categories
SERVICES_CATALOG: List[Dict[str, Any]] = [
    {
        "id": "ac_repair",
        "category_id": "home_services",
        "category_name": "AC & Cooling",
        "name": "AC Repair & Gas Refill",
        "icon": "❄️",
        "description": "Diagnose and repair split/window AC cooling issues, gas leak, PCB fault, water leakage, and compressor.",
        "keywords": ["ac", "air conditioner", "not cooling", "cooling", "gas refill", "split ac", "window ac", "ac leak", "compressor", "hvac", "ac service", "ac cleaning"],
        "labour_min": 350,
        "labour_max": 1200,
        "duration_min": 45,
        "duration_max": 120,
        "workers": 1
    },
    {
        "id": "plumbing_tap_repair",
        "category_id": "home_services",
        "category_name": "Plumbing",
        "name": "Tap, Pipe & Leak Repair",
        "icon": "🚿",
        "description": "Fix leaking taps, burst pipes, blocked drains, flush tanks, water motors, and bathroom fittings.",
        "keywords": ["plumber", "plumbing", "tap", "faucet", "leak", "leaking", "pipe", "burst", "water", "drain", "clog", "blocked", "sink", "toilet", "flush", "commode", "geyser"],
        "labour_min": 200,
        "labour_max": 750,
        "duration_min": 30,
        "duration_max": 90,
        "workers": 1
    },
    {
        "id": "electrical_wiring",
        "category_id": "home_services",
        "category_name": "Electrical",
        "name": "Electrician & Fault Repair",
        "icon": "⚡",
        "description": "Fix short circuits, MCB tripping, switchboards, ceiling fans, inverter/UPS wiring, and lighting.",
        "keywords": ["electrician", "electrical", "wiring", "switch", "socket", "mcb", "short circuit", "spark", "no power", "power cut", "fan", "ceiling fan", "inverter", "ups", "battery", "light"],
        "labour_min": 150,
        "labour_max": 800,
        "duration_min": 30,
        "duration_max": 120,
        "workers": 1
    },
    {
        "id": "washing_machine_repair",
        "category_id": "appliance_repair",
        "category_name": "Appliance Repair",
        "name": "Washing Machine & Fridge Repair",
        "icon": "🫧",
        "description": "Front/top load washing machine drum, spin, drain issues, refrigerator cooling, and microwave repair.",
        "keywords": ["washing machine", "washer", "spin", "drum", "noise", "refrigerator", "fridge", "freezer", "microwave", "oven", "dishwasher", "chimney", "ro", "water purifier", "aquaguard", "tv", "television"],
        "labour_min": 300,
        "labour_max": 1100,
        "duration_min": 45,
        "duration_max": 120,
        "workers": 1
    },
    {
        "id": "carpentry_furniture",
        "category_id": "home_services",
        "category_name": "Carpentry",
        "name": "Carpenter & Door/Lock Repair",
        "icon": "🪑",
        "description": "Furniture assembly, door lock replacement, window hinges, modular kitchen cabinets, and wood polishing.",
        "keywords": ["carpenter", "carpentry", "wood", "furniture", "door", "window", "lock", "hinge", "bed", "sofa", "cupboard", "wardrobe", "almirah", "drill"],
        "labour_min": 250,
        "labour_max": 1000,
        "duration_min": 45,
        "duration_max": 150,
        "workers": 1
    },
    {
        "id": "cleaning_home",
        "category_id": "home_services",
        "category_name": "Cleaning & Pest Control",
        "name": "Deep Cleaning & Pest Control",
        "icon": "🧽",
        "description": "Full home deep cleaning, bathroom sanitization, sofa shampooing, water tank cleaning, cockroach & termite control.",
        "keywords": ["cleaning", "deep clean", "bathroom clean", "sofa clean", "pest control", "cockroach", "termite", "mosquito", "bed bug", "water tank", "maid", "sanitization"],
        "labour_min": 499,
        "labour_max": 2500,
        "duration_min": 90,
        "duration_max": 240,
        "workers": 2
    },
    {
        "id": "car_repair_general",
        "category_id": "vehicle",
        "category_name": "Vehicle & Roadside",
        "name": "Mechanic, Puncture & Roadside Assist",
        "icon": "🚗",
        "description": "24/7 car & bike breakdown assistance, flat tyre puncture repair, battery jumpstart, towing, and car wash.",
        "keywords": ["car", "bike", "mechanic", "puncture", "tyre", "flat tyre", "battery", "jump start", "towing", "breakdown", "roadside", "car wash", "clutch", "brake", "engine", "scooter"],
        "labour_min": 200,
        "labour_max": 1500,
        "duration_min": 20,
        "duration_max": 90,
        "workers": 1
    },
    {
        "id": "laptop_repair",
        "category_id": "technology",
        "category_name": "Tech & CCTV",
        "name": "Laptop, Mobile & CCTV Service",
        "icon": "💻",
        "description": "Laptop screen/keyboard repair, OS installation, WiFi router setup, mobile screen replacement, and CCTV installation.",
        "keywords": ["laptop", "computer", "pc", "mobile", "phone", "screen", "wifi", "router", "internet", "cctv", "camera", "printer", "data recovery", "software", "windows"],
        "labour_min": 300,
        "labour_max": 1500,
        "duration_min": 45,
        "duration_max": 120,
        "workers": 1
    },
    {
        "id": "salon_haircut",
        "category_id": "beauty_wellness",
        "category_name": "Beauty & Wellness",
        "name": "Salon at Home, Spa & Makeup",
        "icon": "💇",
        "description": "Men & women haircut, facial, waxing, bridal/party makeup, massage therapy, yoga and fitness trainer.",
        "keywords": ["salon", "haircut", "barber", "facial", "beauty", "parlour", "makeup", "bridal", "massage", "spa", "waxing", "yoga", "fitness", "trainer"],
        "labour_min": 299,
        "labour_max": 2000,
        "duration_min": 45,
        "duration_max": 120,
        "workers": 1
    },
    {
        "id": "painting_interior",
        "category_id": "construction",
        "category_name": "Painting & Civil",
        "name": "House Painting & Waterproofing",
        "icon": "🎨",
        "description": "Interior & exterior wall painting, dampness waterproofing, POP false ceiling, tiling, and welding.",
        "keywords": ["paint", "painting", "painter", "wall", "waterproofing", "seepage", "damp", "tiles", "tiling", "mason", "welding", "gate", "grill", "ceiling", "pop"],
        "labour_min": 1000,
        "labour_max": 15000,
        "duration_min": 180,
        "duration_max": 720,
        "workers": 2
    },
    {
        "id": "doctor_visit",
        "category_id": "healthcare",
        "category_name": "Healthcare at Home",
        "name": "Doctor Visit, Nursing & Physio",
        "icon": "🩺",
        "description": "Home doctor consultation, physiotherapy for back/joint pain, elder care nursing, and lab sample collection.",
        "keywords": ["doctor", "nurse", "nursing", "physiotherapy", "physio", "pain", "medical", "fever", "health", "clinic", "elder care", "vet", "pet"],
        "labour_min": 400,
        "labour_max": 1500,
        "duration_min": 30,
        "duration_max": 60,
        "workers": 1
    },
    {
        "id": "packers_movers",
        "category_id": "logistics",
        "category_name": "Packers & Logistics",
        "name": "Packers, Movers & Local Delivery",
        "icon": "📦",
        "description": "House shifting, furniture moving, mini-truck tempo hire, and urgent parcel pickup & delivery.",
        "keywords": ["packers", "movers", "shifting", "relocation", "moving", "truck", "tempo", "courier", "delivery", "parcel", "transport"],
        "labour_min": 800,
        "labour_max": 8000,
        "duration_min": 120,
        "duration_max": 360,
        "workers": 3
    }
]


# Seed vendor templates with relative lat/lng offsets so they work around Hyderabad AND dynamically around user's GPS
SEED_VENDORS = [
    {
        "id": "vendor_ac_1",
        "name": "Rajesh Kumar",
        "businessName": "CoolBreeze AC & Appliance Pro",
        "phone": "+919848011223",
        "email": "rajesh@coolbreeze.in",
        "description": "Certified LG, Samsung & Daikin master technician. Fast AC gas refill, PCB repair, jet service & refrigerator repair with 90-day warranty.",
        "dlat": 0.006,
        "dlng": 0.005,
        "address": "Hitech City Main Rd, Near Metro",
        "serviceRadius": 18.0,
        "rating": 4.9,
        "reviewCount": 164,
        "completedJobs": 412,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 350,
        "priceRangeMax": 1200,
        "experienceYears": 11,
        "services": ["ac_repair", "washing_machine_repair"]
    },
    {
        "id": "vendor_plumb_1",
        "name": "Mohammed Irfan",
        "businessName": "AquaFix 24x7 Plumbing & Motors",
        "phone": "+919848022334",
        "email": "irfan@aquafix.in",
        "description": "Emergency leak repair, concealed pipe fixing, pressure pump installation, drain unblocking & Jaguar/Hindware sanitary fittings.",
        "dlat": -0.005,
        "dlng": 0.007,
        "address": "Madhapur Phase 2",
        "serviceRadius": 15.0,
        "rating": 4.8,
        "reviewCount": 128,
        "completedJobs": 350,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 200,
        "priceRangeMax": 700,
        "experienceYears": 9,
        "services": ["plumbing_tap_repair"]
    },
    {
        "id": "vendor_elec_1",
        "name": "Srinivas Rao",
        "businessName": "VoltGuard Electrical & Inverter Hub",
        "phone": "+919848033445",
        "email": "srinivas@voltguard.in",
        "description": "Licensed A-grade electrician. 20-min emergency response for power trips, MCB faults, inverter/battery setup, ceiling fans & full wiring.",
        "dlat": 0.004,
        "dlng": -0.006,
        "address": "Jubilee Hills Checkpost",
        "serviceRadius": 16.0,
        "rating": 4.9,
        "reviewCount": 210,
        "completedJobs": 530,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 150,
        "priceRangeMax": 650,
        "experienceYears": 14,
        "services": ["electrical_wiring"]
    },
    {
        "id": "vendor_plumb_2",
        "name": "Venkatesh Goud",
        "businessName": "Sri Sai Rapid Plumbers & Geyser Fix",
        "phone": "+919848044556",
        "email": "venkatesh@srisaiplumb.in",
        "description": "Affordable tap repair, water tank overflow fix, geyser heating element replacement & kitchen sink block clearance.",
        "dlat": 0.011,
        "dlng": -0.004,
        "address": "Gachibowli Crossroads",
        "serviceRadius": 14.0,
        "rating": 4.6,
        "reviewCount": 89,
        "completedJobs": 215,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 150,
        "priceRangeMax": 500,
        "experienceYears": 7,
        "services": ["plumbing_tap_repair", "electrical_wiring"]
    },
    {
        "id": "vendor_ac_2",
        "name": "Karthik Reddy",
        "businessName": "ArcticCare Home Appliances & AC",
        "phone": "+919848055667",
        "email": "karthik@arcticcare.in",
        "description": "Specialist in inverter split ACs, Bosch/IFB front-load washing machines, double-door refrigerators & RO water purifiers.",
        "dlat": -0.008,
        "dlng": -0.007,
        "address": "Kondapur Main Road",
        "serviceRadius": 20.0,
        "rating": 4.7,
        "reviewCount": 112,
        "completedJobs": 290,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 300,
        "priceRangeMax": 1000,
        "experienceYears": 8,
        "services": ["ac_repair", "washing_machine_repair"]
    },
    {
        "id": "vendor_mech_1",
        "name": "arjun Verma",
        "businessName": "SpeedRescue 24x7 Mechanic & Towing",
        "phone": "+919848066778",
        "email": "arjun@speedrescue.in",
        "description": "On-spot car & bike breakdown repair, tubeless puncture fix, Amaron/Exide battery jumpstart & replacement, flatbed towing.",
        "dlat": 0.003,
        "dlng": 0.009,
        "address": "Banjara Hills Road No 12",
        "serviceRadius": 25.0,
        "rating": 4.8,
        "reviewCount": 175,
        "completedJobs": 480,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 200,
        "priceRangeMax": 1200,
        "experienceYears": 10,
        "services": ["car_repair_general"]
    },
    {
        "id": "vendor_carp_1",
        "name": "Narasimha Chary",
        "businessName": "MasterCraft Woods & Locksmith",
        "phone": "+919848077889",
        "email": "chary@mastercraft.in",
        "description": "Expert in smart digital door locks, Godrej lock repair, hydraulic bed repair, modular kitchen hinges & custom wardrobes.",
        "dlat": -0.009,
        "dlng": 0.003,
        "address": "Kukatpally Housing Board",
        "serviceRadius": 18.0,
        "rating": 4.7,
        "reviewCount": 94,
        "completedJobs": 260,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 250,
        "priceRangeMax": 900,
        "experienceYears": 12,
        "services": ["carpentry_furniture"]
    },
    {
        "id": "vendor_clean_1",
        "name": "Priya Nair",
        "businessName": "SparkleNest Deep Clean & Pest Shield",
        "phone": "+919848088990",
        "email": "priya@sparklenest.in",
        "description": "Eco-friendly full home deep cleaning, sofa & mattress steam sanitization, odorless herbal cockroach & termite treatment.",
        "dlat": 0.007,
        "dlng": 0.002,
        "address": "Ameerpet Metro Enclave",
        "serviceRadius": 22.0,
        "rating": 4.9,
        "reviewCount": 143,
        "completedJobs": 390,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 499,
        "priceRangeMax": 2499,
        "experienceYears": 6,
        "services": ["cleaning_home"]
    },
    {
        "id": "vendor_tech_1",
        "name": "Vikram Aditya",
        "businessName": "TechPulse Laptop, Mobile & CCTV",
        "phone": "+919848099001",
        "email": "vikram@techpulse.in",
        "description": "Doorstep MacBook/Windows laptop chip-level repair, SSD upgrade, iPhone/Android screen replacement, mesh WiFi & Hikvision CCTV.",
        "dlat": -0.004,
        "dlng": -0.005,
        "address": "Begumpet Tech Park",
        "serviceRadius": 20.0,
        "rating": 4.8,
        "reviewCount": 118,
        "completedJobs": 310,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 300,
        "priceRangeMax": 1500,
        "experienceYears": 8,
        "services": ["laptop_repair"]
    },
    {
        "id": "vendor_salon_1",
        "name": "Ananya Sharma",
        "businessName": "GlowAura Home Salon & Bridal Studio",
        "phone": "+919848010112",
        "email": "ananya@glowaura.in",
        "description": "Luxury salon at home — Korean glass facial, Rica waxing, hair spa, keratin, bridal HD makeup & certified therapeutic spa.",
        "dlat": 0.005,
        "dlng": -0.008,
        "address": "Jubilee Hills Road 36",
        "serviceRadius": 18.0,
        "rating": 4.9,
        "reviewCount": 186,
        "completedJobs": 440,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 399,
        "priceRangeMax": 2500,
        "experienceYears": 9,
        "services": ["salon_haircut"]
    },
    {
        "id": "vendor_paint_1",
        "name": "Ramesh Babu",
        "businessName": "ColorCraft Painters & Waterproofing",
        "phone": "+919848012131",
        "email": "ramesh@colorcraft.in",
        "description": "Asian Paints trained contractor. Dust-free mechanized wall painting, Dr. Fixit terrace waterproofing, POP ceiling & tile grouting.",
        "dlat": -0.012,
        "dlng": 0.009,
        "address": "Miyapur Main Road",
        "serviceRadius": 25.0,
        "rating": 4.7,
        "reviewCount": 76,
        "completedJobs": 185,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 800,
        "priceRangeMax": 12000,
        "experienceYears": 15,
        "services": ["painting_interior"]
    },
    {
        "id": "vendor_health_1",
        "name": "Dr. Kavitha Rao",
        "businessName": "CareFirst Home Physio & Doctor Visit",
        "phone": "+919848014151",
        "email": "kavitha@carefirst.in",
        "description": "Certified sports & orthopedic physiotherapist, general physician home visits, post-surgery rehab & 24/7 home nursing care.",
        "dlat": 0.008,
        "dlng": -0.003,
        "address": "Somajiguda Circle",
        "serviceRadius": 20.0,
        "rating": 4.9,
        "reviewCount": 134,
        "completedJobs": 320,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 450,
        "priceRangeMax": 1200,
        "experienceYears": 10,
        "services": ["doctor_visit"]
    },
    {
        "id": "vendor_move_1",
        "name": "Suresh Yadav",
        "businessName": "SwiftShift Packers & Tempo Express",
        "phone": "+919848016171",
        "email": "suresh@swiftshift.in",
        "description": "3-layer bubble-wrap home shifting, AC uninstallation & reinstallation during move, Tata Ace mini-truck & local furniture delivery.",
        "dlat": -0.006,
        "dlng": 0.011,
        "address": "Secunderabad Paradise",
        "serviceRadius": 30.0,
        "rating": 4.8,
        "reviewCount": 105,
        "completedJobs": 275,
        "isAvailable": True,
        "isVerified": True,
        "priceRangeMin": 800,
        "priceRangeMax": 6500,
        "experienceYears": 9,
        "services": ["packers_movers"]
    }
]


class DatabaseManager:
    def __init__(self, db_path: str = DB_PATH):
        self.db_path = db_path
        self._init_db()
        self._seed_defaults()

    def _conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self):
        with self._conn() as conn:
            conn.executescript("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'consumer',
                    created_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS vendors (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    business_name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT DEFAULT '',
                    description TEXT DEFAULT '',
                    profile_image_url TEXT DEFAULT '',
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    dlat REAL DEFAULT 0.0,
                    dlng REAL DEFAULT 0.0,
                    address TEXT NOT NULL,
                    service_radius REAL DEFAULT 15.0,
                    rating REAL DEFAULT 4.8,
                    review_count INTEGER DEFAULT 10,
                    completed_jobs INTEGER DEFAULT 25,
                    is_available INTEGER DEFAULT 1,
                    is_verified INTEGER DEFAULT 1,
                    price_range_min INTEGER DEFAULT 200,
                    price_range_max INTEGER DEFAULT 1000,
                    experience_years INTEGER DEFAULT 5,
                    services_json TEXT DEFAULT '[]',
                    created_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS bookings (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    user_name TEXT DEFAULT 'Customer',
                    user_phone TEXT DEFAULT '',
                    provider_id TEXT NOT NULL,
                    provider_name TEXT DEFAULT '',
                    service_id TEXT NOT NULL,
                    service_name TEXT DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    scheduled_at INTEGER NOT NULL,
                    address TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    notes TEXT DEFAULT '',
                    urgency TEXT DEFAULT 'normal',
                    estimated_cost INTEGER DEFAULT 0,
                    quoted_cost INTEGER DEFAULT 0,
                    actual_cost INTEGER DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    booking_id TEXT NOT NULL,
                    sender_id TEXT NOT NULL,
                    sender_type TEXT NOT NULL,
                    sender_name TEXT DEFAULT '',
                    content TEXT NOT NULL,
                    message_type TEXT DEFAULT 'text',
                    metadata_json TEXT DEFAULT '{}',
                    is_read INTEGER DEFAULT 0,
                    created_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS reviews (
                    id TEXT PRIMARY KEY,
                    provider_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    booking_id TEXT NOT NULL,
                    rating REAL NOT NULL,
                    comment TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS chat_sessions (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL DEFAULT 'local_user',
                    title TEXT NOT NULL,
                    context_json TEXT DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS chat_messages (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    role TEXT NOT NULL,
                    text TEXT NOT NULL,
                    payload_json TEXT DEFAULT '{}',
                    timestamp INTEGER NOT NULL
                );
            """)

    def _seed_defaults(self):
        base_lat, base_lng = 17.3850, 78.4867
        now_ms = int(time.time() * 1000)
        with self._conn() as conn:
            count = conn.execute("SELECT COUNT(*) FROM vendors").fetchone()[0]
            if count == 0:
                for v in SEED_VENDORS:
                    conn.execute("""
                        INSERT OR REPLACE INTO vendors (
                            id, name, business_name, phone, email, description,
                            latitude, longitude, dlat, dlng, address, service_radius,
                            rating, review_count, completed_jobs, is_available, is_verified,
                            price_range_min, price_range_max, experience_years, services_json, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        v["id"], v["name"], v["businessName"], v["phone"], v["email"],
                        v["description"], base_lat + v["dlat"], base_lng + v["dlng"],
                        v["dlat"], v["dlng"], v["address"], v["serviceRadius"],
                        v["rating"], v["reviewCount"], v["completedJobs"],
                        1 if v["isAvailable"] else 0, 1 if v["isVerified"] else 0,
                        v["priceRangeMin"], v["priceRangeMax"], v["experienceYears"],
                        json.dumps(v["services"]), now_ms
                    ))

            # Seed sample reviews if empty
            rev_count = conn.execute("SELECT COUNT(*) FROM reviews").fetchone()[0]
            if rev_count == 0:
                sample_reviews = [
                    ("rev_1", "vendor_ac_1", "user_1", "bk_demo_1", 5.0, "Arrived in 15 mins and fixed our split AC gas leak quickly. Very transparent pricing!", now_ms - 86400000 * 3),
                    ("rev_2", "vendor_ac_1", "user_2", "bk_demo_2", 4.9, "Super professional jet service, cooling is like brand new now.", now_ms - 86400000 * 1),
                    ("rev_3", "vendor_plumb_1", "user_3", "bk_demo_3", 4.8, "Fixed concealed bathroom pipe leak without breaking extra tiles. Highly recommended!", now_ms - 86400000 * 2),
                    ("rev_4", "vendor_elec_1", "user_4", "bk_demo_4", 5.0, "Solved a midnight MCB short-circuit in 20 minutes. Lifesaver!", now_ms - 86400000 * 4),
                ]
                conn.executemany(
                    "INSERT OR REPLACE INTO reviews (id, provider_id, user_id, booking_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    sample_reviews
                )

    def get_vendors(self, user_lat: Optional[float] = None, user_lng: Optional[float] = None) -> List[Dict[str, Any]]:
        with self._conn() as conn:
            rows = conn.execute("SELECT * FROM vendors ORDER BY rating DESC, completed_jobs DESC").fetchall()

        vendors = []
        for r in rows:
            v = dict(r)
            lat = v["latitude"]
            lng = v["longitude"]
            # If user is in a different region/city (or emulator GPS), dynamically project seed vendors around user's active area
            if user_lat is not None and user_lng is not None and (v["dlat"] != 0.0 or v["dlng"] != 0.0):
                raw_dist = haversine_m(user_lat, user_lng, lat, lng)
                if raw_dist > 25000:  # > 25 km away -> anchor relative to user location for seamless local testing
                    lat = round(user_lat + v["dlat"], 6)
                    lng = round(user_lng + v["dlng"], 6)

            dist_m = haversine_m(user_lat, user_lng, lat, lng) if (user_lat is not None and user_lng is not None) else 1200.0
            eta_min = max(5, int((dist_m / 1000.0) / 28.0 * 60.0) + 4)
            services = json.loads(v.get("services_json") or "[]")
            vendors.append({
                "id": v["id"],
                "name": v["name"],
                "businessName": v["business_name"],
                "phone": v["phone"],
                "email": v["email"],
                "description": v["description"],
                "profileImageUrl": v["profile_image_url"] or "",
                "latitude": lat,
                "longitude": lng,
                "latLng": {"lat": lat, "lng": lng},
                "address": v["address"],
                "serviceRadius": v["service_radius"],
                "rating": float(v["rating"]),
                "reviewCount": int(v["review_count"]),
                "completedJobs": int(v["completed_jobs"]),
                "isAvailable": bool(v["is_available"]),
                "isVerified": bool(v["is_verified"]),
                "priceRangeMin": int(v["price_range_min"]),
                "priceRangeMax": int(v["price_range_max"]),
                "experienceYears": int(v["experience_years"]),
                "distanceMeters": round(dist_m, 1),
                "estimatedArrivalMinutes": eta_min,
                "services": services
            })
        return vendors

    def get_vendor_by_id(self, vendor_id: str, user_lat: Optional[float] = None, user_lng: Optional[float] = None) -> Optional[Dict[str, Any]]:
        all_v = self.get_vendors(user_lat, user_lng)
        for v in all_v:
            if v["id"] == vendor_id:
                return v
        return None

    async def save_vendor(self, data: Dict[str, Any]) -> Dict[str, Any]:
        vendor_id = data.get("id") or f"vendor_{uuid.uuid4().hex[:10]}"
        now_ms = int(time.time() * 1000)
        services = data.get("services") or []
        with self._conn() as conn:
            conn.execute("""
                INSERT OR REPLACE INTO vendors (
                    id, name, business_name, phone, email, description,
                    profile_image_url, latitude, longitude, dlat, dlng, address,
                    service_radius, rating, review_count, completed_jobs,
                    is_available, is_verified, price_range_min, price_range_max,
                    experience_years, services_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                vendor_id,
                data.get("name", ""),
                data.get("businessName") or data.get("name", ""),
                data.get("phone", ""),
                data.get("email", ""),
                data.get("description", ""),
                data.get("profileImageUrl", ""),
                float(data.get("latitude", 17.3850)),
                float(data.get("longitude", 78.4867)),
                0.0,
                0.0,
                data.get("address", ""),
                float(data.get("serviceRadius", 15.0)),
                float(data.get("rating", 4.8)),
                int(data.get("reviewCount", 1)),
                int(data.get("completedJobs", 0)),
                1 if data.get("isAvailable", True) else 0,
                1 if data.get("isVerified", True) else 0,
                int(data.get("priceRangeMin", 250)),
                int(data.get("priceRangeMax", 1000)),
                int(data.get("experienceYears", 3)),
                json.dumps(services),
                now_ms
            ))

        # Best-effort async sync to Supabase
        if SUPABASE_URL and SUPABASE_KEY:
            try:
                async with httpx.AsyncClient(timeout=6.0) as client:
                    headers = {
                        "apikey": SUPABASE_KEY,
                        "Authorization": f"Bearer {SUPABASE_KEY}",
                        "Content-Type": "application/json",
                        "Prefer": "return=representation,resolution=merge-duplicates"
                    }
                    sb_payload = {
                        "id": vendor_id,
                        "name": data.get("name", ""),
                        "business_name": data.get("businessName") or data.get("name", ""),
                        "phone": data.get("phone", ""),
                        "email": data.get("email", ""),
                        "description": data.get("description", ""),
                        "latitude": float(data.get("latitude", 17.3850)),
                        "longitude": float(data.get("longitude", 78.4867)),
                        "address": data.get("address", ""),
                        "service_radius": float(data.get("serviceRadius", 15.0)),
                        "rating": float(data.get("rating", 4.8)),
                        "is_available": bool(data.get("isAvailable", True)),
                        "is_verified": True,
                        "experience_years": int(data.get("experienceYears", 3)),
                        "created_at": now_ms
                    }
                    await client.post(f"{SUPABASE_URL}/rest/v1/providers", headers=headers, json=sb_payload)
            except Exception:
                pass

        return self.get_vendor_by_id(vendor_id) or {"id": vendor_id}

    def update_vendor_availability(self, vendor_id: str, is_available: bool) -> Optional[Dict[str, Any]]:
        with self._conn() as conn:
            conn.execute("UPDATE vendors SET is_available = ? WHERE id = ?", (1 if is_available else 0, vendor_id))
        return self.get_vendor_by_id(vendor_id)

    # ── Bookings & Vendor–User Interaction ─────────────────────────────────
    def create_booking(self, data: Dict[str, Any]) -> Dict[str, Any]:
        booking_id = data.get("id") or f"bk_{uuid.uuid4().hex[:10]}"
        now_ms = int(time.time() * 1000)
        provider_id = data.get("providerId", "vendor_ac_1")
        service_id = data.get("serviceId", "ac_repair")

        vendor = self.get_vendor_by_id(provider_id)
        provider_name = vendor["businessName"] if vendor else data.get("providerName", "Verified Vendor")
        svc = next((s for s in SERVICES_CATALOG if s["id"] == service_id), None)
        service_name = svc["name"] if svc else data.get("serviceName", service_id.replace("_", " ").title())
        est_cost = int(data.get("estimatedCost") or (svc["labour_min"] if svc else 350))

        with self._conn() as conn:
            conn.execute("""
                INSERT OR REPLACE INTO bookings (
                    id, user_id, user_name, user_phone, provider_id, provider_name,
                    service_id, service_name, status, scheduled_at, address,
                    latitude, longitude, notes, urgency, estimated_cost,
                    quoted_cost, actual_cost, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                booking_id,
                data.get("userId", "local_user"),
                data.get("userName", "SmartMaps User"),
                data.get("userPhone", ""),
                provider_id,
                provider_name,
                service_id,
                service_name,
                data.get("status", "PENDING"),
                int(data.get("scheduledAt") or (now_ms + 3600 * 1000)),
                data.get("address", "Current Location"),
                float(data.get("latitude", 17.3850)),
                float(data.get("longitude", 78.4867)),
                data.get("notes", ""),
                data.get("urgency", "normal"),
                est_cost,
                int(data.get("quotedCost", est_cost)),
                int(data.get("actualCost", 0)),
                now_ms,
                now_ms
            ))

        # Automatically create an initial welcome message from the vendor in the booking chat!
        initial_msg = (
            f"👋 Namaste! Thank you for reaching out to {provider_name} for {service_name}. "
            f"Estimated inspection & service charge starts around ₹{est_cost}. "
            f"Share any photos or specific details here — I'm reviewing your request now!"
        )
        self.add_booking_message(
            booking_id=booking_id,
            sender_id=provider_id,
            sender_type="provider",
            sender_name=provider_name,
            content=initial_msg,
            message_type="text",
            auto_reply=False
        )
        return self.get_booking(booking_id)

    def get_bookings(self, user_id: Optional[str] = None, provider_id: Optional[str] = None) -> List[Dict[str, Any]]:
        with self._conn() as conn:
            if provider_id:
                rows = conn.execute("SELECT * FROM bookings WHERE provider_id = ? ORDER BY created_at DESC", (provider_id,)).fetchall()
            elif user_id:
                rows = conn.execute("SELECT * FROM bookings WHERE user_id = ? ORDER BY created_at DESC", (user_id,)).fetchall()
            else:
                rows = conn.execute("SELECT * FROM bookings ORDER BY created_at DESC").fetchall()
        return [self._row_to_booking(r) for r in rows]

    def get_booking(self, booking_id: str) -> Optional[Dict[str, Any]]:
        with self._conn() as conn:
            r = conn.execute("SELECT * FROM bookings WHERE id = ?", (booking_id,)).fetchone()
        return self._row_to_booking(r) if r else None

    def update_booking_status(self, booking_id: str, status: str, quoted_cost: Optional[int] = None) -> Optional[Dict[str, Any]]:
        now_ms = int(time.time() * 1000)
        with self._conn() as conn:
            if quoted_cost is not None and quoted_cost > 0:
                conn.execute(
                    "UPDATE bookings SET status = ?, quoted_cost = ?, estimated_cost = ?, updated_at = ? WHERE id = ?",
                    (status, quoted_cost, quoted_cost, now_ms, booking_id)
                )
            else:
                conn.execute(
                    "UPDATE bookings SET status = ?, updated_at = ? WHERE id = ?",
                    (status, now_ms, booking_id)
                )
            if status == "COMPLETED":
                b = conn.execute("SELECT provider_id FROM bookings WHERE id = ?", (booking_id,)).fetchone()
                if b:
                    conn.execute("UPDATE vendors SET completed_jobs = completed_jobs + 1 WHERE id = ?", (b["provider_id"],))

        booking = self.get_booking(booking_id)
        if booking:
            status_msgs = {
                "ACCEPTED": f"✅ Booking Accepted by {booking['providerName']}! I will arrive at {booking['address']} as scheduled.",
                "ON_THE_WAY": f"🛵 {booking['providerName']} is on the way to your location! Live tracking is active.",
                "ARRIVED": f"📍 I have arrived at {booking['address']}. Please share floor/door details if needed.",
                "IN_PROGRESS": f"🔧 Service work for {booking['serviceName']} has started.",
                "COMPLETED": f"🎉 Job completed! Total service charge: ₹{booking['estimatedCost']}. Thank you for choosing {booking['providerName']}!",
                "REJECTED": "⚠️ Sorry, I am currently tied up with an emergency job and cannot take this slot.",
                "CANCELLED": "❌ This booking was cancelled."
            }
            if status in status_msgs:
                self.add_booking_message(
                    booking_id=booking_id,
                    sender_id=booking["providerId"],
                    sender_type="provider",
                    sender_name=booking["providerName"],
                    content=status_msgs[status],
                    message_type="status_update",
                    auto_reply=False
                )
        return booking

    def _row_to_booking(self, r: sqlite3.Row) -> Dict[str, Any]:
        d = dict(r)
        return {
            "id": d["id"],
            "userId": d["user_id"],
            "userName": d["user_name"],
            "userPhone": d["user_phone"],
            "providerId": d["provider_id"],
            "providerName": d["provider_name"],
            "serviceId": d["service_id"],
            "serviceName": d["service_name"],
            "status": d["status"],
            "scheduledAt": d["scheduled_at"],
            "address": d["address"],
            "latitude": d["latitude"],
            "longitude": d["longitude"],
            "notes": d["notes"],
            "urgency": d["urgency"],
            "estimatedCost": d["estimated_cost"],
            "quotedCost": d["quoted_cost"],
            "actualCost": d["actual_cost"],
            "createdAt": d["created_at"],
            "updatedAt": d["updated_at"]
        }

    # ── Vendor-User Direct Messages ────────────────────────────────────────
    def get_booking_messages(self, booking_id: str) -> List[Dict[str, Any]]:
        with self._conn() as conn:
            rows = conn.execute(
                "SELECT * FROM messages WHERE booking_id = ? ORDER BY created_at ASC",
                (booking_id,)
            ).fetchall()
        return [
            {
                "id": r["id"],
                "bookingId": r["booking_id"],
                "senderId": r["sender_id"],
                "senderType": r["sender_type"],
                "senderName": r["sender_name"],
                "content": r["content"],
                "messageType": r["message_type"],
                "isRead": bool(r["is_read"]),
                "createdAt": r["created_at"]
            }
            for r in rows
        ]

    def add_booking_message(
        self,
        booking_id: str,
        sender_id: str,
        sender_type: str,
        sender_name: str,
        content: str,
        message_type: str = "text",
        auto_reply: bool = True
    ) -> List[Dict[str, Any]]:
        now_ms = int(time.time() * 1000)
        msg_id = f"msg_{uuid.uuid4().hex[:10]}"
        with self._conn() as conn:
            conn.execute("""
                INSERT INTO messages (id, booking_id, sender_id, sender_type, sender_name, content, message_type, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)
            """, (msg_id, booking_id, sender_id, sender_type, sender_name, content, message_type, now_ms))

        # Smart interactive vendor reply when user sends a message (so user-vendor interaction works smoothly in real-time!)
        if auto_reply and sender_type == "user":
            booking = self.get_booking(booking_id)
            provider_id = booking["providerId"] if booking else "vendor_1"
            provider_name = booking["providerName"] if booking else "Service Provider"
            est_cost = booking["estimatedCost"] if booking else 400
            lower = content.lower()

            if any(w in lower for w in ["price", "cost", "charge", "quote", "how much", "rate", "discount"]):
                reply_text = f"💰 For this service, my standard labour charge is ₹{est_cost}. If any spare parts are needed, I'll show you the bill first before installing. Shall I lock in ₹{est_cost}?"
            elif any(w in lower for w in ["when", "time", "arrive", "reach", "late", "where", "eta", "urgent", "fast"]):
                reply_text = f"🛵 I am ~1.5 km away and can reach your location within 15–20 minutes. Sharing my live route on the map!"
            elif any(w in lower for w in ["location", "address", "pin", "map", "landmark"]):
                reply_text = f"📍 Got your location pin! I have opened the route on SmartMaps and will call you if I need the flat/gate number."
            elif any(w in lower for w in ["ok", "confirm", "yes", "sure", "book", "come"]):
                reply_text = f"👍 Confirmed! I've marked your job as priority and packed the required tools. See you shortly!"
                if booking and booking["status"] == "PENDING":
                    self.update_booking_status(booking_id, "ACCEPTED")
            else:
                reply_text = f"Got it! Thanks for the update regarding '{content[:50]}'. I have all the tools ready for {booking['serviceName'] if booking else 'your service'} and will take care of it."

            reply_id = f"msg_{uuid.uuid4().hex[:10]}"
            with self._conn() as conn:
                conn.execute("""
                    INSERT INTO messages (id, booking_id, sender_id, sender_type, sender_name, content, message_type, is_read, created_at)
                    VALUES (?, ?, ?, 'provider', ?, ?, 'text', 0, ?)
                """, (reply_id, booking_id, provider_id, provider_name, reply_text, now_ms + 500))

        return self.get_booking_messages(booking_id)

    # ── Reviews ────────────────────────────────────────────────────────────
    def get_reviews(self, provider_id: str) -> List[Dict[str, Any]]:
        with self._conn() as conn:
            rows = conn.execute("SELECT * FROM reviews WHERE provider_id = ? ORDER BY created_at DESC", (provider_id,)).fetchall()
        return [
            {
                "id": r["id"],
                "providerId": r["provider_id"],
                "userId": r["user_id"],
                "bookingId": r["booking_id"],
                "rating": float(r["rating"]),
                "comment": r["comment"],
                "createdAt": r["created_at"]
            }
            for r in rows
        ]

    def add_review(self, provider_id: str, user_id: str, booking_id: str, rating: float, comment: str) -> Dict[str, Any]:
        now_ms = int(time.time() * 1000)
        rev_id = f"rev_{uuid.uuid4().hex[:10]}"
        with self._conn() as conn:
            conn.execute("""
                INSERT INTO reviews (id, provider_id, user_id, booking_id, rating, comment, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (rev_id, provider_id, user_id, booking_id, rating, comment, now_ms))
            avg_row = conn.execute("SELECT AVG(rating), COUNT(*) FROM reviews WHERE provider_id = ?", (provider_id,)).fetchone()
            if avg_row and avg_row[0] is not None:
                conn.execute("UPDATE vendors SET rating = ?, review_count = ? WHERE id = ?", (round(float(avg_row[0]), 1), int(avg_row[1]), provider_id))
        return {"id": rev_id, "providerId": provider_id, "rating": rating, "comment": comment, "createdAt": now_ms}

    # ── Chat Sessions Persistence ──────────────────────────────────────────
    def get_or_create_session(self, session_id: Optional[str], user_id: str = "local_user") -> Dict[str, Any]:
        now_ms = int(time.time() * 1000)
        with self._conn() as conn:
            if session_id:
                row = conn.execute("SELECT * FROM chat_sessions WHERE id = ?", (session_id,)).fetchone()
                if row:
                    return dict(row)
            sid = session_id or f"sess_{uuid.uuid4().hex[:12]}"
            conn.execute(
                "INSERT OR REPLACE INTO chat_sessions (id, user_id, title, context_json, created_at, updated_at) VALUES (?, ?, ?, '{}', ?, ?)",
                (sid, user_id, "New Conversation", now_ms, now_ms)
            )
            return {"id": sid, "user_id": user_id, "title": "New Conversation", "context_json": "{}", "created_at": now_ms, "updated_at": now_ms}

    def save_chat_turn(self, session_id: str, role: str, text: str, payload: Dict[str, Any] = None):
        now_ms = int(time.time() * 1000)
        msg_id = f"cmsg_{uuid.uuid4().hex[:10]}"
        with self._conn() as conn:
            if role == "user":
                sess = conn.execute("SELECT title FROM chat_sessions WHERE id = ?", (session_id,)).fetchone()
                if sess and sess["title"] == "New Conversation":
                    short_title = text[:32] + ("..." if len(text) > 32 else "")
                    conn.execute("UPDATE chat_sessions SET title = ?, updated_at = ? WHERE id = ?", (short_title, now_ms, session_id))
                else:
                    conn.execute("UPDATE chat_sessions SET updated_at = ? WHERE id = ?", (now_ms, session_id))
            else:
                conn.execute("UPDATE chat_sessions SET updated_at = ? WHERE id = ?", (now_ms, session_id))

            conn.execute(
                "INSERT INTO chat_messages (id, session_id, role, text, payload_json, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
                (msg_id, session_id, role, text, json.dumps(payload or {}), now_ms)
            )

    def get_session_messages(self, session_id: str) -> List[Dict[str, Any]]:
        with self._conn() as conn:
            rows = conn.execute("SELECT * FROM chat_messages WHERE session_id = ? ORDER BY timestamp ASC", (session_id,)).fetchall()
        return [
            {
                "id": r["id"],
                "sessionId": r["session_id"],
                "role": r["role"],
                "text": r["text"],
                "payload": json.loads(r["payload_json"] or "{}"),
                "timestamp": r["timestamp"]
            }
            for r in rows
        ]

    def list_sessions(self, user_id: str = "local_user") -> List[Dict[str, Any]]:
        with self._conn() as conn:
            rows = conn.execute("SELECT * FROM chat_sessions WHERE user_id = ? ORDER BY updated_at DESC", (user_id,)).fetchall()
        return [dict(r) for r in rows]

    # ── Auth (Proxied to Supabase + Local SQLite Fallback) ─────────────────
    async def signup_user(self, username: str, password_hash: str, name: str, phone: str, role: str) -> Dict[str, Any]:
        now_ms = int(time.time() * 1000)
        user_id = str(uuid.uuid4())
        with self._conn() as conn:
            existing = conn.execute("SELECT id FROM users WHERE username = ?", (username,)).fetchone()
            if existing:
                raise ValueError(f'Username "{username}" is already taken')
            conn.execute(
                "INSERT INTO users (id, username, password, name, phone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (user_id, username, password_hash, name, phone, role, now_ms)
            )

        if SUPABASE_URL and SUPABASE_KEY:
            try:
                async with httpx.AsyncClient(timeout=6.0) as client:
                    headers = {
                        "apikey": SUPABASE_KEY,
                        "Authorization": f"Bearer {SUPABASE_KEY}",
                        "Content-Type": "application/json",
                        "Prefer": "return=representation"
                    }
                    await client.post(
                        f"{SUPABASE_URL}/rest/v1/users",
                        headers=headers,
                        json={
                            "id": user_id,
                            "username": username,
                            "password": password_hash,
                            "name": name,
                            "phone": phone,
                            "role": role,
                            "created_at": now_ms
                        }
                    )
            except Exception:
                pass

        return {"userId": user_id, "username": username, "name": name, "phone": phone, "role": role}

    async def login_user(self, username: str, password_hash: str) -> Dict[str, Any]:
        # 1. Check local SQLite first for instant response
        with self._conn() as conn:
            row = conn.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()
            if row:
                if row["password"] != password_hash:
                    raise ValueError("Invalid password")
                return {
                    "userId": row["id"],
                    "username": row["username"],
                    "name": row["name"],
                    "phone": row["phone"],
                    "role": row["role"]
                }

        # 2. Check Supabase if not cached locally
        if SUPABASE_URL and SUPABASE_KEY:
            try:
                async with httpx.AsyncClient(timeout=6.0) as client:
                    headers = {
                        "apikey": SUPABASE_KEY,
                        "Authorization": f"Bearer {SUPABASE_KEY}"
                    }
                    resp = await client.get(
                        f"{SUPABASE_URL}/rest/v1/users",
                        params={"username": f"eq.{username}", "select": "*"},
                        headers=headers
                    )
                    if resp.status_code == 200:
                        users = resp.json()
                        if users:
                            u = users[0]
                            if u.get("password") != password_hash:
                                raise ValueError("Invalid password")
                            with self._conn() as conn:
                                conn.execute(
                                    "INSERT OR REPLACE INTO users (id, username, password, name, phone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                    (u["id"], u["username"], u["password"], u.get("name", ""), u.get("phone", ""), u.get("role", "consumer"), int(time.time() * 1000))
                                )
                            return {
                                "userId": u["id"],
                                "username": u["username"],
                                "name": u.get("name", ""),
                                "phone": u.get("phone", ""),
                                "role": u.get("role", "consumer")
                            }
            except ValueError:
                raise
            except Exception:
                pass

        raise ValueError("Username not found. Please Sign Up first.")


db = DatabaseManager()

