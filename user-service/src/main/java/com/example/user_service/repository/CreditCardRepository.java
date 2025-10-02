package com.example.user_service.repository;

import com.example.user_service.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    /**
     * Tìm tất cả thẻ của một user
     */
    List<CreditCard> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Tìm tất cả thẻ của một user (bao gồm cả không active)
     */
    List<CreditCard> findByUserId(UUID userId);

    /**
     * Tìm thẻ mặc định của user
     */
    Optional<CreditCard> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);

    /**
     * Đếm số thẻ active của user
     */
    long countByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Đếm tổng số thẻ của user (bao gồm cả inactive)
     */
    long countByUserId(UUID userId);

    /**
     * Tắt tất cả thẻ mặc định của user (dùng khi set thẻ mới làm default)
     */
    @Modifying
    @Query("UPDATE CreditCard c SET c.isDefault = false WHERE c.userId = :userId AND c.isDefault = true")
    void unsetAllDefaultCardsForUser(@Param("userId") UUID userId);

    /**
     * Kiểm tra user có thẻ nào không
     */
    boolean existsByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Tìm thẻ theo last 4 digits và user ID
     */
    Optional<CreditCard> findByUserIdAndLastFourDigitsAndIsActiveTrue(UUID userId, String lastFourDigits);
}