package com.example.user_service.service;

import com.example.user_service.entity.User;
import com.example.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(UUID id) {
        logger.info("Fetching user with ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        logger.info("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Update all user fields (create if not exists)
     */
    public boolean updateAll(UUID id, String firstName, String lastName, String language, String region,
            Boolean isPremium, Boolean isAddCreditCard, String city) {
        logger.info(
                "Updating all fields for user ID: {} to firstName: {}, lastName: {}, language: {}, region: {}, isPremium: {}, isAddCreditCard: {}, city: {}",
                id, firstName, lastName, language, region, isPremium, isAddCreditCard, city);

        if (!userRepository.existsById(id)) {
            logger.info("User with ID: {} not found, creating new user", id);
            // Create new user
            User newUser = new User();
            newUser.setId(id);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setLanguage(language != null ? language : "en");
            newUser.setRegion(region != null ? region : "UTC");
            newUser.setIsPremium(isPremium != null ? isPremium : false);
            newUser.setIsAddCreditCard(isAddCreditCard != null ? isAddCreditCard : false);
            newUser.setCity(city);

            try {
                userRepository.save(newUser);
                logger.info("Successfully created new user with ID: {}", id);
                return true;
            } catch (Exception e) {
                logger.error("Failed to create new user with ID: {}", id, e);
                return false;
            }
        }

        int updatedRows = userRepository.updateAllById(id, firstName, lastName, language, region, isPremium,
                isAddCreditCard, city);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated all fields for user ID: {}", id);
        } else {
            logger.error("Failed to update all fields for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's first name
     */
    public boolean updateFirstName(UUID id, String firstName) {
        logger.info("Updating first name for user ID: {} to: {}", id, firstName);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateFirstNameById(id, firstName);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated first name for user ID: {}", id);
        } else {
            logger.error("Failed to update first name for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's last name
     */
    public boolean updateLastName(UUID id, String lastName) {
        logger.info("Updating last name for user ID: {} to: {}", id, lastName);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateLastNameById(id, lastName);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated last name for user ID: {}", id);
        } else {
            logger.error("Failed to update last name for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's language
     */
    public boolean updateLanguage(UUID id, String language) {
        logger.info("Updating language for user ID: {} to: {}", id, language);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateLanguageById(id, language);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated language for user ID: {}", id);
        } else {
            logger.error("Failed to update language for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's region
     */
    public boolean updateRegion(UUID id, String region) {
        logger.info("Updating region for user ID: {} to: {}", id, region);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateRegionById(id, region);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated region for user ID: {}", id);
        } else {
            logger.error("Failed to update region for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's city
     */
    public boolean updateCity(UUID id, String city) {
        logger.info("Updating city for user ID: {} to: {}", id, city);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateCityById(id, city);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated city for user ID: {}", id);
        } else {
            logger.error("Failed to update city for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's is_premium status
     */
    public boolean updateIsPremium(UUID id, Boolean isPremium) {
        logger.info("Updating is_premium for user ID: {} to: {}", id, isPremium);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateIsPremiumById(id, isPremium);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated is_premium for user ID: {}", id);
        } else {
            logger.error("Failed to update is_premium for user ID: {}", id);
        }

        return success;
    }

    /**
     * Update user's is_add_credit_card status
     */
    public boolean updateIsAddCreditCard(UUID id, Boolean isAddCreditCard) {
        logger.info("Updating is_add_credit_card for user ID: {} to: {}", id, isAddCreditCard);

        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found", id);
            return false;
        }

        int updatedRows = userRepository.updateIsAddCreditCardById(id, isAddCreditCard);
        boolean success = updatedRows > 0;

        if (success) {
            logger.info("Successfully updated is_add_credit_card for user ID: {}", id);
        } else {
            logger.error("Failed to update is_add_credit_card for user ID: {}", id);
        }

        return success;
    }

    /**
     * Check if user exists
     */
    public boolean userExists(UUID id) {
        return userRepository.existsById(id);
    }

    /**
     * Search users by name (first name or last name)
     */
    public List<User> searchUsersByName(String name) {
        logger.info("Searching users by name: {}", name);
        if (name == null || name.trim().isEmpty()) {
            logger.warn("Search name is empty, returning empty list");
            return List.of();
        }
        List<User> users = userRepository.searchByName(name.trim());
        logger.info("Found {} users matching name: {}", users.size(), name);
        return users;
    }

    /**
     * Get total number of users
     */
    public long getTotalUsers() {
        logger.info("Getting total number of users");
        return userRepository.count();
    }
}