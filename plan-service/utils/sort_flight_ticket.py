"""
Flight Ticket Sorting Algorithms
Provides various sorting strategies for flight tickets based on different criteria
"""

from typing import List, Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)

class FlightSorter:
    """Class containing various flight sorting algorithms"""
    
    @staticmethod
    def sort_by_price(flights: List[Dict[str, Any]], ascending: bool = True) -> List[Dict[str, Any]]:
        """
        Sort flights by price
        
        Args:
            flights: List of flight dictionaries
            ascending: If True, sort from cheapest to most expensive
            
        Returns:
            Sorted list of flights
        """
        return sorted(flights, key=lambda x: x.get('price', float('inf')), reverse=not ascending)
    
    @staticmethod
    def sort_by_duration(flights: List[Dict[str, Any]], ascending: bool = True) -> List[Dict[str, Any]]:
        """
        Sort flights by total duration
        
        Args:
            flights: List of flight dictionaries
            ascending: If True, sort from shortest to longest duration
            
        Returns:
            Sorted list of flights
        """
        return sorted(flights, key=lambda x: x.get('total_duration', float('inf')), reverse=not ascending)
    
    @staticmethod
    def sort_by_carbon_emissions(flights: List[Dict[str, Any]], ascending: bool = True) -> List[Dict[str, Any]]:
        """
        Sort flights by carbon emissions
        
        Args:
            flights: List of flight dictionaries
            ascending: If True, sort from lowest to highest emissions
            
        Returns:
            Sorted list of flights
        """
        def get_emissions(flight):
            carbon = flight.get('carbon_emissions', {})
            # Try to get typical emissions or use difference_percent
            if 'typical_for_this_route' in carbon:
                return carbon['typical_for_this_route']
            elif 'difference_percent' in carbon:
                # Assume base emission and calculate from percentage
                base_emission = 100000  # Default base emission
                return base_emission * (1 + carbon['difference_percent'] / 100)
            return float('inf')
        
        return sorted(flights, key=get_emissions, reverse=not ascending)
    
    @staticmethod
    def sort_by_score(flights: List[Dict[str, Any]], 
                     price_weight: float = 0.4,
                     duration_weight: float = 0.3,
                     emissions_weight: float = 0.3) -> List[Dict[str, Any]]:
        """
        Sort flights by a weighted score combining price, duration, and emissions
        Lower score is better
        
        Args:
            flights: List of flight dictionaries
            price_weight: Weight for price factor (0-1)
            duration_weight: Weight for duration factor (0-1)
            emissions_weight: Weight for emissions factor (0-1)
            
        Returns:
            Sorted list of flights with calculated scores
        """
        if not flights:
            return flights
        
        # Normalize weights
        total_weight = price_weight + duration_weight + emissions_weight
        price_weight /= total_weight
        duration_weight /= total_weight
        emissions_weight /= total_weight
        
        # Get min/max values for normalization
        prices = [f.get('price', 0) for f in flights if f.get('price')]
        durations = [f.get('total_duration', 0) for f in flights if f.get('total_duration')]
        
        emissions = []
        for f in flights:
            carbon = f.get('carbon_emissions', {})
            if 'typical_for_this_route' in carbon:
                emissions.append(carbon['typical_for_this_route'])
            elif 'difference_percent' in carbon:
                emissions.append(abs(carbon['difference_percent']))
        
        if not prices or not durations:
            return flights
        
        min_price, max_price = min(prices), max(prices)
        min_duration, max_duration = min(durations), max(durations)
        min_emissions, max_emissions = (min(emissions), max(emissions)) if emissions else (0, 1)
        
        # Calculate scores
        scored_flights = []
        for flight in flights:
            price = flight.get('price', max_price)
            duration = flight.get('total_duration', max_duration)
            
            # Get emissions
            carbon = flight.get('carbon_emissions', {})
            if 'typical_for_this_route' in carbon:
                emission_value = carbon['typical_for_this_route']
            elif 'difference_percent' in carbon:
                emission_value = abs(carbon['difference_percent'])
            else:
                emission_value = max_emissions
            
            # Normalize values (0-1 scale)
            price_norm = (price - min_price) / (max_price - min_price) if max_price > min_price else 0
            duration_norm = (duration - min_duration) / (max_duration - min_duration) if max_duration > min_duration else 0
            emissions_norm = (emission_value - min_emissions) / (max_emissions - min_emissions) if max_emissions > min_emissions else 0
            
            # Calculate weighted score
            score = (price_norm * price_weight + 
                    duration_norm * duration_weight + 
                    emissions_norm * emissions_weight)
            
            flight_copy = flight.copy()
            flight_copy['calculated_score'] = round(score, 4)
            scored_flights.append(flight_copy)
        
        return sorted(scored_flights, key=lambda x: x['calculated_score'])
    
    @staticmethod
    def get_best_flights(flights: List[Dict[str, Any]], 
                        sort_by: str = "score",
                        limit: int = 5) -> List[Dict[str, Any]]:
        """
        Get the best flights based on specified criteria
        When limit=1, automatically uses weighted score for best overall flight
        
        Args:
            flights: List of flight dictionaries
            sort_by: Sorting criteria ('price', 'duration', 'emissions', 'score')
            limit: Number of flights to return
            
        Returns:
            List of best flights
        """
        if not flights:
            return []
        
        # Special case: if limit=1, always find the best overall flight using weighted score
        # This prioritizes both short duration and low price
        if limit == 1:
            logger.info("Finding single best flight using optimized score (duration + price)")
            # Use custom weights: 50% price, 40% duration, 10% emissions for best single flight
            sorted_flights = FlightSorter.sort_by_score(
                flights, 
                price_weight=0.5,      # 50% weight on price
                duration_weight=0.4,   # 40% weight on duration  
                emissions_weight=0.1   # 10% weight on emissions
            )
        elif sort_by == "price":
            sorted_flights = FlightSorter.sort_by_price(flights)
        elif sort_by == "duration":
            sorted_flights = FlightSorter.sort_by_duration(flights)
        elif sort_by == "emissions":
            sorted_flights = FlightSorter.sort_by_carbon_emissions(flights)
        elif sort_by == "score":
            sorted_flights = FlightSorter.sort_by_score(flights)
        else:
            logger.warning(f"Unknown sort_by parameter: {sort_by}. Using 'score' as default.")
            sorted_flights = FlightSorter.sort_by_score(flights)
        
        return sorted_flights[:limit]


def analyze_flight_data(data: Dict[str, Any]) -> Dict[str, Any]:
    """
    Analyze flight data and provide insights
    
    Args:
        data: Flight search result data
        
    Returns:
        Dictionary containing analysis results
    """
    if 'other_flights' not in data:
        return {"error": "No flight data found"}
    
    flights = data['other_flights']
    
    if not flights:
        return {"error": "No flights available"}
    
    # Basic statistics
    prices = [f.get('price', 0) for f in flights if f.get('price')]
    durations = [f.get('total_duration', 0) for f in flights if f.get('total_duration')]
    
    analysis = {
        "total_flights": len(flights),
        "price_range": {
            "min": min(prices) if prices else 0,
            "max": max(prices) if prices else 0,
            "avg": round(sum(prices) / len(prices), 2) if prices else 0
        },
        "duration_range": {
            "min": min(durations) if durations else 0,
            "max": max(durations) if durations else 0,
            "avg": round(sum(durations) / len(durations), 2) if durations else 0
        }
    }
    
    # Get best flights by different criteria
    analysis["best_by_price"] = FlightSorter.get_best_flights(flights, "price", 3)
    analysis["best_by_duration"] = FlightSorter.get_best_flights(flights, "duration", 3)
    analysis["best_overall"] = FlightSorter.get_best_flights(flights, "score", 5)
    
    return analysis
