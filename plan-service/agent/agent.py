
import os
import json
import math
import glob
import uuid
import logging
from typing import Dict, Any, Optional
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_community.chat_message_histories import RedisChatMessageHistory
from langchain.tools import tool
from langchain.agents import create_tool_calling_agent, AgentExecutor
from dotenv import load_dotenv

load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Tool để query trực tiếp data từ JSON files
@tool  
def query_city_data(city: str, categories: str = "") -> str:
    """
    Query data directly from JSON files in data/ folder
    
    Args:
        city: City name (case insensitive)
        categories: Comma-separated list of categories
    
    Returns:
        JSON string containing place information
    """
    logger.info(f"🔍 query_city_data called with city='{city}', categories='{categories}'")
    
    # Normalize city name
    city_lower = city.lower().strip()
    
    # Find matching JSON file
    import glob
    
    # Try different paths for data folder
    data_paths = [
        "data/*.json",  # Current directory
        "../data/*.json",  # Parent directory
        os.path.join(os.path.dirname(__file__), "..", "data", "*.json")  # Relative to agent folder
    ]
    
    city_files = []
    for path_pattern in data_paths:
        city_files = glob.glob(path_pattern)
        if city_files:
            break
    
    logger.info(f"🔍 Available files: {city_files}")
    
    matching_file = None
    for file_path in city_files:
        file_city = os.path.basename(file_path).replace('.json', '').lower()
        if file_city == city_lower:
            matching_file = file_path
            break
    
    if not matching_file:
        available_cities = [os.path.basename(f).replace('.json', '') for f in city_files]
        return json.dumps({
            "error": f"City '{city}' not found",
            "available_cities": available_cities
        }, ensure_ascii=False)
    
    logger.info(f"✅ Found matching file: {matching_file}")
    
    # Load data
    try:
        with open(matching_file, "r", encoding="utf-8") as f:
            places_data = json.load(f)
        logger.info(f"🔍 Loaded {len(places_data)} places from {matching_file}")
    except Exception as e:
        return json.dumps({"error": f"Error reading file: {str(e)}"}, ensure_ascii=False)
    
    # Filter by categories if specified
    if categories:
        category_list = [cat.strip().lower() for cat in categories.split(",")]
        logger.info(f"🔍 Filtering by categories: {category_list}")
        
        filtered_places = []
        for place in places_data:
            place_category = place.get("category", "").lower()
            for keyword in category_list:
                if keyword in place_category:
                    filtered_places.append(place)
                    break
        
        places_data = filtered_places
        logger.info(f"🔍 After filtering: {len(places_data)} places")
    
    # Sort by rating
    places_data.sort(key=lambda x: x.get("rating", 0) or 0, reverse=True)
    
    return json.dumps({
        "city": city,
        "categories_searched": categories if categories else "all",
        "total_found": len(places_data),
        "places": places_data
    }, ensure_ascii=False, indent=2)

# Tool to search for travel places from local data
@tool
def search_places_in_city(city: str, categories: str = "") -> str:
    """
    Search for places in a city from local JSON data
    
    Args:
        city: City name
        categories: Comma-separated list of categories (restaurant, cafe, attraction, etc.)
    
    Returns:
        JSON string containing place information
    """
    def discover_available_cities():
        """Automatically scan data/ folder to find city data files"""
        import glob
        
        # Find all .json files in data/ folder
        data_paths = [
            "data/*.json",  # Current directory
            "../data/*.json",  # Parent directory
            os.path.join(os.path.dirname(__file__), "..", "data", "*.json")  # Relative to agent folder
        ]
        
        city_files = []
        for path_pattern in data_paths:
            city_files = glob.glob(path_pattern)
            if city_files:
                break
        
        logger.info(f"🔍 Found city files: {city_files}")
        available_cities = {}
        
        for file_path in city_files:
            # Extract city name from filename (remove .json extension)
            city_name = os.path.basename(file_path).replace('.json', '').lower()
            available_cities[city_name] = file_path
            logger.info(f"🔍 Added city: '{city_name}' -> '{file_path}'")
            
        logger.info(f"🔍 Available cities: {available_cities}")
        return available_cities
    
    def extract_city_simple_matching(user_input):
        """Use simple string matching to find city from user input"""
        
        # Scan data/ folder to find available cities
        available_cities = discover_available_cities()
        supported_cities = list(available_cities.keys())
        
        if not available_cities:
            return None, None, 0.0, []
        
        logger.info(f"🔍 Simple matching for input: '{user_input}'")
        user_input_lower = user_input.lower()
        
        # Find exact match first
        for city_name in supported_cities:
            if city_name in user_input_lower:
                logger.info(f"✅ Simple match found: '{city_name}' in '{user_input}'")
                return city_name, available_cities[city_name], 1.0, supported_cities
        
        logger.info(f"❌ No city match found in input: '{user_input}'")
        return None, None, 0.0, supported_cities
    
    # Extract city with simple matching
    logger.info(f"🔍 search_places_in_city called with city='{city}', categories='{categories}'")
    matched_city, city_file, confidence, supported_cities = extract_city_simple_matching(city)
    logger.info(f"🔍 City matching result: matched_city='{matched_city}', confidence={confidence}")
    
    if not matched_city:
        return json.dumps({
            "error": f"Cannot identify city from '{city}'. Currently supported: {', '.join(supported_cities)}",
            "supported_cities": supported_cities,
            "suggestion": "Please be more specific about the city name",
            "available_data_files": len(supported_cities)
        }, ensure_ascii=False)
    
    try:
        with open(city_file, "r", encoding="utf-8") as f:
            places_data = json.load(f)
    except FileNotFoundError:
        return json.dumps({
            "error": f"Data file not found for {matched_city}: {city_file}",
            "supported_cities": supported_cities
        }, ensure_ascii=False)
    
    # Use all data from file (no coordinate filtering)
    valid_places = places_data
    
    if categories:
        logger.info(f"🔍 Categories string: '{categories}'")
        # Filter by categories
        category_list = [cat.strip().lower() for cat in categories.split(",")]
        logger.info(f"🔍 Category list: {category_list}")
        filtered_places = []
        
        for place in valid_places:
            place_category = place.get("category", "").lower()
            # Check if place category contains any keyword
            for keyword in category_list:
                if keyword in place_category:
                    logger.info(f"✅ Found match: '{place_category}' contains '{keyword}'")
                    filtered_places.append(place)
                    break
        
        logger.info(f"🔍 Filtered places count: {len(filtered_places)}")
        valid_places = filtered_places
    
    # Sort by rating (highest first)
    valid_places.sort(key=lambda x: x.get("rating", 0) or 0, reverse=True)
    
    return json.dumps({
        "city": city,
        "matched_city": matched_city,
        "confidence": round(confidence, 2),
        "categories_searched": categories if categories else "all",
        "total_found": len(valid_places),
        "top_places": valid_places
    }, ensure_ascii=False, indent=2)

def calculate_distance(lat1, lon1, lat2, lon2):
    """Calculate distance between 2 points using Haversine formula (km)"""
    if not all([lat1, lon1, lat2, lon2]):
        return float('inf')
    
    R = 6371  # Earth's radius (km)
    
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    
    a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon/2)**2
    c = 2 * math.asin(math.sqrt(a))
    
    return R * c

def cluster_places_by_distance(places, max_distance_km=5):
    """Group places by proximity distance"""
    if not places:
        return []
    
    clusters = []
    unassigned = places.copy()
    
    while unassigned:
        # Create new cluster with first place
        current_cluster = [unassigned.pop(0)]
        
        # Find places near current cluster
        i = 0
        while i < len(unassigned):
            place = unassigned[i]
            
            # Check distance with all places in cluster
            min_distance = min([
                calculate_distance(
                    place.get('latitude'), place.get('longitude'),
                    cluster_place.get('latitude'), cluster_place.get('longitude')
                )
                for cluster_place in current_cluster
            ])
            
            if min_distance <= max_distance_km:
                current_cluster.append(unassigned.pop(i))
            else:
                i += 1
        
        clusters.append(current_cluster)
    
    return clusters

@tool
def create_travel_itinerary(city: str, days: int, interests: str = None, budget: str = None, group_size: int = 1) -> str:
    """
    Create detailed travel itinerary for a city with places grouped by distance.
    
    Args:
        city: City name
        days: Number of travel days
        interests: Travel interests (e.g. "food, hiking, culture")
        budget: Budget reference (e.g. "3000$", "budget friendly")
        group_size: Number of people in group
    
    Returns:
        JSON string containing detailed daily travel itinerary
    """
    
    # Define categories based on interests (supports both Vietnamese and English)
    interest_mapping = {
        'food': ['restaurants', 'cafes', 'night markets'],
        'nature': ['parks', 'mountains', 'waterfalls', 'lakes', 'beaches', 'gardens'],
        'hiking': ['mountains', 'hiking trails', 'viewpoints'],
        'mountain climbing': ['mountains', 'hiking trails', 'viewpoints'],
        'culture': ['temples', 'museums', 'historical sites', 'art galleries'],
        'history': ['museums', 'historical sites', 'temples'],
        'shop': ['shopping malls', 'night markets', 'souvenir counters'],
        'adventure': ['hiking trails', 'ecotourism areas', 'adventure parks', 'hot springs'],
        'entertainment': ['theaters', 'zoo', 'viewpoints', 'swimming pools'],
    }
    
    if interests:
        logger.info(f"🔍 Processing interests: {interests}")
        interest_list = [i.strip().lower() for i in interests.split(',')]
        logger.info(f"🔍 Interest list: {interest_list}")
        categories = []
        
        for interest in interest_list:
            logger.info(f"🔍 Processing interest: '{interest}'")
            # Check for exact matches first
            if interest in interest_mapping:
                logger.info(f"✅ Exact match found for '{interest}': {interest_mapping[interest]}")
                categories.extend(interest_mapping[interest])
            else:
                # Check for partial matches
                found_match = False
                for key, values in interest_mapping.items():
                    if interest in key or key in interest:
                        logger.info(f"✅ Partial match found: '{interest}' matches '{key}': {values}")
                        categories.extend(values)
                        found_match = True
                
                # If no partial match, try word-by-word matching
                if not found_match:
                    words = interest.split()
                    for word in words:
                        for key, values in interest_mapping.items():
                            if word in key or key in word:
                                logger.info(f"✅ Word match found: '{word}' from '{interest}' matches '{key}': {values}")
                                categories.extend(values)
                                found_match = True
                
                if not found_match:
                    logger.info(f"❌ No match found for interest: '{interest}'")
        
        # Add basic categories and enhance food search
        categories.extend(['tourist attractions', 'restaurants', 'cafes'])
        # If user has food interests, add more restaurant types
        food_keywords = ['food', 'cuisine', 'dining']
        if any(food_interest in interests.lower() for food_interest in food_keywords):
            categories.extend(['night markets', 'cafes', 'restaurants'])
        categories = list(set(categories))  # Remove duplicates
        logger.info(f"🔍 Final categories: {categories}")
    else:
        # Default categories for general travel plan
        categories = [
            'tourist attractions', 'restaurants', 'parks', 'temples', 
            'museums', 'shopping malls', 'viewpoints', 'cafes'
        ]
    
    # Search for places using new tool
    categories_str = ','.join(categories)
    search_result = query_city_data.invoke({"city": city, "categories": categories_str})
    
    try:
        search_data = json.loads(search_result)
        all_places = search_data.get('places', [])  # Changed from 'top_places' to 'places'
    except:
        return json.dumps({"error": "Cannot find places", "details": search_result}, ensure_ascii=False)
    
    if not all_places:
        return json.dumps({"error": "No places found"}, ensure_ascii=False)
    
    # If not enough places from specific interests, add more from all categories
    if len(all_places) < days * 2:  # Need at least 2 places per day
        logger.info(f"⚠️ Only {len(all_places)} places from interests. Adding more tourist spots...")
        
        # Danh sách các categories phổ biến để tìm thêm
        additional_categories = [
            "tourist attractions", "viewpoints", "museums", "temples", 
            "parks", "shopping", "entertainment", "cultural sites"
        ]
        
        for category in additional_categories:
            if len(all_places) >= days * 4:  # Stop when we have 4 places per day
                break
                
            additional_search = query_city_data.invoke({"city": city, "categories": category})
            try:
                additional_data = json.loads(additional_search)
                additional_places = additional_data.get('places', [])
                
                # Loại bỏ trùng lặp và thêm vào
                existing_titles = {place.get('title') for place in all_places}
                for place in additional_places:
                    if place.get('title') not in existing_titles:
                        all_places.append(place)
                        existing_titles.add(place.get('title'))
                        
            except:
                continue
        
        # Nếu vẫn chưa đủ, lấy tất cả dữ liệu
        if len(all_places) < days * 2:
            all_search = query_city_data.invoke({"city": city, "categories": ""})
            try:
                all_data = json.loads(all_search)
                all_available_places = all_data.get('places', [])
                
                existing_titles = {place.get('title') for place in all_places}
                for place in all_available_places:
                    if place.get('title') not in existing_titles:
                        all_places.append(place)
                        
            except:
                pass
        
        logger.info(f"✅ Added more places. Total: {len(all_places)} places")
    
    # Group places by distance
    clusters = cluster_places_by_distance(all_places, max_distance_km=5)  # Increase radius for more clusters
    
    # Sort clusters by number of places (prioritize clusters with more places)
    clusters = sorted(clusters, key=len, reverse=True)
    
    # If still not enough clusters, create more small clusters from remaining places
    while len(clusters) < days and len(all_places) > sum(len(cluster) for cluster in clusters):
        remaining_places = []
        used_places = {place['title'] for cluster in clusters for place in cluster}
        for place in all_places:
            if place.get('title') not in used_places:
                remaining_places.append(place)
        
        if remaining_places:
            # Create new cluster from remaining places
            new_clusters = cluster_places_by_distance(remaining_places, max_distance_km=10)
            clusters.extend(new_clusters[:days - len(clusters)])
        else:
            break
    
    # Create itinerary in standard JSON format
    itinerary = {
        "trip_info": {
            "destination": city,
            "duration": f"{days} days",
            "group_size": group_size,
            "budget": budget,
            "interests": interests
        },
        "itinerary": {},
        "notes": [
            f"Itinerary optimized for {group_size} people",
            "Only create itinerary for days with sufficient place data",
            "Places within same day are grouped nearby to save travel time",
            "Recommend booking reservations for high-end restaurants",
            "Bring offline maps and cash"
        ]
    }
    
    # Create pool of unused places
    available_places = all_places.copy()
    used_places = set()
    
    for day in range(1, days + 1):
        day_key = f"day_{day}"
        
        # Filter unused places in pool
        remaining_places = [p for p in available_places if p.get('title') not in used_places]
        
        if not remaining_places:
            break
            
        # Choose 1 morning activity first (prioritize attractions with high rating)
        attractions = [p for p in remaining_places if p.get('category') in ['tourist attractions', 'hiking trails', 'mountains', 'viewpoints', 'temples', 'parks']]
        restaurants = [p for p in remaining_places if p.get('category') in ['restaurants']]
        cafes = [p for p in remaining_places if p.get('category') in ['cafes']]
        
        # Sort by rating
        attractions.sort(key=lambda x: x.get('rating', 0) or 0, reverse=True)
        restaurants.sort(key=lambda x: x.get('rating', 0) or 0, reverse=True)
        cafes.sort(key=lambda x: x.get('rating', 0) or 0, reverse=True)
        
        # Choose morning activity (required)
        morning_activity = None
        if attractions:
            morning_activity = attractions[0]
        elif remaining_places:
            morning_activity = max(remaining_places, key=lambda x: x.get('rating', 0) or 0)
        
        if not morning_activity:
            break
            
        used_places.add(morning_activity.get('title'))
        
        # Find places near morning activity (within 5km radius)
        nearby_places = []
        morning_lat = morning_activity.get('latitude')
        morning_lng = morning_activity.get('longitude')
        
        for place in remaining_places:
            if place.get('title') in used_places:
                continue
                
            distance = calculate_distance(
                morning_lat, morning_lng,
                place.get('latitude'), place.get('longitude')
            )
            if distance <= 5:  # 5km radius
                nearby_places.append(place)
        
        # Categorize nearby places
        nearby_restaurants = [p for p in nearby_places if p.get('category') in ['restaurants']]
        nearby_cafes = [p for p in nearby_places if p.get('category') in ['cafes']]
        nearby_attractions = [p for p in nearby_places if p.get('category') in ['tourist attractions', 'hiking trails', 'mountains', 'viewpoints', 'temples', 'parks']]
        
        # Sort by rating
        nearby_restaurants.sort(key=lambda x: x.get('rating', 0) or 0, reverse=True)
        nearby_cafes.sort(key=lambda x: x.get('rating', 0) or 0, reverse=True)
        nearby_attractions.sort(key=lambda x: x.get('rating', 0) or 0, reverse=True)
        
        # Create full schedule for the day
        day_schedule = {}
        
        # Morning (required)
        day_schedule["morning"] = {
            "activity": f"Visit {morning_activity['title']}",
            "location": morning_activity['address'],
            "coordinates": {
                "latitude": morning_activity['latitude'],
                "longitude": morning_activity['longitude']
            },
            "rating": morning_activity['rating'],
            "category": morning_activity['category'],
            "time": "09:00-12:00"
        }
        
        # Lunch (prioritize restaurants, then cafes)
        lunch_activity = None
        if nearby_restaurants:
            lunch_activity = nearby_restaurants[0]
        elif nearby_cafes:
            lunch_activity = nearby_cafes[0]
        elif restaurants and restaurants[0].get('title') not in used_places:
            lunch_activity = restaurants[0]
        elif cafes and cafes[0].get('title') not in used_places:
            lunch_activity = cafes[0]
            
        if lunch_activity:
            used_places.add(lunch_activity.get('title'))
            day_schedule["lunch"] = {
                "activity": f"Lunch at {lunch_activity['title']}",
                "location": lunch_activity['address'],
                "coordinates": {
                    "latitude": lunch_activity['latitude'],
                    "longitude": lunch_activity['longitude']
                },
                "rating": lunch_activity['rating'],
                "category": lunch_activity['category'],
                "time": "12:00-13:30"
            }
        
        # Afternoon (prioritize nearby attractions, different from morning)
        afternoon_activity = None
        if nearby_attractions:
            afternoon_activity = nearby_attractions[0]
        elif len(attractions) > 1 and attractions[1].get('title') not in used_places:
            afternoon_activity = attractions[1]
        elif remaining_places:
            afternoon_candidates = [p for p in remaining_places if p.get('title') not in used_places]
            if afternoon_candidates:
                afternoon_activity = max(afternoon_candidates, key=lambda x: x.get('rating', 0) or 0)
                
        if afternoon_activity:
            used_places.add(afternoon_activity.get('title'))
            day_schedule["afternoon"] = {
                "activity": f"Visit {afternoon_activity['title']}",
                "location": afternoon_activity['address'],
                "coordinates": {
                    "latitude": afternoon_activity['latitude'],
                    "longitude": afternoon_activity['longitude']
                },
                "rating": afternoon_activity['rating'],
                "category": afternoon_activity['category'],
                "time": "14:00-17:00"
            }
        
        # Dinner (prioritize nearby restaurants, different from lunch)
        dinner_activity = None
        dinner_candidates = [p for p in nearby_restaurants if p.get('title') not in used_places]
        if not dinner_candidates:
            dinner_candidates = [p for p in restaurants if p.get('title') not in used_places]
        if not dinner_candidates:
            dinner_candidates = [p for p in nearby_cafes if p.get('title') not in used_places]
        if not dinner_candidates:
            dinner_candidates = [p for p in cafes if p.get('title') not in used_places]
            
        if dinner_candidates:
            dinner_activity = dinner_candidates[0]
            used_places.add(dinner_activity.get('title'))
            day_schedule["dinner"] = {
                "activity": f"Dinner at {dinner_activity['title']}",
                "location": dinner_activity['address'],
                "coordinates": {
                    "latitude": dinner_activity['latitude'],
                    "longitude": dinner_activity['longitude']
                },
                "rating": dinner_activity['rating'],
                "category": dinner_activity['category'],
                "time": "18:00-20:00"
            }
        
        # Always add day to itinerary (has at least morning)
        itinerary["itinerary"][day_key] = day_schedule
    
    # Update actual number of days created
    actual_days = len(itinerary["itinerary"])
    itinerary["trip_info"]["duration"] = f"{actual_days} days (requested: {days} days)"
    
    if actual_days < days:
        itinerary["notes"].append(f"⚠️ Only created {actual_days}/{days} days due to insufficient place data")
    
    # Save itinerary to file
    output_file = f"{city.replace(' ', '_').lower()}_itinerary_{actual_days}days.json"
    try:
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(itinerary, f, ensure_ascii=False, indent=4)
        itinerary["notes"].append(f"Detailed itinerary saved to file: {output_file}")
    except:
        pass
    
    return json.dumps(itinerary, ensure_ascii=False, indent=2)

class TravelAgent:
    def __init__(self):
        """Initialize Travel Agent with LLM and tools"""
        self.llm = None
        self.agent_executor = None
        self.runnable = None
        self.tools = [query_city_data, search_places_in_city, create_travel_itinerary]
        self._initialize_llm()
        self._create_agent()

    def _initialize_llm(self):
        """Initialize Google Generative AI LLM"""
        try:
            # Check for API key
            if "GOOGLE_API_KEY" not in os.environ:
                raise ValueError("GOOGLE_API_KEY not found in environment variables")
            
            self.llm = ChatGoogleGenerativeAI(
                model="gemini-2.5-flash",
                temperature=0,
                max_tokens=None,
                timeout=None,
                max_retries=2,
            )
            logger.info("LLM initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize LLM: {e}")
            raise

    def _create_agent(self):
        """Create the agent with prompt and tools"""
        try:
            # Create prompt
            prompt = ChatPromptTemplate.from_messages([
                ("system", """You are a helpful travel assistant with access to places search and travel planning tools. 

                SUPPORTED CITIES: You can provide information for cities that have data files in the data/ folder. The system will automatically discover available cities.

                When users ask about places to visit, attractions, restaurants, or any location-based questions about a city, 
                use the search_places_in_city tool to find relevant information.
                
                When users ask to create a travel plan/itinerary (planning a trip, create itinerary, plan trip), 
                ALWAYS use the create_travel_itinerary tool regardless of interests or activities mentioned. 
                The tool will handle all interest matching and find the best available places.
                
                CRITICAL RULES:
                1. ALWAYS attempt to create travel plans when requested - DO NOT refuse based on interests
                2. Return ONLY the JSON from create_travel_itinerary tool
                3. DO NOT wrap it in markdown code blocks
                4. DO NOT add explanatory text before or after
                5. DO NOT reformat or modify the JSON structure
                6. DO NOT add fake data or activities not in the tool output
                7. If create_travel_itinerary returns an error, show that error to user"""),
                MessagesPlaceholder(variable_name="chat_history"),
                ("human", "{input}"),
                MessagesPlaceholder(variable_name="agent_scratchpad"),
            ])

            # Create agent
            agent = create_tool_calling_agent(self.llm, self.tools, prompt)
            self.agent_executor = AgentExecutor(agent=agent, tools=self.tools, verbose=False)

            # Create runnable with history
            self.runnable = RunnableWithMessageHistory(
                self.agent_executor,
                get_session_history=self._get_history,
                input_messages_key="input",
                history_messages_key="chat_history",
            )
            
            logger.info("Agent created successfully")
        except Exception as e:
            logger.error(f"Failed to create agent: {e}")
            raise

    def _get_history(self, session_id: str):
        """Get chat history for session"""
        try:
            return RedisChatMessageHistory(
                session_id=session_id,
                url="redis://nghiapd-rasp.kiko-acrux.ts.net:32769/0"
            )
        except:
            # Fallback to in-memory history if Redis is not available
            logger.warning("Redis not available, using in-memory history")
            from langchain_community.chat_message_histories import ChatMessageHistory
            return ChatMessageHistory()

    def process_query(self, query: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Process user query and return response
        
        Args:
            query: User input query
            session_id: Optional session ID for chat history
            
        Returns:
            Dictionary with response data
        """
        try:
            if not session_id:
                session_id = str(uuid.uuid4())
            
            config = {"configurable": {"session_id": session_id}}
            
            result = self.runnable.invoke({"input": query}, config=config)
            output = result.get("output", "")
            
            # Try to parse as JSON if it looks like itinerary output
            if query.lower().__contains__("itinerary") or query.lower().__contains__("plan"):
                try:
                    # Try to extract JSON from output
                    import re
                    json_match = re.search(r'(\{.*\})', output, re.DOTALL)
                    if json_match:
                        json_data = json.loads(json_match.group(1))
                        return {
                            "success": True,
                            "type": "itinerary",
                            "data": json_data,
                            "session_id": session_id
                        }
                except:
                    pass
            
            return {
                "success": True,
                "type": "text",
                "data": output,
                "session_id": session_id
            }
            
        except Exception as e:
            logger.error(f"Error processing query: {e}")
            return {
                "success": False,
                "error": str(e),
                "session_id": session_id or "unknown"
            }

    def search_places(self, city: str, categories: str = "") -> Dict[str, Any]:
        """
        Direct search for places in a city
        
        Args:
            city: City name
            categories: Categories to search for
            
        Returns:
            Dictionary with places data
        """
        try:
            result = search_places_in_city.invoke({"city": city, "categories": categories})
            return {
                "success": True,
                "data": json.loads(result)
            }
        except Exception as e:
            logger.error(f"Error searching places: {e}")
            return {
                "success": False,
                "error": str(e)
            }

    def create_itinerary(self, city: str, days: int, interests: str = None, budget: str = None, group_size: int = 1) -> Dict[str, Any]:
        """
        Direct itinerary creation
        
        Args:
            city: City name
            days: Number of days
            interests: Travel interests
            budget: Budget information
            group_size: Number of people
            
        Returns:
            Dictionary with itinerary data
        """
        try:
            result = create_travel_itinerary.invoke({
                "city": city,
                "days": days,
                "interests": interests,
                "budget": budget,
                "group_size": group_size
            })
            return {
                "success": True,
                "data": json.loads(result)
            }
        except Exception as e:
            logger.error(f"Error creating itinerary: {e}")
            return {
                "success": False,
                "error": str(e)
            }

# For backward compatibility - can still run as standalone script
if __name__ == "__main__":
    # Interactive mode when run directly
    agent = TravelAgent()
    session_id = str(uuid.uuid4())
    logger.info(f"🔑 Session ID: {session_id}")
    
    while True:
        text = input("You: ")
        if text.lower() in ["exit", "quit"]:
            break
        
        try:
            result = agent.process_query(text, session_id)
            
            if result.get("success"):
                if result.get("type") == "itinerary":
                    # Pretty print JSON for itinerary
                    logger.info(json.dumps(result["data"], ensure_ascii=False, indent=2))
                else:
                    logger.info("Assistant:", result["data"])
            else:
                logger.info(f"❌ Lỗi: {result.get('error')}")
        except Exception as e:
            logger.info(f"❌ Lỗi: {e}")
            logger.info("Vui lòng thử lại.")
