import os
import uuid
import logging
from typing import Dict, Any, Optional
from langchain_google_genai import ChatGoogleGenerativeAI
from dotenv import load_dotenv

load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ChatAgent:
    def __init__(self):
        """Initialize Travel & Tourism Chat Agent with Gemini LLM"""
        self.llm = None
        self._initialize_llm()

    def _initialize_llm(self):
        """Initialize Google Generative AI LLM for Travel & Tourism Chat"""
        try:
            # Check for API key
            if "GOOGLE_API_KEY" not in os.environ:
                raise ValueError("GOOGLE_API_KEY not found in environment variables")

            self.llm = ChatGoogleGenerativeAI(
                model="gemini-2.5-flash",
                temperature=0.7,
                max_tokens=2048,
                timeout=None,
                max_retries=2,
            )
            logger.info("Travel & Tourism Chat Agent LLM initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize Travel & Tourism Chat Agent LLM: {e}")
            raise

    def process_query(self, query: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Process user query and return response using Gemini for Travel & Tourism topics

        Handles two conversation scenarios:
        1. General travel chat and questions
        2. Travel plan analysis - returns only a concise plain text summary

        Args:
            query: User input query about travel/tourism
            session_id: Optional session ID (for compatibility)

        Returns:
            Dictionary with response data focused on travel topics
        """
        try:
            if not session_id:
                session_id = str(uuid.uuid4())

            # Enhanced prompt for travel and tourism chatbot
            prompt = f"""You are a specialized chatbot for travel and tourism. You only answer questions related to travel, vacation, travel planning, and related topics.

            CONVERSATION SCENARIOS:

            1. GENERAL TRAVEL CHAT:
            - Answer questions about destinations, travel experiences, travel tips
            - Suggest places, activities, food, culture
            - Advise on weather, best travel seasons
            - Share real travel experiences

            2. TRAVEL PLAN SUMMARY:
            - When users send travel plan data (JSON objects with destination, dates, budget, etc.)
            - ONLY RETURN a concise summary of the plan as plain text
            - Do not add any extra text, explanations, or questions
            - Format: Just the summary paragraph, nothing else
            - Cover: destination, duration, key activities/interests, budget, group size
            - Example: "Your 3-day trip to Seoul includes cultural sites, local cuisine, and nature exploration with a moderate budget for 2 people."

            RULES:
            - Always respond in English
            - Maintain friendly, professional tone
            - If question is not related to travel, politely decline and redirect to travel topics
            - For travel plans, return ONLY the concise summary as plain text - no extra content

            User: {query}

            Please provide a helpful and engaging response:"""

            # Get response from Gemini
            response = self.llm.invoke(prompt)

            return {
                "success": True,
                "type": "text",
                "data": response.content,
                "session_id": session_id
            }

        except Exception as e:
            logger.error(f"Error processing query: {e}")
            return {
                "success": False,
                "error": str(e),
                "session_id": session_id or "unknown"
            }

    def chat(self, message: str) -> str:
        """
        Simple chat method for travel and tourism conversations

        For travel plans: returns only a concise plain text summary
        For general chat: provides helpful travel advice and information

        Args:
            message: User's message about travel or tourism

        Returns:
            Chatbot response focused on travel topics
        """
        result = self.process_query(message)
        if result.get("success"):
            return result.get("data", "Sorry, I couldn't generate a response.")
        else:
            return f"Sorry, there was an error: {result.get('error', 'Unknown error')}"

# For testing - can run as standalone script
if __name__ == "__main__":
    # Interactive mode when run directly
    agent = ChatAgent()
    session_id = str(uuid.uuid4())
    logger.info(f"🔑 Session ID: {session_id}")

    print("🤖 Travel & Tourism Chat Agent started! Type 'exit' or 'quit' to stop.")
    print("💡 I specialize in travel planning, tourism advice, and destination recommendations.")
    print("-" * 70)

    while True:
        try:
            text = input("You: ").strip()
            if text.lower() in ["exit", "quit"]:
                print("👋 Travel Assistant signing off!")
                break

            if not text:
                continue

            result = agent.process_query(text, session_id)

            if result.get("success"):
                print(f"🤖 Travel Assistant: {result['data']}")
            else:
                print(f"❌ Error: {result.get('error')}")

            print("-" * 70)

        except KeyboardInterrupt:
            print("\n👋 Travel Assistant signing off!")
            break
        except Exception as e:
            print(f"❌ Unexpected error: {e}")
            print("-" * 70)