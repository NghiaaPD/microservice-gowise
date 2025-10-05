import pandas as pd
import math
from typing import Dict, List, Tuple, Optional
import logging
from fuzzywuzzy import fuzz, process

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class IATACodeFinder:
    def __init__(self):
        """Initialize the IATA code finder with airport data"""
        self.df = None
        self.military_keywords = [
            'air base', 'air force', 'military', 'naval', 'army', 'marine',
            'airbase', 'afb', 'base', 'nas', 'mcas', 'joint base'
        ]
        self._load_airport_data()
        
        # Initialize city list for fuzzy matching
        if self.df is not None:
            # Filter out NaN values and empty strings
            self.city_list = [
                city for city in self.df['City'].unique().tolist() 
                if pd.notna(city) and isinstance(city, str) and city.strip()
            ]
    
    def _load_airport_data(self):
        """Load airport data from OpenFlights dataset"""
        try:
            url = "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat"
            cols = [
                "AirportID", "Name", "City", "Country", "IATA", "ICAO",
                "Latitude", "Longitude", "Altitude", "Timezone", "DST",
                "TzDatabaseTimeZone", "Type", "Source"
            ]
            
            logger.info("Loading airport data from OpenFlights...")
            self.df = pd.read_csv(url, names=cols)
            self.df = self.df.dropna(subset=["IATA", "Latitude", "Longitude"])
            
            # Clean IATA codes (remove invalid ones)
            self.df = self.df[self.df["IATA"].str.len() == 3]  # IATA codes are 3 characters
            self.df = self.df[self.df["IATA"] != "\\N"]  # Remove null markers
            
            logger.info(f"Loaded {len(self.df)} airports with valid IATA codes")
            
        except Exception as e:
            logger.error(f"Failed to load airport data: {e}")
            raise

    @staticmethod
    def haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """
        Calculate the great circle distance between two points on Earth
        using the Haversine formula
        
        Args:
            lat1, lon1: Latitude and longitude of first point (degrees)
            lat2, lon2: Latitude and longitude of second point (degrees)
            
        Returns:
            Distance in kilometers
        """
        R = 6371  # Earth's radius in kilometers
        
        phi1, phi2 = math.radians(lat1), math.radians(lat2)
        dphi = math.radians(lat2 - lat1)
        dlambda = math.radians(lon2 - lon1)
        
        a = (math.sin(dphi/2)**2 + 
             math.cos(phi1) * math.cos(phi2) * math.sin(dlambda/2)**2)
        
        return 2 * R * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    def fuzzy_match_city(self, city_name: str, threshold: int = 80) -> str:
        """
        Find the best matching city name using fuzzy matching
        
        Args:
            city_name: Input city name to match
            threshold: Minimum confidence score (0-100)
            
        Returns:
            Best matching city name or None if no good match found
        """
        if not self.city_list or not city_name:
            return None
            
        # Use fuzzy matching to find the best match
        best_match = process.extractOne(
            city_name, 
            self.city_list, 
            scorer=fuzz.ratio
        )
        
        if best_match and best_match[1] >= threshold:
            logger.info(f"Fuzzy match: '{city_name}' → '{best_match[0]}' (confidence: {best_match[1]}%)")
            return best_match[0]
        else:
            logger.warning(f"No good fuzzy match found for '{city_name}' (best score: {best_match[1] if best_match else 0}%)")
            return None

    def find_nearest_by_coordinates(self, lat: float, lon: float, limit: int = 5, max_distance: float = None) -> list:
        """
        Find nearest airports by coordinates using Haversine formula
        
        Args:
            lat: Latitude 
            lon: Longitude
            limit: Number of nearest airports to return
            max_distance: Maximum distance in km to search (None = no limit)
            
        Returns:
            List of nearest airports with distance information
        """
        logger.info(f"Finding nearest airports to coordinates: ({lat}, {lon})")
        
        df_copy = self.df.copy()
        df_copy["Distance_km"] = df_copy.apply(
            lambda row: self.haversine(lat, lon, row["Latitude"], row["Longitude"]), 
            axis=1
        )
        
        # Apply distance filter if specified
        if max_distance is not None:
            df_copy = df_copy[df_copy["Distance_km"] <= max_distance]
            logger.info(f"Filtering airports within {max_distance}km")
        
        # Filter out military bases
        # Create a mask for civilian airports (exclude military)
        civilian_mask = True
        for keyword in self.military_keywords:
            civilian_mask &= ~df_copy["Name"].str.contains(keyword, case=False, na=False)
        
        civilian_airports = df_copy[civilian_mask]
        
        # If we have civilian airports within specified criteria, use them
        if len(civilian_airports) > 0:
            nearest = civilian_airports.sort_values("Distance_km").head(limit)
            logger.info(f"Found {len(civilian_airports)} civilian airports within criteria")
        else:
            # Fall back to all airports (within distance limit) if no civilian ones found
            nearest = df_copy.sort_values("Distance_km").head(limit)
            logger.warning("No civilian airports found within criteria, using all airports")
        
        return nearest[["Name", "City", "Country", "IATA", "Distance_km"]].to_dict('records')

    def find_nearest_by_city(self, city_name: str, limit: int = 5, fallback_coordinates: tuple = None, max_fallback_distance: float = 100.0) -> list:
        """
        Find airports by city name using fuzzy matching with geographical fallback option
        
        Args:
            city_name: Name of the city to search
            limit: Maximum number of results to return
            fallback_coordinates: (lat, lon) tuple for geographical fallback when no city match found
            max_fallback_distance: Maximum distance in km for geographical fallback (default: 100km)
            
        Returns:
            List of airports matching the city name or nearby airports if fallback is used
        """
        logger.info(f"Searching airports for city: {city_name}")
        
        # First try fuzzy matching to find the best city name
        matched_city = self.fuzzy_match_city(city_name, threshold=70)  # Lower threshold for more flexibility
        
        city_matches = pd.DataFrame()
        
        if matched_city:
            # Use the fuzzy matched city name for exact search
            city_matches = self.df[
                self.df['City'].str.lower() == matched_city.lower()
            ].copy()
            logger.info(f"Found {len(city_matches)} airports for fuzzy matched city: '{matched_city}'")
        else:
            # Fallback to partial matching if fuzzy match fails
            clean_city = city_name.strip().lower()
            city_matches = self.df[
                self.df['City'].str.lower().str.contains(clean_city, na=False, regex=False)
            ].copy()
            logger.info(f"Found {len(city_matches)} airports using partial matching for: '{clean_city}'")
        
        # If no city matches and fallback coordinates provided, use geographical search  
        if city_matches.empty and fallback_coordinates:
            lat, lon = fallback_coordinates
            logger.info(f"No city matches for '{city_name}', using geographical fallback within {max_fallback_distance}km")
            return self.find_nearest_by_coordinates(lat, lon, limit=limit, max_distance=max_fallback_distance)
        
        if city_matches.empty:
            logger.warning(f"No airports found for city: {city_name}")
            return []
        
        # Filter out military airports
        civilian_airports = city_matches[
            ~city_matches['Name'].str.lower().str.contains('|'.join(self.military_keywords), na=False, regex=False)
        ]
        
        if not civilian_airports.empty:
            logger.info(f"Found {len(civilian_airports)} civilian airports for {city_name}")
            result_airports = civilian_airports
        else:
            logger.warning(f"No civilian airports found for {city_name}, using all matches")
            result_airports = city_matches
        
        # Sort by airport type preference (International > Airport > others)
        def airport_priority(name):
            name_lower = name.lower()
            if 'international' in name_lower:
                return 0  # Highest priority
            elif 'airport' in name_lower:
                return 1
            else:
                return 2  # Lowest priority
        
        result_df = result_airports.copy()
        result_df['priority'] = result_df['Name'].apply(airport_priority)
        result_df = result_df.sort_values('priority')
        
        return result_df[["Name", "City", "Country", "IATA", "Latitude", "Longitude"]].head(limit).to_dict('records')

    def get_nearest_iata_codes(self, lat: float, lon: float, city_name: str) -> Dict[str, any]:
        """
        Get 2 IATA codes: one from coordinates and one from city name
        Enhanced with geographical fallback for cities without airports
        
        Args:
            lat: Latitude of the coordinates
            lon: Longitude of the coordinates  
            city_name: Name of the city to search
            
        Returns:
            Dictionary containing:
            - coordinate_result: Nearest airport by coordinates
            - city_result: Nearest airport by city name (or geographical fallback)
            - coordinate_iata: IATA code from coordinates
            - city_iata: IATA code from city (or geographical fallback)
            - fallback_used: Whether geographical fallback was used for city search
        """
        try:
            # Find nearest by coordinates
            coord_results = self.find_nearest_by_coordinates(lat, lon, limit=1)
            coord_airport = coord_results[0] if coord_results else None
            coord_iata = coord_airport["IATA"] if coord_airport else None
            
            # Find nearest by city name
            city_results = self.find_nearest_by_city(city_name, limit=1)
            fallback_used = False
            
            # If no airports found in city, use geographical fallback
            if not city_results:
                logger.warning(f"No airports found for city: {city_name}, using geographical fallback")
                city_results = self.find_nearest_by_coordinates(lat, lon, limit=1)
                fallback_used = True
                
            city_airport = city_results[0] if city_results else None
            city_iata = city_airport["IATA"] if city_airport else None
            
            # Log the fallback information
            if fallback_used and city_airport:
                logger.info(f"Geographical fallback successful: {city_name} → {city_airport['Name']} ({city_iata}) at {city_airport.get('Distance_km', 'N/A')} km")
            
            return {
                "success": True,
                "coordinate_result": coord_airport,
                "city_result": city_airport,
                "coordinate_iata": coord_iata,
                "city_iata": city_iata,
                "fallback_used": fallback_used,
                "input": {
                    "latitude": lat,
                    "longitude": lon,
                    "city_name": city_name
                }
            }
            
        except Exception as e:
            logger.error(f"Error finding IATA codes: {e}")
            return {
                "success": False,
                "error": str(e),
                "coordinate_iata": None,
                "city_iata": None,
                "fallback_used": False
            }


# Global instance
iata_finder = None

def get_iata_codes(lat: float, lon: float, city_name: str) -> Dict[str, any]:
    """
    Main function to get IATA codes based on coordinates and city name
    
    Args:
        lat: Latitude
        lon: Longitude
        city_name: City name
        
    Returns:
        Dictionary with IATA codes and airport information
        
    Example:
        result = get_iata_codes(10.7769, 106.7009, "Ho Chi Minh City")
        print(f"Coordinate IATA: {result['coordinate_iata']}")
        print(f"City IATA: {result['city_iata']}")
    """
    global iata_finder
    
    if iata_finder is None:
        iata_finder = IATACodeFinder()
    
    return iata_finder.get_nearest_iata_codes(lat, lon, city_name)


# Example usage and testing
# if __name__ == "__main__":
#     # Test với tọa độ TP.HCM
#     result = get_iata_codes(10.7769, 106.7009, "Ho Chi Minh City")
    
#     print("=== RESULT ===")
#     print(f"Success: {result['success']}")
#     print(f"Coordinate IATA: {result['coordinate_iata']}")
#     print(f"City IATA: {result['city_iata']}")
    
#     if result['coordinate_result']:
#         coord = result['coordinate_result']
#         print(f"\nNearest by coordinates: {coord['Name']} ({coord['IATA']}) - {coord['Distance_km']:.2f} km")
    
#     if result['city_result']:
#         city = result['city_result']
#         print(f"Nearest by city: {city['Name']} ({city['IATA']}) in {city['City']}, {city['Country']}")
