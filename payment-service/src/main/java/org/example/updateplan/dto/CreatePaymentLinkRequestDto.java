package org.example.updateplan.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatePaymentLinkRequestDto {

    @JsonProperty("userId")
    @JsonAlias("id")
    private UUID userId;

    private Long orderCode;

    private Long amount;

    private String description;

    private String cancelUrl;

    private String returnUrl;

    private Long expiredAt;

    private BuyerInfoDto buyer;

    private List<PaymentLinkItemDto> items = new ArrayList<>();

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(Long orderCode) {
        this.orderCode = orderCode;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public Long getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Long expiredAt) {
        this.expiredAt = expiredAt;
    }

    public BuyerInfoDto getBuyer() {
        return buyer;
    }

    public void setBuyer(BuyerInfoDto buyer) {
        this.buyer = buyer;
    }

    public List<PaymentLinkItemDto> getItems() {
        return items;
    }

    public void setItems(List<PaymentLinkItemDto> items) {
        this.items = items;
    }
}
