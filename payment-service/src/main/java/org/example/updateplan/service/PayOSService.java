package org.example.updateplan.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;
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

    private final PayOS payOS;
    private final PaymentRecordRepository paymentRecordRepository;

    public PayOSService(PayOS payOS, PaymentRecordRepository paymentRecordRepository) {
        this.payOS = payOS;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public CreatePaymentLinkResponse createPaymentLink(CreatePaymentLinkRequestDto requestDto, Long priceVnd) {
        Assert.notNull(requestDto, "requestDto must not be null");
        Assert.notNull(priceVnd, "priceVnd must not be null");

        UUID userId = Objects.requireNonNull(requestDto.getUserId(), "userId is required");
        String description = Optional.ofNullable(requestDto.getDescription())
                .filter(StringUtils::hasText)
                .orElse("PayOS payment");
        String cancelUrl = Objects.requireNonNull(requestDto.getCancelUrl(), "cancelUrl is required");
        String returnUrl = Objects.requireNonNull(requestDto.getReturnUrl(), "returnUrl is required");
        Long orderCode = generateOrderCode();

        List<PaymentLinkItem> items = buildFixedPriceItems(requestDto, description, priceVnd);
        OffsetDateTime expiresAt = generateAutoExpiry();

        CreatePaymentLinkRequest.CreatePaymentLinkRequestBuilder builder = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(priceVnd)
                .description(description)
                .cancelUrl(cancelUrl)
                .returnUrl(returnUrl)
                .expiredAt(expiresAt.toEpochSecond())
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

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(builder.build());
        persistPaymentRecord(userId, orderCode, description, response, expiresAt, priceVnd);
        return response;
    }

    public WebhookData verifyWebhook(Map<String, Object> payload) {
        Assert.notNull(payload, "payload must not be null");
        return payOS.webhooks().verify(payload);
    }

    private List<PaymentLinkItem> buildFixedPriceItems(CreatePaymentLinkRequestDto requestDto, String fallbackName, Long priceVnd) {
        String itemName = fallbackName;
        if (requestDto.getItems() != null && !requestDto.getItems().isEmpty()
                && requestDto.getItems().get(0) != null
                && StringUtils.hasText(requestDto.getItems().get(0).getName())) {
            itemName = requestDto.getItems().get(0).getName();
        }
        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(itemName)
                .quantity(1)
                .price(priceVnd)
                .build();
        return List.of(item);
    }

    @Transactional
    public int markExpiredRecordsAsFailed() {
        OffsetDateTime now = OffsetDateTime.now();
        List<PaymentRecord> expiredRecords = paymentRecordRepository.findByExpiresAtBefore(now);
        if (expiredRecords.isEmpty()) {
            return 0;
        }
        List<PaymentRecord> toUpdate = new ArrayList<>();
        for (PaymentRecord record : expiredRecords) {
            String status = record.getStatus();
            if (PaymentLinkStatus.PAID.name().equals(status) || PaymentLinkStatus.FAILED.name().equals(status)) {
                continue;
            }
            record.setStatus(PaymentLinkStatus.FAILED.name());
            toUpdate.add(record);
        }
        if (toUpdate.isEmpty()) {
            return 0;
        }
        paymentRecordRepository.saveAll(toUpdate);
        return toUpdate.size();
    }

    private OffsetDateTime generateAutoExpiry() {
        // Ensure the payment link expires two minutes after creation.
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(2);
    }

    private void persistPaymentRecord(UUID userId, Long orderCode, String description,
            CreatePaymentLinkResponse response, OffsetDateTime expiresAt, Long priceVnd) {
        PaymentRecord record = paymentRecordRepository.findById(userId).orElseGet(PaymentRecord::new);
        record.setId(userId);
        record.setOrderCode(orderCode);
        record.setAmount(priceVnd);
        record.setDescription(description);
        record.setPaymentLinkId(response.getPaymentLinkId());
        PaymentLinkStatus status = response.getStatus();
        if (status != null) {
            record.setStatus(status.name());
        }
        record.setCheckoutUrl(response.getCheckoutUrl());
        record.setQrCode(response.getQrCode());
        record.setExpiresAt(expiresAt);
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
