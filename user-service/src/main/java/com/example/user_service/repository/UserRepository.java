package com.example.user_service.repository;

import com.example.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by ID
     */
    @NonNull
    Optional<User> findById(@NonNull UUID id);

    /**
     * Update user's first name
     */
    @Modifying
    @Query("UPDATE User u SET u.firstName = :firstName, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateFirstNameById(@Param("id") UUID id, @Param("firstName") String firstName);

    /**
     * Update user's last name
     */
    @Modifying
    @Query("UPDATE User u SET u.lastName = :lastName, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateLastNameById(@Param("id") UUID id, @Param("lastName") String lastName);

    /**
     * Update user's language
     */
    @Modifying
    @Query("UPDATE User u SET u.language = :language, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateLanguageById(@Param("id") UUID id, @Param("language") String language);

    /**
     * Update user's region
     */
    @Modifying
    @Query("UPDATE User u SET u.region = :region, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateRegionById(@Param("id") UUID id, @Param("region") String region);

    /**
     * Update user's city
     */
    @Modifying
    @Query("UPDATE User u SET u.city = :city, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateCityById(@Param("id") UUID id, @Param("city") String city);

    /**
     * Update user's is_premium status
     */
    @Modifying
    @Query("UPDATE User u SET u.isPremium = :isPremium, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateIsPremiumById(@Param("id") UUID id, @Param("isPremium") Boolean isPremium);

    /**
     * Update user's is_add_credit_card status
     */
    @Modifying
    @Query("UPDATE User u SET u.isAddCreditCard = :isAddCreditCard, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateIsAddCreditCardById(@Param("id") UUID id, @Param("isAddCreditCard") Boolean isAddCreditCard);

    /**
     * Update all user fields
     */
    @Modifying
    @Query("UPDATE User u SET u.firstName = :firstName, u.lastName = :lastName, u.language = :language, u.region = :region, u.isPremium = :isPremium, u.isAddCreditCard = :isAddCreditCard, u.city = :city, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateAllById(@Param("id") UUID id, @Param("firstName") String firstName, @Param("lastName") String lastName,
            @Param("language") String language, @Param("region") String region,
            @Param("isPremium") Boolean isPremium, @Param("isAddCreditCard") Boolean isAddCreditCard,
            @Param("city") String city);

    /**
     * Check if user exists by ID
     */
    boolean existsById(@NonNull UUID id);
}