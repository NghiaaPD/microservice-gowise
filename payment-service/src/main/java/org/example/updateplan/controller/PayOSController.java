package org.example.updateplan.controller;

import java.util.HashMap;
import java.util.Map;

import org.example.updateplan.dto.CreatePaymentLinkRequestDto;
import org.example.updateplan.service.PayOSService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.payos.exception.APIException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/payos")
public class PayOSController {

    private final PayOSService payOSService;

    public PayOSController(PayOSService payOSService) {
        this.payOSService = payOSService;
    }

    @PostMapping("/payment-link")
    public ResponseEntity<CreatePaymentLinkResponse> createPaymentLink(
            @RequestBody CreatePaymentLinkRequestDto requestDto) {
        CreatePaymentLinkResponse response = payOSService.createPaymentLink(requestDto, 52_397L);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment-link/premium")
    public ResponseEntity<CreatePaymentLinkResponse> createPremiumPaymentLink(
            @RequestBody CreatePaymentLinkRequestDto requestDto) {
        CreatePaymentLinkResponse response = payOSService.createPaymentLink(requestDto, 314_380L);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment-link/enterprise")
    public ResponseEntity<CreatePaymentLinkResponse> createEnterprisePaymentLink(
            @RequestBody CreatePaymentLinkRequestDto requestDto) {
        CreatePaymentLinkResponse response = payOSService.createPaymentLink(requestDto, 628_760L);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> payload) {
        WebhookData data = payOSService.verifyWebhook(payload);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "success");
        responseBody.put("data", data);

        // Return HTTP 200 to acknowledge PayOS that the webhook was processed.
        return ResponseEntity.ok(responseBody);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(APIException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getErrorDesc().orElse(ex.getMessage()));
        ex.getErrorCode().ifPresent(code -> body.put("code", code));
        HttpStatus status = ex.getStatusCode()
                .map(HttpStatus::resolve)
                .orElse(HttpStatus.BAD_GATEWAY);
        return ResponseEntity.status(status != null ? status : HttpStatus.BAD_GATEWAY).body(body);
    }
}

