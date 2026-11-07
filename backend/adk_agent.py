import os
import json
from typing import Dict, Any, List, Optional
from dotenv import load_dotenv

from google.adk.agents import Agent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.genai import types

from database import db, SERVICES_CATALOG
from search_engine import search_engine

load_dotenv()

APP_NAME = "smartmaps_vendor_agent"
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")

# Session-scoped context store to track active vendors, map pins, and user intent across turns
_SESSION_CONTEXT: Dict[str, Dict[str, Any]] = {}


# ─────────────────────────────────────────────────────────────────────────────
# Google ADK Tools
# ─────────────────────────────────────────────────────────────────────────────

def find_smart_vendors(
    problem_or_service: str,
    latitude: float = 17.3850,
    longitude: float = 78.4867,
    urgency: str = "normal",
    radius_km: float = 15.0
) -> dict:
    """
    Finds and ranks the best verified local service vendors for a user's problem or service need.

    Args:
        problem_or_service: Description of the problem or service needed (e.g. 'AC not cooling', 'leaking tap', 'electrician').
        latitude: User's current GPS latitude.
        longitude: User's current GPS longitude.
        urgency: Urgency of the request ('low', 'normal', 'high', 'emergency').
        radius_km: Search radius in kilometers.

    Returns:
        Dictionary containing ranked vendors with AI match scores, reasons, ETA, and labour cost estimates.
    """
    intent_info = search_engine.analyze_query(problem_or_service)
    vendors = search_engine.rank_vendors(
        query=problem_or_service,
        user_lat=latitude,
        user_lng=longitude,
        service_id=intent_info.get("service"),
        radius_km=radius_km,
        urgency=urgency,
        limit=5
    )
    return {
        "status": "success",
        "matched_service": intent_info.get("serviceName"),
        "service_id": intent_info.get("service"),
        "estimated_labour_inr": f"₹{intent_info.get('estimatedLabourMin', 250)} - ₹{intent_info.get('estimatedLabourMax', 900)}",
        "vendor_count": len(vendors),
        "top_vendors": [
            {
                "id": v["id"],
                "businessName": v["businessName"],
                "personName": v["name"],
                "matchScore": f"{v['matchScore']}%",
                "matchReason": v["matchReason"],
                "rating": v["rating"],
                "completedJobs": v["completedJobs"],
                "distanceKm": round(v["distanceMeters"] / 1000.0, 2),
                "etaMinutes": v["estimatedArrivalMinutes"],
                "priceRange": f"₹{v['priceRangeMin']} - ₹{v['priceRangeMax']}",
                "phone": v["phone"],
                "address": v["address"]
            }
            for v in vendors
        ]
    }


def estimate_job_cost(service_or_problem: str, urgency: str = "normal") -> dict:
    """
    Estimates labour cost in INR, duration in minutes, and required workers for a home/local service problem.

    Args:
        service_or_problem: The user's issue or requested service.
        urgency: 'low', 'normal', 'high', or 'emergency'.

    Returns:
        Dictionary with cost range in INR, duration, workers required, and advice.
    """
    info = search_engine.analyze_query(service_or_problem)
    surcharge = 150 if urgency == "emergency" else (75 if urgency == "high" else 0)
    min_c = info.get("estimatedLabourMin", 250) + surcharge
    max_c = info.get("estimatedLabourMax", 900) + surcharge
    return {
        "service": info.get("serviceName") or "General Service",
        "urgency": urgency,
        "estimated_labour_min_inr": min_c,
        "estimated_labour_max_inr": max_c,
        "estimated_duration_minutes": f"{info.get('estimatedDurationMin', 30)} - {info.get('estimatedDurationMax', 90)} mins",
        "workers_required": info.get("workersRequired", 1),
        "note": "Inspection & labour estimate in INR. Spare parts (if needed) are billed transparently after on-site check."
    }


def search_map_places_tool(query: str, latitude: float = 17.3850, longitude: float = 78.4867) -> dict:
    """
    Searches for nearby places of interest (hospitals, restaurants, cafes, ATMs, pharmacies, petrol pumps) around the user's map coordinates.

    Args:
        query: Place category or name to search for.
        latitude: User's GPS latitude.
        longitude: User's GPS longitude.

    Returns:
        Dictionary of matching nearby places with distances and coordinates.
    """
    # Synchronous fast local + cached lookup for tool execution
    from search_engine import INSTANT_PLACE_TEMPLATES, haversine_m
    q_low = query.lower()
    matches = []
    for idx, tpl in enumerate(INSTANT_PLACE_TEMPLATES):
        if tpl["categoryKey"] in q_low or tpl["categoryLabel"].lower() in q_low or any(w in tpl["name"].lower() for w in q_low.split()):
            plat = round(latitude + tpl["dlat"], 6)
            plng = round(longitude + tpl["dlng"], 6)
            dist = haversine_m(latitude, longitude, plat, plng)
            matches.append({
                "name": tpl["name"],
                "category": tpl["categoryLabel"],
                "distanceKm": round(dist / 1000.0, 2),
                "rating": tpl["rating"],
                "address": tpl["address"],
                "openingHours": tpl["openingHours"]
            })
    return {"status": "success", "places": matches[:5]}


# ─────────────────────────────────────────────────────────────────────────────
# Google ADK Agent & Runner Initialization
# ─────────────────────────────────────────────────────────────────────────────

SYSTEM_INSTRUCTION = """
You are the SmartMaps AI Vendor Concierge, built with Google ADK (Agent Development Kit).
Your primary mission is connecting users with the best verified local service vendors (plumbers, AC technicians, electricians, mechanics, carpenters, cleaners, etc.) and providing live map context.

Key Guidelines:
1. ALWAYS use `find_smart_vendors` and `estimate_job_cost` when a user describes a home/vehicle/tech/personal service problem or asks for a vendor.
2. Explain clearly WHY you chose the top vendor (mention their AI Match %, rating, completed jobs, ETA in minutes, and transparent INR price estimate).
3. Maintain full conversational context across turns:
   - If the user asks "which one is cheaper?", "who can come fastest?", "tell me more about the first vendor", or "book them", refer directly to the vendors already found in the session.
4. Keep your responses warm, structured, concise (3–6 short bullet points or paragraphs), and action-oriented.
5. Remind the user they can tap "Book Now", "Chat with Vendor", or view the live pins on the embedded Smart Map right below your message.
"""

adk_session_service = InMemorySessionService()

smartmaps_adk_agent = Agent(
    name="smartmaps_vendor_concierge",
    model=GEMINI_MODEL,
    description="SmartMaps AI Concierge that diagnoses service needs, smart-ranks local vendors, estimates costs, and connects users to maps.",
    instruction=SYSTEM_INSTRUCTION,
    tools=[find_smart_vendors, estimate_job_cost, search_map_places_tool]
)

adk_runner = Runner(
    agent=smartmaps_adk_agent,
    app_name=APP_NAME,
    session_service=adk_session_service
)


class SmartMapsAgenticOrchestrator:
    """
    Executes the Google ADK Runner with full session persistence, contextual follow-up resolution,
    smart vendor ranking, and live map marker generation.
    """

    async def process_chat(
        self,
        session_id: Optional[str],
        user_id: str,
        message: str,
        latitude: float,
        longitude: float
    ) -> Dict[str, Any]:
        # 1. Ensure persistent session exists
        sess_row = db.get_or_create_session(session_id, user_id)
        sid = sess_row["id"]
        db.save_chat_turn(sid, "user", message)

        # Load or initialize session context memory
        ctx = _SESSION_CONTEXT.get(sid)
        if not ctx:
            try:
                ctx = json.loads(sess_row.get("context_json") or "{}")
            except Exception:
                ctx = {}
            _SESSION_CONTEXT[sid] = ctx

        history = db.get_session_messages(sid)
        msg_lower = message.strip().lower()

        # 2. Detect if this is a contextual follow-up referencing previous vendors in this session
        prev_vendors: List[Dict[str, Any]] = ctx.get("last_vendors", [])
        prev_intent: Optional[Dict[str, Any]] = ctx.get("last_intent")

        is_followup = bool(prev_vendors) and any(
            phrase in msg_lower for phrase in [
                "cheaper", "cheapest", "lowest price", "cost", "price", "compare",
                "fastest", "closest", "nearest", "eta", "quickest", "urgent",
                "first", "second", "best", "top", "who is better", "why",
                "experience", "experienced", "rating", "reviews", "book", "map", "where are they"
            ]
        )

        if is_followup and prev_intent:
            intent_info = dict(prev_intent)
            if "urgent" in msg_lower or "emergency" in msg_lower or "fast" in msg_lower:
                intent_info["urgency"] = "emergency"
            vendors = search_engine.rank_vendors(
                query=intent_info.get("query", message),
                user_lat=latitude,
                user_lng=longitude,
                service_id=intent_info.get("service"),
                urgency=intent_info.get("urgency", "normal"),
                sort_by_distance=any(w in msg_lower for w in ["closest", "nearest", "fastest", "eta"]),
                limit=5
            )
            places = ctx.get("last_places", [])
        else:
            # Analyze new intent with BeastSearchEngine
            intent_info = search_engine.analyze_query(message)
            vendors = []
            places = []

            if intent_info["intent"] in ("service_request", "hybrid"):
                vendors = search_engine.rank_vendors(
                    query=message,
                    user_lat=latitude,
                    user_lng=longitude,
                    service_id=intent_info.get("service"),
                    urgency=intent_info.get("urgency", "normal"),
                    limit=5
                )

            if intent_info["intent"] == "place_search" or (not vendors and intent_info.get("placeCategories")):
                places = await search_engine.search_places_fast(
                    query=message,
                    user_lat=latitude,
                    user_lng=longitude,
                    categories=intent_info.get("placeCategories"),
                    limit=6
                )

        # Save updated context in session memory
        ctx["last_vendors"] = vendors
        ctx["last_places"] = places
        ctx["last_intent"] = intent_info
        ctx["user_lat"] = latitude
        ctx["user_lng"] = longitude

        # 3. Run Google ADK Agent with session context
        adk_reply_text: Optional[str] = None
        api_key = os.getenv("GOOGLE_API_KEY") or os.getenv("GEMINI_API_KEY") or ""
        # Ensure GOOGLE_API_KEY is set in os.environ for google-adk
        if api_key:
            os.environ["GOOGLE_API_KEY"] = api_key

        if api_key and not api_key.startswith("your_"):
            try:
                # Ensure ADK session exists in InMemorySessionService
                existing_adk_sess = await adk_session_service.get_session(
                    app_name=APP_NAME, user_id=user_id, session_id=sid
                )
                if not existing_adk_sess:
                    await adk_session_service.create_session(
                        app_name=APP_NAME,
                        user_id=user_id,
                        session_id=sid,
                        state={"latitude": latitude, "longitude": longitude}
                    )

                augmented_prompt = (
                    f"[User GPS: lat={latitude:.4f}, lng={longitude:.4f}]\n"
                    f"User message: {message}"
                )
                content = types.Content(
                    role="user",
                    parts=[types.Part.from_text(text=augmented_prompt)]
                )
                collected_parts = []
                async for event in adk_runner.run_async(
                    user_id=user_id,
                    session_id=sid,
                    new_message=content
                ):
                    if event.is_final_response() and event.content and event.content.parts:
                        for part in event.content.parts:
                            if getattr(part, "text", None) and not getattr(part, "thought", False):
                                collected_parts.append(part.text)
                if collected_parts:
                    adk_reply_text = "\n".join(collected_parts).strip()
            except Exception as e:
                # If API key is invalid or network restricted, fall back gracefully to our deterministic ADK synthesizer
                print(f"[ADK Runner Note] Using deterministic ADK tool synthesizer: {e}")

        # 4. If ADK LLM call didn't return text (e.g., key quota/format), synthesize rich contextual response from ADK tool data
        if not adk_reply_text:
            adk_reply_text = self._synthesize_contextual_response(
                message=message,
                msg_lower=msg_lower,
                is_followup=is_followup,
                intent_info=intent_info,
                vendors=vendors,
                places=places,
                history_len=len(history)
            )

        # 5. Build Map Connection Context (pins, camera center, bounds)
        map_markers = []
        for v in vendors[:5]:
            map_markers.append({
                "id": v["id"],
                "type": "vendor",
                "title": v["businessName"],
                "subtitle": f"{v.get('matchScore', 95)}% Match · ₹{v['priceRangeMin']}+ · {v['estimatedArrivalMinutes']}m ETA",
                "lat": v["latitude"],
                "lng": v["longitude"],
                "rating": v["rating"]
            })
        for p in places[:5]:
            map_markers.append({
                "id": p["id"],
                "type": "place",
                "title": p["name"],
                "subtitle": f"{p['categoryLabel']} · {round(p['distanceMeters']/1000.0, 1)} km",
                "lat": p["latLng"]["lat"],
                "lng": p["latLng"]["lng"],
                "rating": p.get("rating", 4.5)
            })

        map_context = {
            "centerLat": latitude,
            "centerLng": longitude,
            "zoom": 14.2,
            "markers": map_markers,
            "hasVendorPins": len(vendors) > 0,
            "hasPlacePins": len(places) > 0
        }

        # 6. Build smart contextual quick-reply chips for the next turn
        quick_replies = self._build_quick_replies(intent_info, vendors, places, is_followup)

        response_payload = {
            "sessionId": sid,
            "reply": adk_reply_text,
            "intentResult": intent_info,
            "suggestedProviders": vendors,
            "suggestedPlaces": places,
            "mapContext": map_context,
            "quickReplies": quick_replies,
            "agentEngine": "Google ADK (smartmaps_vendor_concierge)"
        }

        db.save_chat_turn(sid, "model", adk_reply_text, response_payload)
        return response_payload

    def _synthesize_contextual_response(
        self,
        message: str,
        msg_lower: str,
        is_followup: bool,
        intent_info: Dict[str, Any],
        vendors: List[Dict[str, Any]],
        places: List[Dict[str, Any]],
        history_len: int
    ) -> str:
        # Handle contextual follow-ups first
        if is_followup and vendors:
            if any(w in msg_lower for w in ["cheaper", "cheapest", "price", "cost", "compare"]):
                by_price = sorted(vendors, key=lambda v: v["priceRangeMin"])
                cheapest = by_price[0]
                lines = [
                    f"💰 **Price Comparison for {intent_info.get('serviceName', 'this service')}:**",
                    f"• **Most Affordable:** **{cheapest['businessName']}** ({cheapest['name']}) starts at **₹{cheapest['priceRangeMin']}** ({cheapest['rating']}★, ~{cheapest['estimatedArrivalMinutes']} min ETA)."
                ]
                for v in by_price[1:3]:
                    lines.append(f"• **{v['businessName']}**: ₹{v['priceRangeMin']}–₹{v['priceRangeMax']} ({v.get('matchScore', 92)}% AI Match · {v['completedJobs']} jobs)")
                lines.append("\nTap **Book** or **Chat** on any vendor card below to request an exact quote!")
                return "\n".join(lines)

            if any(w in msg_lower for w in ["fastest", "closest", "nearest", "eta", "urgent", "quickest"]):
                by_dist = sorted(vendors, key=lambda v: v["distanceMeters"])
                fastest = by_dist[0]
                dist_km = fastest["distanceMeters"] / 1000.0
                return (
                    f"🛵 **Fastest Available Vendor Near You:**\n"
                    f"• **{fastest['businessName']}** ({fastest['name']}) is just **{dist_km:.1f} km away** and can arrive in **~{fastest['estimatedArrivalMinutes']} mins**!\n"
                    f"• **AI Match:** {fastest.get('matchScore', 96)}% · **Rating:** {fastest['rating']}★ ({fastest['completedJobs']} completed jobs)\n"
                    f"• **Standard Charge:** ₹{fastest['priceRangeMin']} – ₹{fastest['priceRangeMax']}\n\n"
                    f"I've highlighted their live location on the map below. Tap **Book Now** for priority dispatch!"
                )

            if any(w in msg_lower for w in ["experience", "best", "top", "why", "first", "recommend"]):
                top = vendors[0]
                return (
                    f"🏆 **Why AI Picked {top['businessName']} (#1 Match - {top.get('matchScore', 97)}%):**\n"
                    f"• **Expertise:** {top['experienceYears']} years experience · {top['completedJobs']} verified jobs completed.\n"
                    f"• **Specialty:** {top['description']}\n"
                    f"• **Proximity & Pricing:** {top['distanceMeters']/1000.0:.1f} km away (~{top['estimatedArrivalMinutes']} min ETA) · ₹{top['priceRangeMin']}–₹{top['priceRangeMax']}.\n\n"
                    f"You can message **{top['name']}** directly or book in one tap below!"
                )

        # Primary service / vendor recommendation
        if vendors and intent_info.get("intent") in ("service_request", "hybrid"):
            top = vendors[0]
            svc_name = intent_info.get("serviceName") or "Service Specialist"
            urg_badge = "🚨 **Emergency Priority** · " if intent_info.get("urgency") == "emergency" else ""
            lines = [
                f"{urg_badge}🤖 **AI Smart Vendor Match for {svc_name}:**",
                f"Estimated Labour: **₹{intent_info.get('estimatedLabourMin', 250)} – ₹{intent_info.get('estimatedLabourMax', 900)}** ({intent_info.get('estimatedDurationMin', 30)}–{intent_info.get('estimatedDurationMax', 90)} mins)\n",
                f"⭐ **Top Pick: {top['businessName']} ({top.get('matchScore', 96)}% AI Match)**",
                f"• **Why chosen:** {top.get('matchReason', 'Verified specialist nearby')}",
                f"• **Pricing & ETA:** Starts at **₹{top['priceRangeMin']}** · Arrives in **~{top['estimatedArrivalMinutes']} mins**"
            ]
            if len(vendors) > 1:
                runner_up = vendors[1]
                lines.append(
                    f"\n🔹 **Alternative:** **{runner_up['businessName']}** ({runner_up.get('matchScore', 91)}% Match · "
                    f"{runner_up['distanceMeters']/1000.0:.1f} km · ₹{runner_up['priceRangeMin']}+)"
                )
            lines.append("\n🗺️ I've pinned the top vendors on the **Smart Map** below. Tap **Book**, **Chat**, or **Call** to connect instantly!")
            return "\n".join(lines)

        # Place search response
        if places:
            top_p = places[0]
            lines = [
                f"📍 **Found {len(places)} Nearby Places on SmartMaps:**",
                f"• **Closest:** **{top_p['name']}** ({top_p['categoryLabel']}) — **{top_p['distanceMeters']/1000.0:.1f} km away** ({top_p.get('openingHours', 'Open')})"
            ]
            for p in places[1:3]:
                lines.append(f"• **{p['name']}** — {p['distanceMeters']/1000.0:.1f} km · {p['address']}")
            lines.append("\nTap any pin on the map preview or card below for directions!")
            return "\n".join(lines)

        return (
            "Hi! I'm your **SmartMaps AI Vendor Concierge** (powered by Google ADK). 🛠️🗺️\n\n"
            "Tell me what issue you're facing (e.g., *\"My AC isn't cooling\"*, *\"Leaking pipe in bathroom\"*, *\"Need an electrician urgently\"*, or *\"Car battery dead\"*), "
            "and I will:\n"
            "1. **Smart-match the best verified vendors** near your GPS location\n"
            "2. **Estimate fair labour costs** in ₹ (INR)\n"
            "3. **Pin vendors live on the map** and connect you via instant chat or booking!"
        )

    def _build_quick_replies(
        self,
        intent_info: Dict[str, Any],
        vendors: List[Dict[str, Any]],
        places: List[Dict[str, Any]],
        is_followup: bool
    ) -> List[str]:
        if vendors:
            top_name = vendors[0]["businessName"].split()[0]
            return [
                f"Why is {top_name} #1?",
                "Which vendor is cheapest?",
                "Who can arrive fastest?",
                "Emergency / Urgent visit",
                "Show all on Map"
            ]
        if places:
            return [
                "Need a service vendor nearby",
                "Find AC repair pro",
                "Find 24x7 Plumber",
                "Find Electrician"
            ]
        return [
            "My AC isn't cooling ❄️",
            "Bathroom tap is leaking 🚿",
            "MCB tripping / Electrician ⚡",
            "Car breakdown / Puncture 🚗",
            "Home deep cleaning 🧽"
        ]


orchestrator = SmartMapsAgenticOrchestrator()

