package com.example.user_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank(message = "Card number is required")
    @Column(name = "card_number_encrypted", nullable = false, columnDefinition = "TEXT")
    private String cardNumberEncrypted;

    @NotBlank(message = "Last four digits are required")
    @Size(min = 4, max = 4, message = "Last four digits must be exactly 4 characters")
    @Pattern(regexp = "^[0-9]{4}$", message = "Last four digits must contain only numbers")
    @Column(name = "last_four_digits", nullable = false, columnDefinition = "CHAR(4)")
    private String lastFourDigits;

    @NotNull(message = "Card type is required")
    @Column(name = "card_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @NotNull(message = "Expiry month is required")
    @Column(name = "expiry_month", nullable = false)
    private Short expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Column(name = "expiry_year", nullable = false)
    private Short expiryYear;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum for Card Types
    public enum CardType {
        VISA, MASTERCARD, AMEX, DISCOVER, OTHER;

        // Custom deserialization to handle case-insensitive input
        @com.fasterxml.jackson.annotation.JsonCreator
        public static CardType fromString(String value) {
            if (value == null)
                return null;
            return CardType.valueOf(value.toUpperCase());
        }
    }

    // Constructors
    public CreditCard() {
    }

    public CreditCard(UUID userId, String cardNumberEncrypted, String lastFourDigits,
            CardType cardType, Short expiryMonth, Short expiryYear) {
        this.userId = userId;
        this.cardNumberEncrypted = cardNumberEncrypted;
        this.lastFourDigits = lastFourDigits;
        this.cardType = cardType;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCardNumberEncrypted() {
        return cardNumberEncrypted;
    }

    public void setCardNumberEncrypted(String cardNumberEncrypted) {
        this.cardNumberEncrypted = cardNumberEncrypted;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public Short getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Short expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Short getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Short expiryYear) {
        this.expiryYear = expiryYear;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "id=" + id +
                ", userId=" + userId +
                ", lastFourDigits='****" + lastFourDigits + '\'' +
                ", cardType=" + cardType +
                ", expiryMonth=" + expiryMonth +
                ", expiryYear=" + expiryYear +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}