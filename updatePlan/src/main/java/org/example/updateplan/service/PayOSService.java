package org.example.updateplan.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.CRC32;

import org.example.updateplan.dto.BuyerInfoDto;
import org.example.updateplan.dto.CreatePaymentLinkRequestDto;
import org.example.updateplan.model.PaymentRecord;
import org.example.updateplan.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

@Service
public class PayOSService {

    private static final long FIXED_PRICE_VND = 5_000L;

    private final PayOS payOS;
    private final PaymentRecordRepository paymentRecordRepository;

    public PayOSService(PayOS payOS, PaymentRecordRepository paymentRecordRepository) {
        this.payOS = payOS;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public CreatePaymentLinkResponse createPaymentLink(CreatePaymentLinkRequestDto requestDto) {
        Assert.notNull(requestDto, "requestDto must not be null");

        String description = Optional.ofNullable(requestDto.getDescription())
                .filter(StringUtils::hasText)
                .orElse("PayOS payment");
        String cancelUrl = Objects.requireNonNull(requestDto.getCancelUrl(), "cancelUrl is required");
        String returnUrl = Objects.requireNonNull(requestDto.getReturnUrl(), "returnUrl is required");
        Long orderCode = generateOrderCode();

        List<PaymentLinkItem> items = buildFixedPriceItems(requestDto, description);

        CreatePaymentLinkRequest.CreatePaymentLinkRequestBuilder builder = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(FIXED_PRICE_VND)
                .description(description)
                .cancelUrl(cancelUrl)
                .returnUrl(returnUrl)
                .items(items);

        BuyerInfoDto buyer = requestDto.getBuyer();
        if (buyer != null) {
            if (StringUtils.hasText(buyer.getName())) {
                builder.buyerName(buyer.getName());
            }
            if (StringUtils.hasText(buyer.getCompanyName())) {
                builder.buyerCompanyName(buyer.getCompanyName());
            }
            if (StringUtils.hasText(buyer.getTaxCode())) {
                builder.buyerTaxCode(buyer.getTaxCode());
            }
            if (StringUtils.hasText(buyer.getEmail())) {
                builder.buyerEmail(buyer.getEmail());
            }
            if (StringUtils.hasText(buyer.getPhone())) {
                builder.buyerPhone(buyer.getPhone());
            }
            if (StringUtils.hasText(buyer.getAddress())) {
                builder.buyerAddress(buyer.getAddress());
            }
        }

        if (requestDto.getExpiredAt() != null) {
            builder.expiredAt(requestDto.getExpiredAt());
        }

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(builder.build());
        persistPaymentRecord(orderCode, description, response);
        return response;
    }

    public WebhookData verifyWebhook(Map<String, Object> payload) {
        Assert.notNull(payload, "payload must not be null");
        return payOS.webhooks().verify(payload);
    }

    private List<PaymentLinkItem> buildFixedPriceItems(CreatePaymentLinkRequestDto requestDto, String fallbackName) {
        String itemName = fallbackName;
        if (requestDto.getItems() != null && !requestDto.getItems().isEmpty()
                && requestDto.getItems().get(0) != null
                && StringUtils.hasText(requestDto.getItems().get(0).getName())) {
            itemName = requestDto.getItems().get(0).getName();
        }
        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(itemName)
                .quantity(1)
                .price(FIXED_PRICE_VND)
                .build();
        return List.of(item);
    }

    private void persistPaymentRecord(Long orderCode, String description, CreatePaymentLinkResponse response) {
        PaymentRecord record = new PaymentRecord();
        record.setOrderCode(orderCode);
        record.setAmount(FIXED_PRICE_VND);
        record.setDescription(description);
        record.setPaymentLinkId(response.getPaymentLinkId());
        PaymentLinkStatus status = response.getStatus();
        if (status != null) {
            record.setStatus(status.name());
        }
        record.setCheckoutUrl(response.getCheckoutUrl());
        record.setQrCode(response.getQrCode());
        paymentRecordRepository.save(record);
    }

    private Long generateOrderCode() {
        CRC32 crc32 = new CRC32();
        Long orderCode;
        do {
            String uuid = UUID.randomUUID().toString();
            crc32.reset();
            crc32.update(uuid.getBytes(StandardCharsets.UTF_8));
            long value = crc32.getValue();
            orderCode = value == 0 ? null : value;
        } while (orderCode == null || paymentRecordRepository.findByOrderCode(orderCode).isPresent());
        return orderCode;
    }
}
