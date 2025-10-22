import os
import socket
import logging
from typing import Dict, Any
from fastapi import FastAPI
from dotenv import load_dotenv
import py_eureka_client.eureka_client as eureka_client
from prometheus_fastapi_instrumentator import Instrumentator

# Import our ChatAgent
from chatbot.agent import ChatAgent

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Chatbot Service", description="Travel and Tourism Chatbot Service - Specialized in travel planning and tourism advice", version="1.0.0")
load_dotenv()

# Initialize Chat Agent
chat_agent = None

def initialize_chat_agent():
    """Initialize the chat agent on startup"""
    global chat_agent
    try:
        chat_agent = ChatAgent()
        logger.info("Chat agent initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize chat agent: {e}")
        chat_agent = None

@app.on_event("startup")
async def startup_events():
    # Initialize chat agent
    initialize_chat_agent()

    # Register to Eureka
    ip = socket.gethostbyname(socket.gethostname())
    eureka_server = os.getenv(
        "EUREKA_SERVER_URL",
        "http://localhost:8761/eureka/"
    )
    app_name = os.getenv("APPLICATION_NAME", "chatbot-service")
    instance_port = int(os.getenv("SERVER_PORT", "8002"))
    await eureka_client.init_async(
        eureka_server=eureka_server,
        app_name=app_name,
        instance_port=instance_port,
        instance_host=ip
    )

@app.get("/")
def root():
    return {
        "message": "Travel & Tourism Chatbot Service is running",
        "service": "chatbot-service",
        "version": "1.0.0",
        "specialization": "Travel planning, tourism advice, and destination recommendations"
    }

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "chatbot-service",
        "agent_status": "initialized" if chat_agent else "not_initialized"
    }

@app.get("/hello")
def read_hello():
    return {"message": "Hello from chatbot service"}

@app.post("/chat")
def chat_with_agent(request: Dict[str, Any]):
    """
    Chat with the Travel & Tourism AI Agent

    This endpoint allows users to chat with an AI assistant specialized in:
    - Travel planning and advice
    - Tourism recommendations
    - Destination information
    - Travel itinerary discussions

    The agent handles two conversation scenarios:
    1. General travel chat and questions
    2. Travel plan analysis and discussion (summarizes plans and focuses conversation within them)

    Args:
        request: JSON object with "message" field and optional "plan_content" field

    Returns:
        AI response focused on travel and tourism topics
    """
    if not chat_agent:
        return {"error": "Chat agent not initialized"}

    try:
        import json
        
        message = request.get("message", "")
        plan_content = request.get("plan_content", None)
        
        logger.info(f"Received request - message: {message}, has plan_content: {plan_content is not None}")
        
        # Check if plan_content exists (travel plan scenario)
        if plan_content and isinstance(plan_content, dict):
            logger.info(f"Processing travel plan with content: {plan_content}")
            # Send plan_content as JSON string for summarization
            response = chat_agent.chat(json.dumps(plan_content, ensure_ascii=False))
        elif message:
            # Regular message scenario
            logger.info(f"Processing regular message: {message}")
            
            # Check if message itself is a dict (backwards compatibility)
            if isinstance(message, dict):
                travel_indicators = ['destination', 'startDate', 'endDate', 'budget', 'plan_id', 'selectedInterests']
                has_travel_data = any(key in message for key in travel_indicators)
                
                if has_travel_data:
                    logger.info("Message contains travel plan data")
                    response = chat_agent.chat(json.dumps(message, ensure_ascii=False))
                else:
                    response = chat_agent.chat(str(message))
            else:
                # Try to parse as JSON string
                try:
                    plan_data = json.loads(message)
                    travel_indicators = ['destination', 'startDate', 'endDate', 'budget', 'plan_id', 'selectedInterests']
                    has_travel_data = any(key in plan_data for key in travel_indicators)
                    
                    if has_travel_data:
                        logger.info("Parsed travel plan from JSON string")
                        response = chat_agent.chat(message)
                    else:
                        response = chat_agent.chat(message)
                except (json.JSONDecodeError, TypeError, ValueError):
                    # Not JSON, treat as regular text message
                    logger.info("Processing as regular text message")
                    response = chat_agent.chat(str(message))
        else:
            return {"error": "Either 'message' or 'plan_content' field is required"}

        logger.info(f"Final response: {response[:200]}...")
        return {"response": response}
    except Exception as e:
        logger.error(f"Error in chat endpoint: {e}")
        return {"error": f"Chat service error: {str(e)}"}# Add more endpoints here as needed

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVER_PORT", "8002"))
    uvicorn.run(app, host="0.0.0.0", port=port)
