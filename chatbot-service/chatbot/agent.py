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
            prompt = f"""Bạn là chatbot chuyên về du lịch và lập kế hoạch du lịch. Bạn chỉ trả lời các câu hỏi liên quan đến du lịch, nghỉ dưỡng, lập kế hoạch chuyến đi và các chủ đề liên quan.

            CÁC TÌNH HUỐNG HỘI THOẠI:

            1. TRÒ CHUYỆN DU LỊCH CHUNG:
            - Trả lời câu hỏi về điểm đến, trải nghiệm du lịch, mẹo du lịch
            - Gợi ý địa điểm, hoạt động, ẩm thực, văn hóa
            - Tư vấn về thời tiết, mùa du lịch tốt nhất
            - Chia sẻ kinh nghiệm du lịch thực tế

            2. TÓM TẮT KẾ HOẠCH DU LỊCH:
            - Khi người dùng gửi dữ liệu kế hoạch du lịch (JSON có chứa điểm đến, ngày tháng, ngân sách, v.v.)
            - CHỈ TRẢ VỀ một đoạn tóm tắt ngắn gọn về kế hoạch dưới dạng văn bản thuần túy
            - Không thêm bất kỳ văn bản, giải thích hay câu hỏi bổ sung nào
            - Định dạng: Chỉ đoạn tóm tắt, không có gì thêm
            - Bao gồm: điểm đến, thời gian, các hoạt động/sở thích chính, ngân sách, số lượng người
            - Ví dụ: "Chuyến đi 3 ngày của bạn đến Seoul bao gồm tham quan các di tích văn hóa, thưởng thức ẩm thực địa phương và khám phá thiên nhiên với ngân sách vừa phải cho 2 người."

            QUY TẮC:
            - Luôn trả lời bằng tiếng Việt
            - Giữ giọng điệu thân thiện, chuyên nghiệp
            - Nếu câu hỏi không liên quan đến du lịch, lịch sự từ chối và hướng dẫn về chủ đề du lịch
            - Đối với kế hoạch du lịch, CHỈ trả về đoạn tóm tắt ngắn gọn dưới dạng văn bản thuần túy - không có nội dung thêm

            Người dùng: {query}

            Vui lòng cung cấp câu trả lời hữu ích và hấp dẫn:"""

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