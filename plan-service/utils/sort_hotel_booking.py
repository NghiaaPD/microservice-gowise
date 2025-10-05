"""
Hotel Booking Sorting Utilities
Algorithms for sorting and ranking hotels based on multiple criteria
"""

import logging
from typing import Dict, List, Any, Optional
import statistics

# Configure logging
logger = logging.getLogger(__name__)

class HotelSorter:
    """Class for sorting hotels by various criteria"""
    
    @staticmethod
    def calculate_hotel_score(hotel: Dict[str, Any], 
                             price_weight: float = 0.4,
                             rating_weight: float = 0.3,
                             location_weight: float = 0.3) -> float:
        """
        Calculate hotel score based on price, rating, and location
        
        Args:
            hotel: Hotel dictionary
            price_weight: Weight for price factor (0-1)
            rating_weight: Weight for rating factor (0-1) 
            location_weight: Weight for location factor (0-1)
            
        Returns:
            Hotel score (lower is better)
        """
        try:
            # Extract price (remove currency symbols)
            price_str = hotel.get('rate_per_night', {}).get('extracted_lowest', '0')
            if isinstance(price_str, str):
                price = float(''.join(filter(str.isdigit, price_str))) or float('inf')
            else:
                price = float(price_str) if price_str else float('inf')
            
            # Extract rating
            rating = hotel.get('overall_rating', 0)
            if rating == 0:
                rating = 3.0  # Default rating if not available
            
            # Extract distance (if available)
            distance_str = hotel.get('distance', '0')
            if isinstance(distance_str, str):
                distance = float(''.join(filter(str.isdigit, distance_str.split()[0]))) or 0
            else:
                distance = float(distance_str) if distance_str else 0
            
            # Normalize scores (0-1 scale, lower is better)
            # Price: lower price = lower score (better)
            price_score = min(price / 1000, 1.0)  # Normalize around $1000
            
            # Rating: higher rating = lower score (better) 
            rating_score = (5.0 - rating) / 5.0
            
            # Distance: shorter distance = lower score (better)
            location_score = min(distance / 10, 1.0)  # Normalize around 10km
            
            # Calculate weighted score
            total_weight = price_weight + rating_weight + location_weight
            price_weight /= total_weight
            rating_weight /= total_weight  
            location_weight /= total_weight
            
            score = (price_score * price_weight + 
                    rating_score * rating_weight + 
                    location_score * location_weight)
            
            return score
            
        except Exception as e:
            logger.error(f"Error calculating hotel score: {e}")
            return float('inf')
    
    @staticmethod
    def sort_by_price(hotels: List[Dict[str, Any]], ascending: bool = True) -> List[Dict[str, Any]]:
        """
        Sort hotels by price
        
        Args:
            hotels: List of hotel dictionaries
            ascending: Sort order (True for cheapest first)
            
        Returns:
            Sorted list of hotels
        """
        def get_price(hotel):
            price_str = hotel.get('rate_per_night', {}).get('extracted_lowest', '0')
            if isinstance(price_str, str):
                return float(''.join(filter(str.isdigit, price_str))) or float('inf')
            return float(price_str) if price_str else float('inf')
        
        return sorted(hotels, key=get_price, reverse=not ascending)
    
    @staticmethod
    def sort_by_rating(hotels: List[Dict[str, Any]], ascending: bool = False) -> List[Dict[str, Any]]:
        """
        Sort hotels by rating
        
        Args:
            hotels: List of hotel dictionaries
            ascending: Sort order (False for highest rating first)
            
        Returns:
            Sorted list of hotels
        """
        def get_rating(hotel):
            return hotel.get('overall_rating', 0)
        
        return sorted(hotels, key=get_rating, reverse=not ascending)
    
    @staticmethod
    def sort_by_distance(hotels: List[Dict[str, Any]], ascending: bool = True) -> List[Dict[str, Any]]:
        """
        Sort hotels by distance from center
        
        Args:
            hotels: List of hotel dictionaries
            ascending: Sort order (True for closest first)
            
        Returns:
            Sorted list of hotels
        """
        def get_distance(hotel):
            distance_str = hotel.get('distance', '0')
            if isinstance(distance_str, str):
                return float(''.join(filter(str.isdigit, distance_str.split()[0]))) or 0
            return float(distance_str) if distance_str else 0
        
        return sorted(hotels, key=get_distance, reverse=not ascending)
    
    @staticmethod
    def sort_by_composite_score(hotels: List[Dict[str, Any]], 
                               price_weight: float = 0.4,
                               rating_weight: float = 0.3,
                               location_weight: float = 0.3) -> List[Dict[str, Any]]:
        """
        Sort hotels by composite score (price + rating + location)
        
        Args:
            hotels: List of hotel dictionaries
            price_weight: Weight for price factor (0-1)
            rating_weight: Weight for rating factor (0-1)
            location_weight: Weight for location factor (0-1)
            
        Returns:
            Sorted list of hotels with calculated scores
        """
        scored_hotels = []
        
        for hotel in hotels:
            score = HotelSorter.calculate_hotel_score(
                hotel, price_weight, rating_weight, location_weight
            )
            hotel_copy = hotel.copy()
            hotel_copy['calculated_score'] = round(score, 4)
            scored_hotels.append(hotel_copy)
        
        return sorted(scored_hotels, key=lambda x: x['calculated_score'])

def analyze_hotel_data(hotels: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Analyze hotel data to get statistics and insights
    
    Args:
        hotels: List of hotel dictionaries
        
    Returns:
        Dictionary containing analysis results
    """
    if not hotels:
        return {"error": "No hotels to analyze"}
    
    try:
        # Extract prices
        prices = []
        for hotel in hotels:
            price_str = hotel.get('rate_per_night', {}).get('extracted_lowest', '0')
            if isinstance(price_str, str):
                price = float(''.join(filter(str.isdigit, price_str))) or 0
            else:
                price = float(price_str) if price_str else 0
            if price > 0:
                prices.append(price)
        
        # Extract ratings
        ratings = [h.get('overall_rating', 0) for h in hotels if h.get('overall_rating', 0) > 0]
        
        # Extract distances
        distances = []
        for hotel in hotels:
            distance_str = hotel.get('distance', '0')
            if isinstance(distance_str, str):
                distance = float(''.join(filter(str.isdigit, distance_str.split()[0]))) or 0
            else:
                distance = float(distance_str) if distance_str else 0
            if distance > 0:
                distances.append(distance)
        
        # Calculate statistics
        analysis = {
            "total_hotels": len(hotels),
            "price_analysis": {
                "count": len(prices),
                "min": min(prices) if prices else 0,
                "max": max(prices) if prices else 0,
                "avg": round(statistics.mean(prices), 2) if prices else 0,
                "median": round(statistics.median(prices), 2) if prices else 0,
                "std_dev": round(statistics.stdev(prices), 2) if len(prices) > 1 else 0
            },
            "rating_analysis": {
                "count": len(ratings),
                "min": min(ratings) if ratings else 0,
                "max": max(ratings) if ratings else 0,
                "avg": round(statistics.mean(ratings), 2) if ratings else 0,
                "median": round(statistics.median(ratings), 2) if ratings else 0
            },
            "distance_analysis": {
                "count": len(distances),
                "min": round(min(distances), 2) if distances else 0,
                "max": round(max(distances), 2) if distances else 0,
                "avg": round(statistics.mean(distances), 2) if distances else 0,
                "median": round(statistics.median(distances), 2) if distances else 0
            }
        }
        
        # Add price categories
        if prices:
            price_ranges = {
                "budget": [p for p in prices if p < 100],
                "mid_range": [p for p in prices if 100 <= p < 300],
                "luxury": [p for p in prices if p >= 300]
            }
            
            analysis["price_categories"] = {
                "budget_count": len(price_ranges["budget"]),
                "mid_range_count": len(price_ranges["mid_range"]),
                "luxury_count": len(price_ranges["luxury"]),
                "budget_percentage": round(len(price_ranges["budget"]) / len(prices) * 100, 1),
                "mid_range_percentage": round(len(price_ranges["mid_range"]) / len(prices) * 100, 1),
                "luxury_percentage": round(len(price_ranges["luxury"]) / len(prices) * 100, 1)
            }
        
        # Add rating categories
        if ratings:
            rating_ranges = {
                "excellent": [r for r in ratings if r >= 4.5],
                "very_good": [r for r in ratings if 4.0 <= r < 4.5],
                "good": [r for r in ratings if 3.5 <= r < 4.0],
                "fair": [r for r in ratings if r < 3.5]
            }
            
            analysis["rating_categories"] = {
                "excellent_count": len(rating_ranges["excellent"]),
                "very_good_count": len(rating_ranges["very_good"]),
                "good_count": len(rating_ranges["good"]),
                "fair_count": len(rating_ranges["fair"])
            }
        
        return analysis
        
    except Exception as e:
        logger.error(f"Error analyzing hotel data: {e}")
        return {"error": f"Analysis failed: {str(e)}"}

def get_best_value_hotels(hotels: List[Dict[str, Any]], 
                         price_weight: float = 0.4,
                         rating_weight: float = 0.3,
                         location_weight: float = 0.3,
                         limit: int = 5) -> List[Dict[str, Any]]:
    """
    Get best value hotels based on composite scoring
    
    Args:
        hotels: List of hotel dictionaries
        price_weight: Weight for price factor (0-1)
        rating_weight: Weight for rating factor (0-1)
        location_weight: Weight for location factor (0-1)
        limit: Maximum number of hotels to return
        
    Returns:
        List of best value hotels with scores
    """
    if not hotels:
        return []
    
    # Sort by composite score
    sorted_hotels = HotelSorter.sort_by_composite_score(
        hotels, price_weight, rating_weight, location_weight
    )
    
    return sorted_hotels[:limit]

def filter_hotels_by_criteria(hotels: List[Dict[str, Any]], 
                             min_rating: Optional[float] = None,
                             max_price: Optional[float] = None,
                             max_distance: Optional[float] = None,
                             amenities: Optional[List[str]] = None) -> List[Dict[str, Any]]:
    """
    Filter hotels by various criteria
    
    Args:
        hotels: List of hotel dictionaries
        min_rating: Minimum rating filter
        max_price: Maximum price filter
        max_distance: Maximum distance filter (km)
        amenities: Required amenities list
        
    Returns:
        Filtered list of hotels
    """
    filtered = hotels.copy()
    
    # Filter by rating
    if min_rating is not None:
        filtered = [h for h in filtered if h.get('overall_rating', 0) >= min_rating]
    
    # Filter by price
    if max_price is not None:
        def get_price(hotel):
            price_str = hotel.get('rate_per_night', {}).get('extracted_lowest', '0')
            if isinstance(price_str, str):
                return float(''.join(filter(str.isdigit, price_str))) or float('inf')
            return float(price_str) if price_str else float('inf')
        
        filtered = [h for h in filtered if get_price(h) <= max_price]
    
    # Filter by distance
    if max_distance is not None:
        def get_distance(hotel):
            distance_str = hotel.get('distance', '0')
            if isinstance(distance_str, str):
                return float(''.join(filter(str.isdigit, distance_str.split()[0]))) or 0
            return float(distance_str) if distance_str else 0
        
        filtered = [h for h in filtered if get_distance(h) <= max_distance]
    
    # Filter by amenities
    if amenities:
        def has_amenities(hotel):
            hotel_amenities = hotel.get('amenities', [])
            if isinstance(hotel_amenities, list):
                hotel_amenities_lower = [a.lower() for a in hotel_amenities]
                return all(amenity.lower() in hotel_amenities_lower for amenity in amenities)
            return False
        
        filtered = [h for h in filtered if has_amenities(h)]
    
    return filtered

# Example usage and testing
if __name__ == "__main__":
    # Sample hotel data for testing
    sample_hotels = [
        {
            "name": "Luxury Hotel A",
            "rate_per_night": {"extracted_lowest": "250"},
            "overall_rating": 4.5,
            "distance": "2.1 km",
            "amenities": ["WiFi", "Pool", "Spa", "Gym"]
        },
        {
            "name": "Budget Hotel B", 
            "rate_per_night": {"extracted_lowest": "80"},
            "overall_rating": 3.8,
            "distance": "5.3 km",
            "amenities": ["WiFi", "Breakfast"]
        },
        {
            "name": "Business Hotel C",
            "rate_per_night": {"extracted_lowest": "180"},
            "overall_rating": 4.2,
            "distance": "1.5 km",
            "amenities": ["WiFi", "Business Center", "Gym"]
        }
    ]
    
    print("=== HOTEL SORTING ALGORITHMS TEST ===")
    
    # Test composite scoring
    best_value = get_best_value_hotels(sample_hotels, limit=3)
    print("\nBest Value Hotels:")
    for i, hotel in enumerate(best_value, 1):
        print(f"{i}. {hotel['name']} - Score: {hotel['calculated_score']}")
    
    # Test analysis
    analysis = analyze_hotel_data(sample_hotels)
    print("\nHotel Analysis:")
    print(f"Total hotels: {analysis['total_hotels']}")
    print(f"Price range: ${analysis['price_analysis']['min']} - ${analysis['price_analysis']['max']}")
    print(f"Average rating: {analysis['rating_analysis']['avg']}")
    
    # Test filtering
    filtered = filter_hotels_by_criteria(
        sample_hotels,
        min_rating=4.0,
        max_price=200,
        amenities=["WiFi", "Gym"]
    )
    print(f"\nFiltered hotels (rating >= 4.0, price <= $200, has WiFi & Gym): {len(filtered)}")
    for hotel in filtered:
        print(f"- {hotel['name']}")
