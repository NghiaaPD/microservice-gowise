"""
Flight Ticket Service
Main service for searching and sorting flight tickets
"""

import serpapi
from dotenv import load_dotenv
import os
import json
from typing import Dict, List, Any, Optional, Tuple
import logging
from datetime import datetime, date

# Import our custom modules
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from functions.get_iata_code import get_iata_codes
from utils.sort_flight_ticket import FlightSorter, analyze_flight_data

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

class FlightTicketService:
    """Service for searching and processing flight tickets"""
    
    def __init__(self):
        self.api_key = os.getenv("SERP_API_KEY")
        
        if not self.api_key:
            logger.warning("SERP_API_KEY not found in environment variables")
    
    def get_airport_codes(self, departure_lat: float, departure_lon: float, 
                         arrival_city: str) -> Tuple[Optional[str], Optional[str]]:
        """
        Get IATA codes for departure (from coordinates) and arrival (from city name)
        
        Args:
            departure_lat: Departure latitude
            departure_lon: Departure longitude
            arrival_city: Arrival city name
            
        Returns:
            Tuple of (departure_iata, arrival_iata)
        """
        try:
            result = get_iata_codes(departure_lat, departure_lon, arrival_city)
            
            if result.get('success'):
                departure_iata = result.get('coordinate_iata')
                arrival_iata = result.get('city_iata')
                
                logger.info(f"Found IATA codes: {departure_iata} -> {arrival_iata}")
                return departure_iata, arrival_iata
            else:
                logger.error(f"Failed to get IATA codes: {result.get('error')}")
                return None, None
                
        except Exception as e:
            logger.error(f"Error getting IATA codes: {e}")
            return None, None
    
    def _format_flight(self, flight: Dict[str, Any]) -> Dict[str, Any]:
        """
        Format flight information for better readability
        
        Args:
            flight: Flight dictionary
            
        Returns:
            Formatted flight information
        """
        duration_minutes = flight.get('total_duration', 0)
        duration_hours = round(duration_minutes / 60, 1) if duration_minutes else 0
        duration_display = f"{duration_hours}h ({duration_minutes}min)"
        
        # Extract airline info from flights array if available
        flights_info = flight.get('flights', [])
        airline_info = {}
        if flights_info:
            first_flight = flights_info[0]
            airline_info = {
                "departure_airport": first_flight.get('departure_airport', {}),
                "arrival_airport": first_flight.get('arrival_airport', {}),
                "airplane": first_flight.get('airplane', ''),
                "flight_number": first_flight.get('flight_number', ''),
                "airline": first_flight.get('airline', '')
            }
        
        formatted = {
            "price": f"${flight.get('price', 0)}",
            "price_value": flight.get('price', 0),
            "duration": duration_display,
            "duration_minutes": duration_minutes,
            "duration_hours": duration_hours,
            "score": round(flight.get('calculated_score', 0), 4),
            "carbon_emissions": flight.get('carbon_emissions', {}),
            "airline_logo": flight.get('airline_logo', ''),
            "booking_token": flight.get('booking_token', ''),
            "type": flight.get('type', 'One way'),
            "airline_info": airline_info
        }
        
        return formatted
    
    def search_flights(self, 
                      departure_id: str,
                      arrival_id: str,
                      outbound_date: str,
                      return_date: Optional[str] = None,
                      travel_class: str = "1",
                      currency: str = "USD",
                      sort_by: str = "2") -> Optional[Dict[str, Any]]:
        """
        Search for flights using SerpAPI Google Flights
        
        Args:
            departure_id: IATA code for departure airport
            arrival_id: IATA code for arrival airport  
            outbound_date: Departure date (YYYY-MM-DD)
            return_date: Return date for round trip (YYYY-MM-DD)
            travel_class: Travel class (1=Economy, 2=Premium Economy, 3=Business, 4=First)
            currency: Currency code (USD, EUR, etc.)
            sort_by: Sort method (1=Best, 2=Price, 3=Duration)
            
        Returns:
            Flight search results or None if error
        """
        if not self.api_key:
            logger.error("API key not configured")
            return None
        
        # Build request parameters
        params = {
            "engine": "google_flights",
            "departure_id": departure_id,
            "arrival_id": arrival_id,
            "outbound_date": outbound_date,
            "type": "1" if return_date else "2",  # 1=Round trip, 2=One way
            "travel_class": travel_class,
            "currency": currency,
            "sort_by": sort_by,
            "hl": "en",
            "gl": "us",
            "api_key": self.api_key
        }
        
        if return_date:
            params["return_date"] = return_date
        
        try:
            logger.info(f"Searching flights from {departure_id} to {arrival_id} on {outbound_date}")
            
            # Use SerpAPI
            results = serpapi.search(params)
            
            # Convert to dict if needed
            if hasattr(results, "as_dict"):
                data = results.as_dict()
            else:
                data = results
            
            logger.info(f"Found {len(data.get('other_flights', []))} flights")
            
            return data
            
        except Exception as e:
            logger.error(f"SerpAPI error: {e}")
            return None
    
    def get_best_flight_tickets(self,
                               departure_lat: float,
                               departure_lon: float,
                               arrival_city: str,
                               outbound_date: str,
                               return_date: Optional[str] = None,
                               sort_criteria: str = "score",
                               limit: int = 5,
                               travel_class: str = "1") -> Dict[str, Any]:
        """
        Main function to get best flight tickets
        
        Args:
            departure_lat: Departure latitude
            departure_lon: Departure longitude
            arrival_city: Arrival city name
            outbound_date: Departure date (YYYY-MM-DD)
            return_date: Return date for round trip (YYYY-MM-DD)
            sort_criteria: Sorting criteria ('price', 'duration', 'emissions', 'score')
            limit: Number of results to return
            travel_class: Travel class (1=Economy, 2=Premium Economy, 3=Business, 4=First)
            
        Returns:
            Dictionary containing best flights and analysis
        """
        try:
            # Step 1: Get IATA codes
            departure_iata, arrival_iata = self.get_airport_codes(
                departure_lat, departure_lon, arrival_city
            )
            
            if not departure_iata or not arrival_iata:
                return {
                    "success": False,
                    "error": "Could not find airport codes for the specified locations",
                    "departure_iata": departure_iata,
                    "arrival_iata": arrival_iata
                }
            
            # Step 2: Search flights
            flight_data = self.search_flights(
                departure_id=departure_iata,
                arrival_id=arrival_iata,
                outbound_date=outbound_date,
                return_date=return_date,
                travel_class=travel_class
            )
            
            if not flight_data:
                return {
                    "success": False,
                    "error": "Failed to search flights",
                    "departure_iata": departure_iata,
                    "arrival_iata": arrival_iata
                }
            
            # Step 3: Process and sort flights
            flights = flight_data.get('other_flights', [])
            
            if not flights:
                return {
                    "success": False,
                    "error": "No flights found for the specified route and date",
                    "departure_iata": departure_iata,
                    "arrival_iata": arrival_iata,
                    "raw_data": flight_data
                }
            
            # Always get top flights using optimized score (price + duration)
            # Use limit to control total number of flights returned
            max_flights = min(limit if limit > 1 else 6, len(flights))
            all_best_flights = FlightSorter.get_best_flights(
                flights, sort_by="score", limit=max_flights
            )
            
            # Analyze flight data
            analysis = analyze_flight_data(flight_data)
            
            # Format result according to user requirement
            result = {
                "success": True,
                "departure_iata": departure_iata,
                "arrival_iata": arrival_iata,
                "route": f"{departure_iata} → {arrival_iata}",
                "search_params": {
                    "departure_coordinates": [departure_lat, departure_lon],
                    "arrival_city": arrival_city,
                    "outbound_date": outbound_date,
                    "return_date": return_date,
                    "travel_class": travel_class,
                    "requested_limit": limit
                },
                "total_flights_found": len(flights)
            }
            
            # Add top 1 and top 5 structure
            if all_best_flights:
                # Top 1: Best flight
                result["top_1"] = {
                    "description": "Chuyến bay tốt nhất về giá và thời gian",
                    "optimization": "50% giá vé + 40% thời gian + 10% phát thải",
                    "flight": self._format_flight(all_best_flights[0])
                }
                
                # Top 5: Other best flights based on limit parameter
                if limit == 1:
                    # If limit=1, only return top_1, no top_5
                    result["top_5"] = {
                        "description": "Chỉ hiển thị 1 chuyến bay tốt nhất (limit=1)",
                        "flights": []
                    }
                elif len(all_best_flights) > 1:
                    # Return other flights based on limit
                    max_others = min(limit - 1, len(all_best_flights) - 1, 5)  # Max 5 others
                    other_flights = all_best_flights[1:1+max_others]
                    result["top_5"] = {
                        "description": f"{len(other_flights)} chuyến bay tốt nhất khác",
                        "flights": [self._format_flight(flight) for flight in other_flights]
                    }
                else:
                    result["top_5"] = {
                        "description": "Không có chuyến bay khác",
                        "flights": []
                    }
            else:
                result["top_1"] = None
                result["top_5"] = {"flights": []}
            
            # Add summary info
            result["summary"] = {
                "total_flights_analyzed": len(flights),
                "flights_returned": len(all_best_flights),
                "price_insights": flight_data.get('price_insights', {}),
                "analysis": analysis
            }
            
            return result
            
        except Exception as e:
            logger.error(f"Error in get_best_flight_tickets: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    def save_results_to_file(self, results: Dict[str, Any], filename: str = None) -> str:
        """
        Save flight search results to JSON file
        
        Args:
            results: Flight search results
            filename: Optional filename, will auto-generate if not provided
            
        Returns:
            Filename where results were saved
        """
        try:
            if not filename:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"flight_results_{timestamp}.json"
            
            with open(filename, "w", encoding="utf-8") as f:
                json.dump(results, f, ensure_ascii=False, indent=4)
            
            logger.info(f"Results saved to {filename}")
            return filename
            
        except Exception as e:
            logger.error(f"Error saving results to file: {e}")
            return ""


# Global service instance
flight_service = FlightTicketService()

def search_best_flights(departure_lat: float,
                       departure_lon: float,
                       arrival_city: str,
                       outbound_date: str,
                       return_date: Optional[str] = None,
                       sort_criteria: str = "score",
                       limit: int = 5) -> Dict[str, Any]:
    """
    Convenience function to search for best flights
    
    Args:
        departure_lat: Departure latitude
        departure_lon: Departure longitude
        arrival_city: Arrival city name
        outbound_date: Departure date (YYYY-MM-DD)
        return_date: Return date for round trip (YYYY-MM-DD)
        sort_criteria: Sorting criteria ('price', 'duration', 'emissions', 'score')
        limit: Number of results to return
        
    Returns:
        Dictionary containing best flights and analysis
        
    Example:
        results = search_best_flights(
            departure_lat=35.6762,
            departure_lon=139.6503,
            arrival_city="Seoul",
            outbound_date="2025-10-06",
            sort_criteria="price",
            limit=3
        )
    """
    return flight_service.get_best_flight_tickets(
        departure_lat=departure_lat,
        departure_lon=departure_lon,
        arrival_city=arrival_city,
        outbound_date=outbound_date,
        return_date=return_date,
        sort_criteria=sort_criteria,
        limit=limit
    )


# Example usage and testing
# if __name__ == "__main__":
#     # Test with Tokyo (Narita) coordinates to Seoul
#     results = search_best_flights(
#         departure_lat=35.7649,  # Narita Airport area
#         departure_lon=140.3865,
#         arrival_city="Seoul",
#         outbound_date="2025-10-06",
#         sort_criteria="price",
#         limit=5
#     )
    
#     print("=== FLIGHT SEARCH RESULTS ===")
#     print(f"Success: {results['success']}")
    
#     if results['success']:
#         print(f"Route: {results['departure_iata']} -> {results['arrival_iata']}")
#         print(f"Total flights found: {results['total_flights_found']}")
        
#         print("\n=== BEST FLIGHTS ===")
#         for i, flight in enumerate(results['best_flights'], 1):
#             price = flight.get('price', 'N/A')
#             duration = flight.get('total_duration', 'N/A')
#             score = flight.get('calculated_score', 'N/A')
#             print(f"{i}. Price: ${price}, Duration: {duration}min, Score: {score}")
        
#         # Save results
#         filename = flight_service.save_results_to_file(results)
#         print(f"\nResults saved to: {filename}")
#     else:
#         print(f"Error: {results.get('error')}")