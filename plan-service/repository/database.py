import os
import logging
from pymongo.mongo_client import MongoClient
from pymongo.server_api import ServerApi
from dotenv import load_dotenv
from bson.objectid import ObjectId
from datetime import datetime

load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class Database:
    """MongoDB database connection and operations"""

    def __init__(self):
        self.client = None
        self.db = None
        self._connect()

    def _connect(self):
        """Connect to MongoDB"""
        try:
            uri = os.getenv("MONGODB_URI")
            if not uri:
                raise ValueError("MONGODB_URI not found in environment variables")

            # Create a new client and connect to the server
            self.client = MongoClient(uri, server_api=ServerApi('1'))

            # Send a ping to confirm a successful connection
            self.client.admin.command('ping')
            logger.info("Successfully connected to MongoDB!")

            # Get database name from URI
            # URI format: mongodb+srv://username:password@cluster.mongodb.net/database_name?...
            db_name = uri.split('/')[-1].split('?')[0]
            self.db = self.client[db_name]
            logger.info(f"Connected to database: {db_name}")

        except Exception as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise

    def get_collection(self, collection_name: str):
        """Get a collection from the database"""
        if self.db is None:
            raise ConnectionError("Database not connected")
        return self.db[collection_name]

    def get_plan_service_collection(self):
        """Get plan-service collection"""
        return self.get_collection("plan-service")

    def get_user_preferences_collection(self):
        """Get user preferences collection"""
        return self.get_collection("user_preferences")

    def get_search_history_collection(self):
        """Get search history collection"""
        return self.get_collection("search_history")

    def get_bookings_collection(self):
        """Get bookings collection"""
        return self.get_collection("bookings")

    def close(self):
        """Close the database connection"""
        if self.client:
            self.client.close()
            logger.info("MongoDB connection closed")

    def ping(self):
        """Ping the database to check connection"""
        try:
            self.client.admin.command('ping')
            return True
        except Exception as e:
            logger.error(f"Database ping failed: {e}")
            return False

    def save_itinerary(self, user_id: str, itinerary_data: dict) -> str:
        """Save a travel itinerary to MongoDB"""
        try:
            itinerary = {
                "user_id": user_id,
                "itinerary_data": itinerary_data,
                "created_at": datetime.utcnow(),
                "updated_at": datetime.utcnow()
            }
            result = self.get_plan_service_collection().insert_one(itinerary)
            return str(result.inserted_id)
        except Exception as e:
            raise Exception(f"Failed to save itinerary: {str(e)}")

    def get_user_itineraries(self, user_id: str, limit: int = 10) -> list:
        """Get user's saved itineraries"""
        try:
            itineraries = list(self.get_plan_service_collection().find(
                {"user_id": user_id}
            ).sort("created_at", -1).limit(limit))
            
            # Convert ObjectId to string for JSON serialization
            for itinerary in itineraries:
                itinerary["_id"] = str(itinerary["_id"])
            
            return itineraries
        except Exception as e:
            raise Exception(f"Failed to get user itineraries: {str(e)}")

    def save_user_preferences(self, user_id: str, preferences: dict) -> bool:
        """Save or update user preferences"""
        try:
            result = self.get_user_preferences_collection().update_one(
                {"user_id": user_id},
                {
                    "$set": {
                        "preferences": preferences,
                        "updated_at": datetime.utcnow()
                    },
                    "$setOnInsert": {
                        "created_at": datetime.utcnow()
                    }
                },
                upsert=True
            )
            return result.acknowledged
        except Exception as e:
            raise Exception(f"Failed to save user preferences: {str(e)}")

    def get_user_preferences(self, user_id: str) -> dict:
        """Get user preferences"""
        try:
            preferences = self.get_user_preferences_collection().find_one({"user_id": user_id})
            if preferences:
                preferences["_id"] = str(preferences["_id"])
                return preferences
            return {}
        except Exception as e:
            raise Exception(f"Failed to get user preferences: {str(e)}")

    def save_search_history(self, user_id: str, search_type: str, search_data: dict) -> str:
        """Save search history"""
        try:
            history = {
                "user_id": user_id,
                "search_type": search_type,  # "flight" or "hotel"
                "search_data": search_data,
                "created_at": datetime.utcnow()
            }
            result = self.get_search_history_collection().insert_one(history)
            return str(result.inserted_id)
        except Exception as e:
            raise Exception(f"Failed to save search history: {str(e)}")

    def get_user_search_history(self, user_id: str, search_type: str = None, limit: int = 20) -> list:
        """Get user's search history"""
        try:
            query = {"user_id": user_id}
            if search_type:
                query["search_type"] = search_type
            
            history = list(self.get_search_history_collection().find(query)
                         .sort("created_at", -1).limit(limit))
            
            # Convert ObjectId to string
            for item in history:
                item["_id"] = str(item["_id"])
            
            return history
        except Exception as e:
            raise Exception(f"Failed to get search history: {str(e)}")

    def save_booking(self, user_id: str, booking_type: str, booking_data: dict) -> str:
        """Save a booking (flight or hotel)"""
        try:
            booking = {
                "user_id": user_id,
                "booking_type": booking_type,  # "flight" or "hotel"
                "booking_data": booking_data,
                "status": "confirmed",  # confirmed, cancelled, pending
                "created_at": datetime.utcnow(),
                "updated_at": datetime.utcnow()
            }
            result = self.get_bookings_collection().insert_one(booking)
            return str(result.inserted_id)
        except Exception as e:
            raise Exception(f"Failed to save booking: {str(e)}")

    def get_user_bookings(self, user_id: str, booking_type: str = None, status: str = None) -> list:
        """Get user's bookings"""
        try:
            query = {"user_id": user_id}
            if booking_type:
                query["booking_type"] = booking_type
            if status:
                query["status"] = status
            
            bookings = list(self.get_bookings_collection().find(query)
                          .sort("created_at", -1))
            
            # Convert ObjectId to string
            for booking in bookings:
                booking["_id"] = str(booking["_id"])
            
            return bookings
        except Exception as e:
            raise Exception(f"Failed to get user bookings: {str(e)}")

    def update_booking_status(self, booking_id: str, status: str) -> bool:
        """Update booking status"""
        try:
            result = self.get_bookings_collection().update_one(
                {"_id": ObjectId(booking_id)},
                {
                    "$set": {
                        "status": status,
                        "updated_at": datetime.utcnow()
                    }
                }
            )
            return result.modified_count > 0
        except Exception as e:
            raise Exception(f"Failed to update booking status: {str(e)}")

    def delete_itinerary(self, itinerary_id: str, user_id: str) -> bool:
        """Delete a user's itinerary"""
        try:
            result = self.get_plan_service_collection().delete_one({
                "_id": ObjectId(itinerary_id),
                "user_id": user_id
            })
            return result.deleted_count > 0
        except Exception as e:
            raise Exception(f"Failed to delete itinerary: {str(e)}")

    def get_itinerary_by_id(self, itinerary_id: str, user_id: str) -> dict:
        """Get a specific itinerary by ID"""
        try:
            itinerary = self.get_plan_service_collection().find_one({
                "_id": ObjectId(itinerary_id),
                "user_id": user_id
            })
            if itinerary:
                itinerary["_id"] = str(itinerary["_id"])
                return itinerary
            return {}
        except Exception as e:
            raise Exception(f"Failed to get itinerary: {str(e)}")

    def get_total_plans(self) -> int:
        """Get total number of plans in the system"""
        try:
            collection = self.get_plan_service_collection()
            count = collection.count_documents({})
            logger.info(f"Total plans in collection 'plan-service': {count}")
            
            # Also log collection names to verify
            all_collections = self.db.list_collection_names()
            logger.info(f"Available collections: {all_collections}")
            
            return count
        except Exception as e:
            logger.error(f"Failed to get total plans: {str(e)}")
            raise Exception(f"Failed to get total plans: {str(e)}")

# Global database instance
db_instance = None

def get_database():
    """Get the global database instance (singleton pattern)"""
    global db_instance
    if db_instance is None:
        db_instance = Database()
    return db_instance

def get_collection(collection_name: str):
    """Get a collection from the database"""
    db = get_database()
    return db.get_collection(collection_name)

# Collections
def get_itineraries_collection():
    """Get itineraries collection"""
    return get_collection("itineraries")

def get_user_preferences_collection():
    """Get user preferences collection"""
    return get_collection("user_preferences")

def get_search_history_collection():
    """Get search history collection"""
    return get_collection("search_history")

def get_flight_bookings_collection():
    """Get flight bookings collection"""
    return get_collection("flight_bookings")

def get_hotel_bookings_collection():
    """Get hotel bookings collection"""
    return get_collection("hotel_bookings")

# Test connection
if __name__ == "__main__":
    try:
        db = get_database()
        print("✅ MongoDB connection successful!")

        # Test collections
        itineraries = get_itineraries_collection()
        print(f"✅ Itineraries collection: {itineraries.name}")

        user_prefs = get_user_preferences_collection()
        print(f"✅ User preferences collection: {user_prefs.name}")

        search_history = get_search_history_collection()
        print(f"✅ Search history collection: {search_history.name}")

        flight_bookings = get_flight_bookings_collection()
        print(f"✅ Flight bookings collection: {flight_bookings.name}")

        hotel_bookings = get_hotel_bookings_collection()
        print(f"✅ Hotel bookings collection: {hotel_bookings.name}")

    except Exception as e:
        print(f"❌ MongoDB connection failed: {e}")