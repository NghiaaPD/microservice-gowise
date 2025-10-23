package org.example.updateplan.dto;

import java.util.ArrayList;
import java.util.List;

public class CreatePaymentLinkRequestDto {

    private Long orderCode;

    private Long amount;

    private String description;

    private String cancelUrl;

    private String returnUrl;

    private Long expiredAt;

    private BuyerInfoDto buyer;

    private List<PaymentLinkItemDto> items = new ArrayList<>();

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

