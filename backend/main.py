import os
import time
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import httpx
from dotenv import load_dotenv

from database import db, SERVICES_CATALOG
from search_engine import search_engine
from adk_agent import orchestrator

load_dotenv()

app = FastAPI(
    title="SmartMaps Agentic Platform Backend",
    description="Google ADK Agentic Chatbot + Beast Hybrid Geo-Search + Vendor-User Platform",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─────────────────────────────────────────────────────────────────────────────
# Request / Response Models
# ─────────────────────────────────────────────────────────────────────────────

class ChatRequest(BaseModel):
    sessionId: Optional[str] = None
    userId: str = "local_user"
    message: str
    latitude: float = 17.3850
    longitude: float = 78.4867


class SearchRequest(BaseModel):
    query: str = ""
    category: Optional[str] = None
    tab: str = "ALL"  # "ALL" | "SERVICES" | "PLACES"
    latitude: float = 17.3850
    longitude: float = 78.4867
    radiusKm: float = 10.0
    sortByDistance: bool = False
    urgency: str = "normal"
    limit: int = 30


class VendorCreateRequest(BaseModel):
    id: Optional[str] = None
    name: str
    businessName: Optional[str] = None
    phone: str
    email: str = ""
    description: str = ""
    latitude: float = 17.3850
    longitude: float = 78.4867
    address: str = ""
    serviceRadius: float = 15.0
    experienceYears: int = 3
    priceRangeMin: int = 250
    priceRangeMax: int = 1000
    isAvailable: bool = True
    services: List[str] = []


class AvailabilityUpdate(BaseModel):
    isAvailable: bool


class BookingCreateRequest(BaseModel):
    id: Optional[str] = None
    userId: str = "local_user"
    userName: str = "SmartMaps User"
    userPhone: str = ""
    providerId: str
    serviceId: str
    address: str
    latitude: float = 17.3850
    longitude: float = 78.4867
    scheduledAt: Optional[int] = None
    notes: str = ""
    urgency: str = "normal"
    estimatedCost: int = 0


class BookingStatusUpdate(BaseModel):
    status: str
    quotedCost: Optional[int] = None


class MessageSendRequest(BaseModel):
    senderId: str = "local_user"
    senderType: str = "user"  # "user" | "provider"
    senderName: str = "Customer"
    content: str
    messageType: str = "text"


class ReviewCreateRequest(BaseModel):
    providerId: str
    userId: str = "local_user"
    bookingId: str
    rating: float
    comment: str


class AuthSignupRequest(BaseModel):
    username: str
    password: str
    name: str
    phone: str
    role: str = "consumer"


class AuthLoginRequest(BaseModel):
    username: str
    password: str


# ─────────────────────────────────────────────────────────────────────────────
# Endpoints
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "engine": "Google ADK + Beast Hybrid Search",
        "timestamp": int(time.time() * 1000)
    }


@app.post("/api/chat")
async def chat_endpoint(req: ChatRequest):
    """
    Google ADK Stateful Agentic Chat Endpoint.
    Maintains session context, runs ADK tools, ranks vendors, and returns map markers.
    """
    result = await orchestrator.process_chat(
        session_id=req.sessionId,
        user_id=req.userId,
        message=req.message,
        latitude=req.latitude,
        longitude=req.longitude
    )
    return result


@app.get("/api/chat/sessions")
async def get_chat_sessions(userId: str = Query("local_user")):
    return {"sessions": db.list_sessions(userId)}


@app.get("/api/chat/sessions/{session_id}")
async def get_chat_session_history(session_id: str):
    return {"sessionId": session_id, "messages": db.get_session_messages(session_id)}


@app.post("/api/search")
async def beast_search_endpoint(req: SearchRequest):
    """
    Beast-Level Unified Search:
    Instant hybrid search across Vendors (AI Smart-Ranked), Services, and Map Places.
    """
    t0 = time.perf_counter()
    q = req.query.strip()
    intent = search_engine.analyze_query(q or (req.category or ""))

    vendors: List[Dict[str, Any]] = []
    places: List[Dict[str, Any]] = []

    if req.tab in ("ALL", "SERVICES"):
        vendors = search_engine.rank_vendors(
            query=q,
            user_lat=req.latitude,
            user_lng=req.longitude,
            service_id=req.category if (req.category and req.category not in ("food", "restaurant", "cafe", "hospital", "atm", "pharmacy")) else intent.get("service"),
            radius_km=req.radiusKm,
            urgency=req.urgency,
            sort_by_distance=req.sortByDistance,
            limit=req.limit
        )

    if req.tab in ("ALL", "PLACES"):
        place_cats = [req.category] if req.category else intent.get("placeCategories", ["restaurant"])
        places = await search_engine.search_places_fast(
            query=q or (req.category or "restaurant"),
            user_lat=req.latitude,
            user_lng=req.longitude,
            categories=place_cats,
            radius_m=int(req.radiusKm * 1000),
            limit=req.limit
        )

    elapsed_ms = round((time.perf_counter() - t0) * 1000, 1)
    top_vendor = vendors[0] if vendors else None
    summary = (
        f"AI matched {len(vendors)} verified vendors & {len(places)} places in {elapsed_ms} ms"
        + (f" · Top Pick: {top_vendor['businessName']} ({top_vendor.get('matchScore', 96)}% Match)" if top_vendor else "")
    )

    return {
        "query": req.query,
        "latencyMs": elapsed_ms,
        "analysisText": summary,
        "intent": intent,
        "vendors": vendors,
        "places": places,
        "services": intent.get("matchedServices") or SERVICES_CATALOG[:8]
    }


# ── Vendors / Service Providers ────────────────────────────────────────────

@app.get("/api/vendors")
async def list_vendors(
    lat: float = Query(17.3850),
    lng: float = Query(78.4867),
    serviceId: Optional[str] = Query(None),
    query: str = Query(""),
    radiusKm: float = Query(20.0)
):
    vendors = search_engine.rank_vendors(
        query=query,
        user_lat=lat,
        user_lng=lng,
        service_id=serviceId,
        radius_km=radiusKm,
        limit=50
    )
    return {"vendors": vendors}


@app.get("/api/vendors/{vendor_id}")
async def get_vendor_detail(
    vendor_id: str,
    lat: Optional[float] = Query(17.3850),
    lng: Optional[float] = Query(78.4867)
):
    ranked_all = search_engine.rank_vendors(
        query="",
        user_lat=lat or 17.3850,
        user_lng=lng or 78.4867,
        radius_km=50.0,
        limit=100
    )
    vendor = next((v for v in ranked_all if v["id"] == vendor_id), None)
    if not vendor:
        vendor = db.get_vendor_by_id(vendor_id, lat, lng)
    if not vendor:
        raise HTTPException(status_code=404, detail="Vendor not found")
    vendor.setdefault("latLng", {"lat": vendor.get("latitude", 17.3850), "lng": vendor.get("longitude", 78.4867)})
    vendor.setdefault("matchScore", 96)
    vendor.setdefault("matchReason", "Verified top-rated local specialist")
    vendor.setdefault("estimatedArrivalMinutes", vendor.get("responseTimeMinutes", 15))
    vendor.setdefault("services", [])
    reviews = db.get_reviews(vendor_id)
    vendor["reviews"] = reviews
    return vendor


@app.post("/api/vendors")
async def register_or_update_vendor(req: VendorCreateRequest):
    saved = await db.save_vendor(req.model_dump())
    return saved


@app.patch("/api/vendors/{vendor_id}/availability")
async def toggle_vendor_availability(vendor_id: str, req: AvailabilityUpdate):
    updated = db.update_vendor_availability(vendor_id, req.isAvailable)
    if not updated:
        raise HTTPException(status_code=404, detail="Vendor not found")
    return updated


# ── Bookings & Vendor-User Messaging ───────────────────────────────────────

@app.get("/api/bookings")
async def list_bookings(
    userId: Optional[str] = Query(None),
    providerId: Optional[str] = Query(None)
):
    return {"bookings": db.get_bookings(user_id=userId, provider_id=providerId)}


@app.post("/api/bookings")
async def create_booking(req: BookingCreateRequest):
    booking = db.create_booking(req.model_dump())
    return booking


@app.get("/api/bookings/{booking_id}")
async def get_booking(booking_id: str):
    b = db.get_booking(booking_id)
    if not b:
        raise HTTPException(status_code=404, detail="Booking not found")
    return b


@app.patch("/api/bookings/{booking_id}/status")
async def update_booking_status(booking_id: str, req: BookingStatusUpdate):
    b = db.update_booking_status(booking_id, req.status, req.quotedCost)
    if not b:
        raise HTTPException(status_code=404, detail="Booking not found")
    return b


@app.get("/api/bookings/{booking_id}/messages")
async def get_booking_messages(booking_id: str):
    return {"bookingId": booking_id, "messages": db.get_booking_messages(booking_id)}


@app.post("/api/bookings/{booking_id}/messages")
async def send_booking_message(booking_id: str, req: MessageSendRequest):
    msgs = db.add_booking_message(
        booking_id=booking_id,
        sender_id=req.senderId,
        sender_type=req.senderType,
        sender_name=req.senderName,
        content=req.content,
        message_type=req.messageType,
        auto_reply=(req.senderType == "user")
    )
    return {"bookingId": booking_id, "messages": msgs}


# ── Reviews ────────────────────────────────────────────────────────────────

@app.get("/api/vendors/{vendor_id}/reviews")
async def get_vendor_reviews(vendor_id: str):
    return {"reviews": db.get_reviews(vendor_id)}


@app.post("/api/reviews")
async def submit_review(req: ReviewCreateRequest):
    return db.add_review(req.providerId, req.userId, req.bookingId, req.rating, req.comment)


# ── Auth (Backend-Managed so Android Needs Zero Supabase Keys) ─────────────

@app.post("/api/auth/signup")
async def signup(req: AuthSignupRequest):
    try:
        user = await db.signup_user(req.username, req.password, req.name, req.phone, req.role)
        return user
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/api/auth/login")
async def login(req: AuthLoginRequest):
    try:
        user = await db.login_user(req.username, req.password)
        return user
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))


# ── Geocoding & Map Style Proxy (Keyless for Android) ──────────────────────

@app.get("/api/reverse-geocode")
async def reverse_geocode(lat: float = Query(...), lon: float = Query(...)):
    try:
        async with httpx.AsyncClient(timeout=3.5) as client:
            resp = await client.get(
                "https://nominatim.openstreetmap.org/reverse",
                params={"lat": lat, "lon": lon, "format": "json", "addressdetails": 1},
                headers={"User-Agent": "SmartMaps-Backend/2.0"}
            )
            if resp.status_code == 200:
                return resp.json()
    except Exception:
        pass
    return {
        "display_name": f"Area ({lat:.3f}, {lon:.3f})",
        "address": {"suburb": "SmartMaps Zone", "city": "Local Area"}
    }


@app.get("/api/map/style-url")
async def get_map_style_url():
    """
    Returns map style URL from backend so no MapTiler key is stored in Android.
    """
    maptiler_key = os.getenv("MAPTILER_API_KEY", "")
    if maptiler_key:
        return {"styleUrl": f"https://api.maptiler.com/maps/streets-v2/style.json?key={maptiler_key}"}
    return {"styleUrl": "https://tiles.openfreemap.org/styles/liberty"}


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=False)

