import os
import socket
import logging
from fastapi import FastAPI, HTTPException
from dotenv import load_dotenv
import py_eureka_client.eureka_client as eureka_client
from prometheus_fastapi_instrumentator import Instrumentator

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Plan Service", description="Plan and Airport Service", version="1.0.0")
load_dotenv()

@app.on_event("startup")
async def register_to_eureka():
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

Instrumentator().instrument(app).expose(app)
