"""
Hotel Information Service
Service for searching and sorting hotels using Google Hotels API via SerpAPI
"""

import serpapi
from dotenv import load_dotenv
import os
import json
from typing import Dict, List, Any, Optional
import logging
from datetime import datetime, date
import sys

# Import our custom modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.sort_hotel_booking import HotelSorter, analyze_hotel_data, get_best_value_hotels

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load environment variables
load_dotenv()

class HotelService:
    """Service for searching and processing hotel information"""
    
    def __init__(self):
        self.api_key = os.getenv("SERP_API_KEY")
        
        if not self.api_key:
            logger.warning("SERP_API_KEY not found in environment variables")
    
    def search_hotels(self,
                     location: str,
                     check_in_date: str,
                     check_out_date: str,
                     adults: int = 2,
                     children: int = 0,
                     currency: str = "USD",
                     sort_by: Optional[str] = None) -> Optional[Dict[str, Any]]:
        """
        Search for hotels using SerpAPI Google Hotels
        
        Args:
            location: Location to search (city, address, etc.)
            check_in_date: Check-in date (YYYY-MM-DD)
            check_out_date: Check-out date (YYYY-MM-DD)
            adults: Number of adults
            children: Number of children
            currency: Currency code (USD, EUR, etc.)
            sort_by: Sort method (optional, None=Default)
            
        Returns:
            Hotel search results or None if error
        """
        if not self.api_key:
            logger.error("API key not configured")
            return None
        
        # Build request parameters
        params = {
            "engine": "google_hotels",
            "q": location,
            "check_in_date": check_in_date,
            "check_out_date": check_out_date,
            "adults": str(adults),
            "children": str(children) if children > 0 else None,
            "currency": currency,
            "hl": "en",
            "gl": "us",
            "api_key": self.api_key
        }
        
        # Add sort_by only if provided
        if sort_by:
            params["sort_by"] = sort_by
        
        # Remove None values
        params = {k: v for k, v in params.items() if v is not None}
        
        try:
            logger.info(f"Searching hotels in {location} for {adults} adults, {children} children")
            logger.info(f"Dates: {check_in_date} to {check_out_date}")
            logger.info(f"Request params: {params}")
            
            # Use SerpAPI
            results = serpapi.search(params)
            
            # Convert to dict if needed
            if hasattr(results, "as_dict"):
                data = results.as_dict()
            else:
                data = results
            
            logger.info(f"Found {len(data.get('properties', []))} hotels")
            
            return data
            
        except Exception as e:
            logger.error(f"SerpAPI error: {e}")
            logger.error(f"Error type: {type(e)}")
            return None
    

    
    def _format_hotel(self, hotel: Dict[str, Any]) -> Dict[str, Any]:
        """
        Format hotel information for better readability
        
        Args:
            hotel: Hotel dictionary
            
        Returns:
            Formatted hotel information
        """
        # Extract price information
        rate_info = hotel.get('rate_per_night', {})
        price_str = rate_info.get('extracted_lowest', '0')
        if isinstance(price_str, str):
            price_value = float(''.join(filter(str.isdigit, price_str))) or 0
        else:
            price_value = float(price_str) if price_str else 0
        
        # Extract images
        images = hotel.get('images', [])
        main_image = images[0] if images else ""
        
        # Extract amenities
        amenities = hotel.get('amenities', [])
        
        formatted = {
            "name": hotel.get('name', ''),
            "type": hotel.get('type', ''),
            "price": f"${price_value:.0f}",
            "price_value": price_value,
            "currency": rate_info.get('currency', 'USD'),
            "rating": hotel.get('overall_rating', 0),
            "rating_count": hotel.get('reviews', 0),
            "distance": hotel.get('distance', ''),
            "neighborhood": hotel.get('neighborhood', ''),
            "address": hotel.get('address', ''),
            "score": hotel.get('calculated_score', 0),
            "main_image": main_image,
            "images": images[:5],  # Limit to 5 images
            "amenities": amenities[:10],  # Limit to 10 amenities
            "hotel_class": hotel.get('hotel_class', ''),
            "description": hotel.get('description', ''),
            "link": hotel.get('link', '')
        }
        
        return formatted
    
    def get_best_hotels(self,
                       location: str,
                       check_in_date: str,
                       check_out_date: str,
                       adults: int = 2,
                       children: int = 0,
                       limit: int = 5) -> Dict[str, Any]:
        """
        Get best hotels based on price, rating and location
        
        Args:
            location: Location to search
            check_in_date: Check-in date (YYYY-MM-DD)
            check_out_date: Check-out date (YYYY-MM-DD)
            adults: Number of adults
            children: Number of children
            limit: Number of results to return
            
        Returns:
            Dictionary containing best hotels and analysis
        """
        try:
            # Search hotels
            hotel_data = self.search_hotels(
                location=location,
                check_in_date=check_in_date,
                check_out_date=check_out_date,
                adults=adults,
                children=children
            )
            
            if not hotel_data:
                return {
                    "success": False,
                    "error": "Failed to search hotels"
                }
            
            hotels = hotel_data.get('properties', [])
            
            if not hotels:
                return {
                    "success": False,
                    "error": "No hotels found for the specified location and dates"
                }
            
            # Sort hotels by best value using external sorting module
            sorted_hotels = HotelSorter.sort_by_composite_score(hotels)
            
            # Get best hotels
            max_hotels = min(limit if limit > 1 else 6, len(sorted_hotels))
            best_hotels = sorted_hotels[:max_hotels]
            
            # Format result
            result = {
                "success": True,
                "location": location,
                "search_params": {
                    "check_in_date": check_in_date,
                    "check_out_date": check_out_date,
                    "adults": adults,
                    "children": children,
                    "total_guests": adults + children,
                    "requested_limit": limit
                },
                "total_hotels_found": len(hotels)
            }
            
            # Add top 1 and top 5 structure similar to flights
            if best_hotels:
                # Top 1: Best hotel
                result["top_1"] = {
                    "description": "Khách sạn tốt nhất về giá cả và chất lượng",
                    "optimization": "40% giá cả + 30% rating + 30% vị trí",
                    "hotel": self._format_hotel(best_hotels[0])
                }
                
                # Top 5: Other best hotels based on limit
                if limit == 1:
                    result["top_5"] = {
                        "description": "Chỉ hiển thị 1 khách sạn tốt nhất (limit=1)",
                        "hotels": []
                    }
                elif len(best_hotels) > 1:
                    max_others = min(limit - 1, len(best_hotels) - 1, 5)
                    other_hotels = best_hotels[1:1+max_others]
                    result["top_5"] = {
                        "description": f"{len(other_hotels)} khách sạn tốt nhất khác",
                        "hotels": [self._format_hotel(hotel) for hotel in other_hotels]
                    }
                else:
                    result["top_5"] = {
                        "description": "Không có khách sạn khác",
                        "hotels": []
                    }
            else:
                result["top_1"] = None
                result["top_5"] = {"hotels": []}
            
            # Add summary info using analysis module
            if hotels:
                analysis = analyze_hotel_data(hotels)
                result["summary"] = {
                    "total_hotels_analyzed": analysis.get("total_hotels", 0),
                    "hotels_returned": len(best_hotels),
                    "price_analysis": analysis.get("price_analysis", {}),
                    "rating_analysis": analysis.get("rating_analysis", {}),
                    "distance_analysis": analysis.get("distance_analysis", {}),
                    "price_categories": analysis.get("price_categories", {}),
                    "rating_categories": analysis.get("rating_categories", {})
                }
            
            return result
            
        except Exception as e:
            logger.error(f"Error in get_best_hotels: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    def save_results_to_file(self, results: Dict[str, Any], filename: str = None) -> str:
        """
        Save hotel search results to JSON file
        
        Args:
            results: Hotel search results
            filename: Optional filename, will auto-generate if not provided
            
        Returns:
            Filename where results were saved
        """
        try:
            if not filename:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"hotel_results_{timestamp}.json"
            
            with open(filename, "w", encoding="utf-8") as f:
                json.dump(results, f, ensure_ascii=False, indent=4)
            
            logger.info(f"Results saved to {filename}")
            return filename
            
        except Exception as e:
            logger.error(f"Error saving results to file: {e}")
            return ""


# Global service instance
hotel_service = HotelService()

def search_best_hotels(location: str,
                      check_in_date: str,
                      check_out_date: str,
                      adults: int = 2,
                      children: int = 0,
                      limit: int = 5) -> Dict[str, Any]:
    """
    Convenience function to search for best hotels
    
    Args:
        location: Location to search
        check_in_date: Check-in date (YYYY-MM-DD)
        check_out_date: Check-out date (YYYY-MM-DD)
        adults: Number of adults
        children: Number of children
        limit: Number of results to return
        
    Returns:
        Dictionary containing best hotels and analysis
        
    Example:
        results = search_best_hotels(
            location="Seoul, South Korea",
            check_in_date="2025-10-15",
            check_out_date="2025-10-18",
            adults=2,
            children=1,
            limit=3
        )
    """
    return hotel_service.get_best_hotels(
        location=location,
        check_in_date=check_in_date,
        check_out_date=check_out_date,
        adults=adults,
        children=children,
        limit=limit
    )


# Example usage and testing
# if __name__ == "__main__":
#     # Test hotel search
#     results = search_best_hotels(
#         location="Seoul, South Korea",
#         check_in_date="2025-10-15",
#         check_out_date="2025-10-18",
#         adults=2,
#         children=0,
#         limit=5
#     )
    
#     print("=== HOTEL SEARCH RESULTS ===")
#     print(f"Success: {results['success']}")
    
#     if results['success']:
#         print(f"Location: {results['location']}")
#         print(f"Total hotels found: {results['total_hotels_found']}")
        
#         if results['top_1']:
#             hotel = results['top_1']['hotel']
#             print(f"\nBest hotel: {hotel['name']}")
#             print(f"Price: {hotel['price']}, Rating: {hotel['rating']}")
#             print(f"Score: {hotel['score']}")
        
#         # Save results
#         filename = hotel_service.save_results_to_file(results)
#         print(f"\nResults saved to: {filename}")
#     else:
#         print(f"Error: {results.get('error')}")
