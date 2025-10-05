import pandas as pd
import math
from typing import Dict, List, Tuple, Optional
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class IATACodeFinder:
    def __init__(self):
        """Initialize the IATA code finder with airport data"""
        self.df = None
        self._load_airport_data()
    
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

    def find_nearest_by_coordinates(self, lat: float, lon: float, limit: int = 5) -> List[Dict]:
        """
        Find nearest airports by coordinates, prioritizing civilian airports
        
        Args:
            lat: Latitude
            lon: Longitude
            limit: Number of results to return
            
        Returns:
            List of dictionaries containing airport information
        """
        if self.df is None:
            raise RuntimeError("Airport data not loaded")
            
        # Calculate distances
        df_copy = self.df.copy()
        df_copy["Distance_km"] = df_copy.apply(
            lambda row: self.haversine(lat, lon, row["Latitude"], row["Longitude"]), 
            axis=1
        )
        
        # Filter out military bases
        military_keywords = [
            'air base', 'air force', 'military', 'naval', 'army', 'marine',
            'airbase', 'afb', 'base', 'nas', 'mcas', 'joint base'
        ]
        
        # Create a mask for civilian airports (exclude military)
        civilian_mask = True
        for keyword in military_keywords:
            civilian_mask &= ~df_copy["Name"].str.contains(keyword, case=False, na=False)
        
        civilian_airports = df_copy[civilian_mask]
        
        # If we have civilian airports within reasonable distance, use them
        if len(civilian_airports) > 0:
            nearest = civilian_airports.sort_values("Distance_km").head(limit)
            logger.info(f"Found {len(civilian_airports)} civilian airports within range")
        else:
            # Fall back to all airports if no civilian ones found
            nearest = df_copy.sort_values("Distance_km").head(limit)
            logger.warning("No civilian airports found, using all airports")
        
        return nearest[["Name", "City", "Country", "IATA", "Distance_km"]].to_dict('records')

    def find_nearest_by_city(self, city_name: str, limit: int = 5) -> List[Dict]:
        """
        Find nearest airports by city name, prioritizing civilian airports
        
        Args:
            city_name: Name of the city
            limit: Number of results to return
            
        Returns:
            List of dictionaries containing airport information
        """
        if self.df is None:
            raise RuntimeError("Airport data not loaded")
            
        # Search for exact matches first, then partial matches
        city_matches = self.df[
            self.df["City"].str.contains(city_name, case=False, na=False) |
            self.df["Name"].str.contains(city_name, case=False, na=False)
        ]
        
        if len(city_matches) == 0:
            logger.warning(f"No airports found for city: {city_name}")
            return []
        
        # Filter out military bases and prioritize civilian airports
        # Military keywords to exclude
        military_keywords = [
            'air base', 'air force', 'military', 'naval', 'army', 'marine',
            'airbase', 'afb', 'base', 'nas', 'mcas', 'joint base'
        ]
        
        # Create a mask for civilian airports (exclude military)
        civilian_mask = True
        for keyword in military_keywords:
            civilian_mask &= ~city_matches["Name"].str.contains(keyword, case=False, na=False)
        
        civilian_airports = city_matches[civilian_mask]
        
        # If we have civilian airports, use them; otherwise fall back to all matches
        if len(civilian_airports) > 0:
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
        
        Args:
            lat: Latitude of the coordinates
            lon: Longitude of the coordinates  
            city_name: Name of the city to search
            
        Returns:
            Dictionary containing:
            - coordinate_result: Nearest airport by coordinates
            - city_result: Nearest airport by city name
            - coordinate_iata: IATA code from coordinates
            - city_iata: IATA code from city
        """
        try:
            # Find nearest by coordinates
            coord_results = self.find_nearest_by_coordinates(lat, lon, limit=1)
            coord_airport = coord_results[0] if coord_results else None
            coord_iata = coord_airport["IATA"] if coord_airport else None
            
            # Find nearest by city name
            city_results = self.find_nearest_by_city(city_name, limit=1)
            city_airport = city_results[0] if city_results else None
            city_iata = city_airport["IATA"] if city_airport else None
            
            return {
                "success": True,
                "coordinate_result": coord_airport,
                "city_result": city_airport,
                "coordinate_iata": coord_iata,
                "city_iata": city_iata,
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
                "city_iata": None
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
