package com.example.payos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ThuService {

    private final Map<String, String> paymentStatuses = new ConcurrentHashMap<>();
    private final Map<String, PendingDeposit> pendingDeposits = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GoogleSheetsService sheetsService;
    private final String thuSheetName;
    public ThuService() {
        String spreadsheetId = EnvUtils.get("GOOGLE_SHEET_ID");
        this.thuSheetName = EnvUtils.getOrDefault("GOOGLE_THU_SHEET_NAME", "Sheet1");
        GoogleSheetsService service = null;
        if (spreadsheetId != null && !spreadsheetId.isEmpty()) {
            try {
                service = new GoogleSheetsService(spreadsheetId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.sheetsService = service;
    }

    @PostMapping(path = "/payment/create-link", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPaymentLink(@RequestBody CreatePaymentRequest request) {
        try {
            long orderCode = System.currentTimeMillis();
            paymentStatuses.put(String.valueOf(orderCode), "PENDING");
            PendingDeposit pending = new PendingDeposit();
            pending.orderCode = String.valueOf(orderCode);
            pending.amount = request.getAmount();
            String description = String.format("Q%s U%s", request.getFundId(), request.getUserId());
            pending.description = description;

            int port = 3000;
            String returnUrl = String.format("http://localhost:%d/", port);
            String cancelUrl = returnUrl;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderCode", orderCode);
            payload.put("amount", request.getAmount());
            payload.put("description", description);
            payload.put("returnUrl", returnUrl);
            payload.put("cancelUrl", cancelUrl);

            String checksumKey = EnvUtils.get("PAYOS_CHECKSUM_KEY");
            String dataString = payload.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            String signature = hmacSha256(checksumKey, dataString);
            payload.put("signature", signature);

            String jsonBody = objectMapper.writeValueAsString(payload);

            String payosUrl = EnvUtils.getOrDefault("PAYOS_URL", "https://api-merchant.payos.vn/v2/payment-requests");
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(payosUrl))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", EnvUtils.get("PAYOS_CLIENT_ID"))
                    .header("x-api-key", EnvUtils.get("PAYOS_API_KEY"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> payosResponse = objectMapper.readValue(response.body(), new TypeReference<>() {});

            Object dataObj = payosResponse.get("data");
            if (dataObj != null) {
                pendingDeposits.put(pending.orderCode, pending);
                return ResponseEntity.ok(dataObj);
            } else {
                return ResponseEntity.status(response.statusCode()).body(payosResponse);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Lỗi khi tạo link thanh toán: " + e.getMessage()));
        }
    }
    @PostMapping(path = "/payos-webhook", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        if (!webhookData.containsKey("data")) {
            return ResponseEntity.ok(Collections.singletonMap("message", "Webhook tét."));
        }
        try {
            Object dataObj = webhookData.get("data");
            Map<String, Object> data = objectMapper.convertValue(dataObj, new TypeReference<>() {});
            String code = (String) data.getOrDefault("code", "");
            if ("00".equals(code)) {
                String orderCode = String.valueOf(data.get("orderCode"));
                paymentStatuses.put(orderCode, "PAID");
                PendingDeposit pending = pendingDeposits.remove(orderCode);
                if (pending != null && sheetsService != null) {
                    try {
                        List<List<Object>> rows = sheetsService.readSheet(thuSheetName);
                        if (rows != null && rows.size() > 1) {
                            int rowNum = 2;
                            List<Object> header = rows.get(0);
                            int statusIdx = header.indexOf("Trạng thái");
                            int colIndex = (statusIdx >= 0) ? statusIdx + 1 : 15; 
                            String colLetter = toColumnLetter(colIndex);
                            String cellRange = thuSheetName + "!" + colLetter + rowNum;
                            sheetsService.updateCells(cellRange,
                                Collections.singletonList(Collections.singletonList("CHƯA RÚT")));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.singletonMap("success", false));
        }
    }

    @GetMapping(path = "/payment/check-status/{orderCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checkStatus(@PathVariable String orderCode) {
        String status = paymentStatuses.getOrDefault(orderCode, "NOT_FOUND");
        return ResponseEntity.ok(Collections.singletonMap("status", status));
    }

    private String hmacSha256(String secretKey, String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hashBytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ko tạo đc HMAC: " + e.getMessage(), e);
        }
    }

    private static String toColumnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        while (index > 0) {
            int rem = (index - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            index = (index - 1) / 26;
        }
        return sb.toString();
    }

    private static class PendingDeposit {
        String orderCode;
        int amount;
        String description;
    }
    public static class CreatePaymentRequest {
        private String fundId;
        private String userId;
        private int amount;

        public String getFundId() {
            return fundId;
        }

        public void setFundId(String fundId) {
            this.fundId = fundId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}