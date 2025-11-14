import os
import socket
import logging
import json
import uuid
from fastapi import FastAPI, HTTPException
from dotenv import load_dotenv
import py_eureka_client.eureka_client as eureka_client
from prometheus_fastapi_instrumentator import Instrumentator

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Plan Service", description="Plan and Airport Service", version="1.0.0")
load_dotenv()

# Initialize Travel Agent
travel_agent = None

def initialize_travel_agent():
    """Initialize the travel agent on startup"""
    global travel_agent
    try:
        from agent.agent import TravelAgent
        travel_agent = TravelAgent()
        logger.info("Travel agent initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize travel agent: {e}")
        travel_agent = None

@app.on_event("startup")
async def startup_events():
    # Initialize travel agent
    initialize_travel_agent()
    
    # Register to Eureka
    ip = socket.gethostbyname(socket.gethostname())
    eureka_server = os.getenv(
        "EUREKA_SERVER_URL",
        "http://localhost:8761/eureka/"
    )
    app_name = os.getenv("APPLICATION_NAME", "python-service")
    instance_port = int(os.getenv("SERVER_PORT", "8001"))
    await eureka_client.init_async(
        eureka_server=eureka_server,
        app_name=app_name,
        instance_port=instance_port,
        instance_host=ip
    )


@app.get("/")
def root():
    return {"message": "Plan Service is running", "service": "plan-service", "version": "1.0.0"}

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "plan-service"}

@app.get("/hello")
def read_hello():
    return {"message": "hello from plan service"}

@app.get("/plan-statistics")
def get_statistics():
    """Get system statistics including total plans"""
    try:
        from repository.database import get_database
        db = get_database()
        total_plans = db.get_total_plans()
        return {
            "success": True,
            "total_plans": total_plans
        }
    except Exception as e:
        logger.error(f"Statistics error: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to get statistics: {str(e)}")

@app.get("/cities/suggest")
def suggest_cities(q: str = "", limit: int = 10):
    """
    Get city suggestions for auto-complete
    
    Query parameters:
    - q: Query string for city search
    - limit: Maximum number of suggestions (default: 10)
    """
    try:
        from functions.get_iata_code import iata_finder
        
        if not q or len(q.strip()) < 2:
            return {"suggestions": []}
        
        # Get city matcher from IATA finder
        if iata_finder and hasattr(iata_finder, 'city_matcher'):
            suggestions = iata_finder.city_matcher.get_city_suggestions(q.strip(), limit)
            return {"suggestions": suggestions}
        else:
            return {"suggestions": [], "error": "City index not initialized"}
            
    except Exception as e:
        logger.error(f"City suggestion error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.get("/cities/airports")  
def get_city_airports(city: str):
    """
    Get all airports for a specific city
    
    Query parameters:
    - city: City name to search for airports
    """
    try:
        from functions.get_iata_code import iata_finder
        
        if not city:
            raise HTTPException(status_code=400, detail="City parameter is required")
        
        if iata_finder and hasattr(iata_finder, 'city_matcher'):
            airports = iata_finder.city_matcher.get_airports_for_city(city)
            matched_city = iata_finder.city_matcher.find_city_match(city)
            
            return {
                "query": city,
                "matched_city": matched_city,
                "airports": airports,
                "count": len(airports)
            }
        else:
            raise HTTPException(status_code=500, detail="City index not initialized")
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"City airports error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

# Flight ticket endpoints
@app.post("/flights/search")
def search_flights(request: dict):
    """
    Search for best flight tickets
    
    Request body should contain:
    - departure_lat: float
    - departure_lon: float  
    - arrival_city: str
    - outbound_date: str (YYYY-MM-DD)
    - return_date: str (optional, YYYY-MM-DD)
    - sort_criteria: str (optional, default: "score")
    - limit: int (optional, default: 5)
    """
    try:
        from service.get_flight_ticket import search_best_flights
        
        # Extract parameters
        departure_lat = request.get("departure_lat")
        departure_lon = request.get("departure_lon")
        arrival_city = request.get("arrival_city")
        outbound_date = request.get("outbound_date")
        return_date = request.get("return_date")
        sort_criteria = request.get("sort_criteria", "score")
        limit = request.get("limit", 5)
        
        # Validate required parameters
        if not all([departure_lat, departure_lon, arrival_city, outbound_date]):
            raise HTTPException(
                status_code=400, 
                detail="Missing required parameters: departure_lat, departure_lon, arrival_city, outbound_date"
            )
        
        # Search flights
        results = search_best_flights(
            departure_lat=float(departure_lat),
            departure_lon=float(departure_lon),
            arrival_city=arrival_city,
            outbound_date=outbound_date,
            return_date=return_date,
            sort_criteria=sort_criteria,
            limit=int(limit)
        )
        
        if not results.get("success"):
            raise HTTPException(status_code=400, detail=results.get("error", "Flight search failed"))
        
        return results
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Flight search error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

# Hotel search endpoints
@app.post("/hotels/search")
def search_hotels(request: dict):
    """
    Search for best hotels
    
    Request body should contain:
    - location: str (city, address, etc.)
    - check_in_date: str (YYYY-MM-DD)
    - check_out_date: str (YYYY-MM-DD)
    - adults: int (optional, default: 2)
    - children: int (optional, default: 0)
    - limit: int (optional, default: 5)
    """
    try:
        from service.get_hotel_info import search_best_hotels
        
        # Extract parameters
        location = request.get("location")
        check_in_date = request.get("check_in_date")
        check_out_date = request.get("check_out_date")
        adults = request.get("adults", 2)
        children = request.get("children", 0)
        limit = request.get("limit", 5)
        
        # Validate required parameters
        if not all([location, check_in_date, check_out_date]):
            raise HTTPException(
                status_code=400,
                detail="Missing required parameters: location, check_in_date, check_out_date"
            )
        
        # Search hotels
        results = search_best_hotels(
            location=location,
            check_in_date=check_in_date,
            check_out_date=check_out_date,
            adults=int(adults),
            children=int(children),
            limit=int(limit)
        )
        
        if not results.get("success"):
            raise HTTPException(status_code=400, detail=results.get("error", "Hotel search failed"))
        
        return results
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Hotel search error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

# Travel Agent endpoints
@app.post("/agent/chat")
def chat_with_agent(request: dict):
    """
    Chat with travel agent
    
    Request body should contain:
    - query: str (user question/request)
    - session_id: str (optional, for conversation history)
    """
    try:
        if not travel_agent:
            raise HTTPException(status_code=503, detail="Travel agent is not available")
        
        query = request.get("query")
        session_id = request.get("session_id")
        
        if not query:
            raise HTTPException(status_code=400, detail="Query is required")
        
        result = travel_agent.process_query(query, session_id)
        
        if not result.get("success"):
            raise HTTPException(status_code=400, detail=result.get("error", "Agent processing failed"))
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Agent chat error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.post("/agent/places")
def search_places_agent(request: dict):
    """
    Search places using travel agent
    
    Request body should contain:
    - city: str (city name)
    - categories: str (optional, comma-separated categories)
    """
    try:
        if not travel_agent:
            raise HTTPException(status_code=503, detail="Travel agent is not available")
        
        city = request.get("city")
        categories = request.get("categories", "")
        
        if not city:
            raise HTTPException(status_code=400, detail="City is required")
        
        result = travel_agent.search_places(city, categories)
        
        if not result.get("success"):
            raise HTTPException(status_code=400, detail=result.get("error", "Places search failed"))
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Agent places search error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.post("/agent/itinerary")
def create_itinerary_agent(request: dict):
    """
    Create travel itinerary using travel agent
    
    Request body should contain:
    - city: str (city name)
    - days: int (number of days)
    - interests: str (optional, travel interests)
    - budget: str (optional, budget information)
    - group_size: int (optional, number of people, default: 1)
    """
    try:
        if not travel_agent:
            raise HTTPException(status_code=503, detail="Travel agent is not available")
        
        city = request.get("city")
        days = request.get("days")
        interests = request.get("interests")
        budget = request.get("budget")
        group_size = request.get("group_size", 1)
        
        if not city or not days:
            raise HTTPException(status_code=400, detail="City and days are required")
        
        result = travel_agent.create_itinerary(
            city=city,
            days=int(days),
            interests=interests,
            budget=budget,
            group_size=int(group_size)
        )
        
        if not result.get("success"):
            raise HTTPException(status_code=400, detail=result.get("error", "Itinerary creation failed"))
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Agent itinerary creation error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.get("/agent/status")
def get_agent_status():
    """Get travel agent status"""
    return {
        "agent_available": travel_agent is not None,
        "endpoints": [
            "/agent/chat - Chat with travel agent",
            "/agent/places - Search places in city", 
            "/agent/itinerary - Create travel itinerary",
            "/agent/status - Get agent status"
        ]
    }

# Plan Management endpoints
@app.post("/plans/save")
def save_plan(request: dict):
    """
    Save a travel plan to database
    
    Request body should contain:
    - user_id: str (user identifier)
    - plan_id: str (plan identifier)
    - plan_content: dict (plan data in JSON format)
    """
    try:
        from repository.database import get_database
        
        user_id = request.get("user_id")
        plan_id = request.get("plan_id")
        plan_content = request.get("plan_content")
        
        if not user_id or not plan_id or not plan_content:
            raise HTTPException(
                status_code=400, 
                detail="Missing required parameters: user_id, plan_id, plan_content"
            )
        
        # Add plan_id to plan_content for easier retrieval
        plan_data = {
            "plan_id": plan_id,
            **plan_content
        }
        
        db = get_database()
        saved_id = db.save_itinerary(user_id, plan_data)
        
        return {
            "success": True,
            "message": "Plan saved successfully",
            "plan_id": plan_id,
            "saved_id": saved_id
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Save plan error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.get("/plans/{user_id}/{plan_id}")
def get_plan(user_id: str, plan_id: str):
    """
    Get a specific plan by user_id and plan_id
    
    Path parameters:
    - user_id: str (user identifier)
    - plan_id: str (plan identifier)
    """
    try:
        from repository.database import get_database
        
        db = get_database()
        
        # Get all user itineraries and find the one with matching plan_id
        user_itineraries = db.get_user_itineraries(user_id, limit=100)  # Get more to find specific plan
        
        for itinerary in user_itineraries:
            if itinerary.get("itinerary_data", {}).get("plan_id") == plan_id:
                return {
                    "success": True,
                    "user_id": user_id,
                    "plan_id": plan_id,
                    "plan_content": itinerary["itinerary_data"],
                    "created_at": itinerary.get("created_at"),
                    "updated_at": itinerary.get("updated_at")
                }
        
        # Plan not found
        raise HTTPException(status_code=404, detail=f"Plan not found for user {user_id} with plan_id {plan_id}")
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get plan error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.get("/plans/{user_id}")
def get_user_plans(user_id: str, limit: int = 10):
    """
    Get all plans for a specific user
    
    Path parameters:
    - user_id: str (user identifier)
    
    Query parameters:
    - limit: int (optional, default: 10, max: 50)
    """
    try:
        from repository.database import get_database
        
        if limit > 50:
            limit = 50  # Cap at 50
        
        db = get_database()
        user_itineraries = db.get_user_itineraries(user_id, limit=limit)
        
        # Convert to plan format
        plans = []
        for itinerary in user_itineraries:
            itinerary_data = itinerary.get("itinerary_data", {})
            if "plan_id" in itinerary_data:  # Only include items that have plan_id
                plans.append({
                    "plan_id": itinerary_data["plan_id"],
                    "plan_content": itinerary_data,
                    "created_at": itinerary.get("created_at"),
                    "updated_at": itinerary.get("updated_at")
                })
        
        return {
            "success": True,
            "user_id": user_id,
            "total_plans": len(plans),
            "plans": plans
        }
        
    except Exception as e:
        logger.error(f"Get user plans error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.delete("/plans/{user_id}/{plan_id}")
def delete_plan(user_id: str, plan_id: str):
    """
    Delete a specific plan by user_id and plan_id
    
    Path parameters:
    - user_id: str (user identifier)
    - plan_id: str (plan identifier)
    """
    try:
        from repository.database import get_database
        
        db = get_database()
        
        # Find the itinerary with matching plan_id and delete it
        user_itineraries = db.get_user_itineraries(user_id, limit=100)
        
        for itinerary in user_itineraries:
            if itinerary.get("itinerary_data", {}).get("plan_id") == plan_id:
                itinerary_id = itinerary["_id"]
                success = db.delete_itinerary(itinerary_id, user_id)
                
                if success:
                    return {
                        "success": True,
                        "message": "Plan deleted successfully",
                        "user_id": user_id,
                        "plan_id": plan_id
                    }
                else:
                    raise HTTPException(status_code=500, detail="Failed to delete plan")
        
        # Plan not found
        raise HTTPException(status_code=404, detail=f"Plan not found for user {user_id} with plan_id {plan_id}")
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Delete plan error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

Instrumentator().instrument(app).expose(app)
