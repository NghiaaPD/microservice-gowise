package com.example.user_service.service;

import com.example.user_service.entity.CreditCard;
import com.example.user_service.entity.User;
import com.example.user_service.repository.CreditCardRepository;
import com.example.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditCardService {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardService.class);

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * Thêm thẻ tín dụng mới cho user
     * Tự động set is_add_credit_card = true cho user
     */
    @Transactional
    public CreditCard addCreditCard(UUID userId, String cardNumber, CreditCard.CardType cardType,
            Short expiryMonth, Short expiryYear, Boolean setAsDefault) {

        logger.info("Adding credit card for user: {}", userId);

        // Validate expiry date
        java.time.Year currentYear = java.time.Year.now();
        if (expiryYear < currentYear.getValue()) {
            logger.error("Credit card expired: year {} is before current year {}", expiryYear, currentYear.getValue());
            throw new RuntimeException("Credit card has expired");
        }

        // Kiểm tra user tồn tại
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.error("User not found: {}", userId);
            throw new RuntimeException("User not found");
        }

        // Mã hóa số thẻ (đơn giản - trong thực tế cần encryption mạnh hơn)
        String encryptedCardNumber = encryptCardNumber(cardNumber);
        String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);

        // Tạo thẻ mới
        CreditCard creditCard = new CreditCard(userId, encryptedCardNumber, lastFourDigits,
                cardType, expiryMonth, expiryYear);

        // Nếu đây là thẻ đầu tiên hoặc được yêu cầu set làm default
        long existingCardsCount = creditCardRepository.countByUserIdAndIsActiveTrue(userId);
        if (existingCardsCount == 0 || (setAsDefault != null && setAsDefault)) {
            // Tắt tất cả thẻ default cũ
            creditCardRepository.unsetAllDefaultCardsForUser(userId);
            creditCard.setIsDefault(true);
            logger.info("Setting new card as default for user {}", userId);
        }

        // Lưu thẻ
        CreditCard savedCard = creditCardRepository.save(creditCard);
        logger.info("Credit card added successfully: {} for user {}", savedCard.getId(), userId);

        // **QUAN TRỌNG: Tự động set is_add_credit_card = true cho user**
        boolean updated = userService.updateIsAddCreditCard(userId, true);
        if (updated) {
            logger.info("Successfully updated is_add_credit_card = true for user {}", userId);
        } else {
            logger.warn("Failed to update is_add_credit_card for user {}", userId);
        }

        return savedCard;
    }

    /**
     * Lấy tất cả thẻ của user
     */
    public List<CreditCard> getUserCreditCards(UUID userId) {
        logger.info("Fetching credit cards for user: {}", userId);
        return creditCardRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Lấy thẻ mặc định của user
     */
    public Optional<CreditCard> getDefaultCreditCard(UUID userId) {
        logger.info("Fetching default credit card for user: {}", userId);
        return creditCardRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
    }

    /**
     * Set thẻ làm mặc định
     */
    @Transactional
    public boolean setDefaultCreditCard(UUID userId, UUID cardId) {
        logger.info("Setting card {} as default for user {}", cardId, userId);

        Optional<CreditCard> cardOpt = creditCardRepository.findById(cardId);
        if (cardOpt.isEmpty() || !cardOpt.get().getUserId().equals(userId)) {
            logger.error("Credit card not found or doesn't belong to user: {}", cardId);
            return false;
        }

        // Tắt tất cả thẻ default cũ
        creditCardRepository.unsetAllDefaultCardsForUser(userId);

        // Set thẻ mới làm default
        CreditCard card = cardOpt.get();
        card.setIsDefault(true);
        creditCardRepository.save(card);

        logger.info("Successfully set card {} as default for user {}", cardId, userId);
        return true;
    }

    /**
     * Xóa thẻ (hard delete - xóa vĩnh viễn khỏi database)
     */
    @Transactional
    public boolean deleteCreditCard(UUID userId, UUID cardId) {
        logger.info("Deleting credit card {} for user {}", cardId, userId);

        Optional<CreditCard> cardOpt = creditCardRepository.findById(cardId);
        if (cardOpt.isEmpty() || !cardOpt.get().getUserId().equals(userId)) {
            logger.error("Credit card not found or doesn't belong to user: {}", cardId);
            return false;
        }

        CreditCard card = cardOpt.get();

        // HARD DELETE - xóa thật sự khỏi database
        creditCardRepository.delete(card);
        logger.info("Successfully HARD DELETED credit card {} for user {}", cardId, userId);

        // Kiểm tra nếu không còn thẻ nào, set is_add_credit_card = false
        long remainingCards = creditCardRepository.countByUserId(userId);
        if (remainingCards == 0) {
            boolean updated = userService.updateIsAddCreditCard(userId, false);
            if (updated) {
                logger.info("Updated is_add_credit_card = false for user {} (no cards remaining)", userId);
            }
        }

        return true;
    }

    /**
     * Mã hóa số thẻ (đơn giản - trong thực tế cần encryption mạnh hơn)
     */
    private String encryptCardNumber(String cardNumber) {
        // Đây chỉ là ví dụ đơn giản - trong thực tế cần sử dụng AES hoặc thuật toán mã
        // hóa mạnh
        StringBuilder encrypted = new StringBuilder();
        for (int i = 0; i < cardNumber.length(); i++) {
            char c = cardNumber.charAt(i);
            encrypted.append((char) (c + 3)); // Shift by 3 (very basic)
        }
        return encrypted.toString();
    }
}
