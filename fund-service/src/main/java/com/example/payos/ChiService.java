package com.example.payos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Additional Spring annotation import for GET mapping
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ChiService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Google Sheets
    private final GoogleSheetsService sheetsService;
    private final String thuSheetName;
    private final String chiSheetName;

    public ChiService() {
        String spreadsheetId = EnvUtils.get("GOOGLE_SHEET_ID");
        this.thuSheetName = EnvUtils.getOrDefault("GOOGLE_THU_SHEET_NAME", "Sheet1");
        this.chiSheetName = EnvUtils.getOrDefault("GOOGLE_CHI_SHEET_NAME", "Chi");
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

    @PostMapping(path = "/payout/transfer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> payout(@RequestBody PayoutRequest request) {
        try {
            String safeDescription = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                    ? request.getDescription().trim() : "Thanh toán";
            String fundId = null;
            if (sheetsService != null) {
                fundId = extractFundId(safeDescription);
                if (fundId != null) {
                    long balance = sheetsService.computeBalance(thuSheetName, fundId);
                    if (balance < request.getAmount()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Collections.singletonMap("error", "Số dư quỹ không đủ. Số dư hiện tại: " + balance));
                    }
                    try {
                        markFundAsProcessed(fundId, request.getAmount());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            String referenceId = "GOWISE_PAYOUT_" + System.currentTimeMillis();

            Map<String, Object> payoutData = new LinkedHashMap<>();
            payoutData.put("referenceId", referenceId);
            payoutData.put("amount", request.getAmount());
            payoutData.put("description", safeDescription);
            payoutData.put("toBin", request.getBankCode());
            payoutData.put("toAccountNumber", request.getAccountNumber());

            // dO payOS ngu nên phải HAS256
            String checksumKey = EnvUtils.get("PAYOS_CHECKSUM_KEY_PAYMENT");
            String dataString = payoutData.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");
            String signature = hmacSha256(checksumKey, dataString);

            String idempotencyKey = UUID.randomUUID().toString();
            String jsonBody = objectMapper.writeValueAsString(payoutData);

            String payoutUrl = EnvUtils.getOrDefault("PAYOS_URL_PAYMENT", "https://api-merchant.payos.vn/v1/payouts");
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(payoutUrl))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", EnvUtils.get("PAYOS_CLIENT_ID_PAYMENT"))
                    .header("x-api-key", EnvUtils.get("PAYOS_API_KEY_PAYMENT"))
                    .header("x-idempotency-key", idempotencyKey)
                    .header("x-signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 == 2) {
                Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
                // Chuyển trạng thái cho gg sheet thành 'đã chi'
                if (sheetsService != null) {
                    try {
                        List<Object> row = Arrays.asList(
                                referenceId,
                                request.getAmount(),
                                safeDescription,
                                request.getBankCode(),
                                request.getAccountNumber(),
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                "ĐÃ CHI"
                        );
                        sheetsService.appendRow(chiSheetName, row);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return ResponseEntity.ok(body);
            } else {
                return ResponseEntity.status(response.statusCode())
                        .body(Collections.singletonMap("error", response.body()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Lỗi Payout: " + e.getMessage()));
        }
    }

    private String extractFundId(String description) {
        if (description == null) return null;
        // Regex to match Q followed by digits
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("Q(\\d+)").matcher(description);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void markFundAsProcessed(String fundId, long amount) throws IOException {
        if (sheetsService == null) return;
        List<List<Object>> rows = sheetsService.readSheet(thuSheetName);
        if (rows == null || rows.size() <= 1) return;

        List<Object> header = rows.get(0);
        int descIdx = header.indexOf("Mô tả");
        int amountIdx = header.indexOf("Số tiền");
        int statusIdx = header.indexOf("Trạng thái");
        if (descIdx < 0 || amountIdx < 0 || statusIdx < 0) return;

        long remaining = amount;
        List<Integer> rowNumbers = new ArrayList<>();
        for (int i = 1; i < rows.size() && remaining > 0; i++) {
            List<Object> row = rows.get(i);
            String desc = row.size() > descIdx ? String.valueOf(row.get(descIdx)) : "";
            String status = row.size() > statusIdx ? String.valueOf(row.get(statusIdx)).trim() : "";
            boolean unprocessed = status.isEmpty()
                    || (!status.equalsIgnoreCase("ĐÃ CHI") && !status.equalsIgnoreCase("DA CHI"));
            boolean matchesFund = java.util.regex.Pattern
                    .compile("\\bQ" + fundId + "\\b")
                    .matcher(desc)
                    .find();
            if (matchesFund && unprocessed) {
                String moneyStr = row.size() > amountIdx ? String.valueOf(row.get(amountIdx)) : "0";
                long rowAmount;
                try {
                    rowAmount = Long.parseLong(moneyStr.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException nfe) {
                    rowAmount = 0;
                }
                rowNumbers.add(i + 1);
                remaining -= rowAmount;
            }
        }
        for (int rowNum : rowNumbers) {
            String colLetter = toColumnLetter(statusIdx + 1);
            String cellRange = thuSheetName + "!" + colLetter + rowNum;
            sheetsService.updateCells(cellRange,
                    Collections.singletonList(Collections.singletonList("ĐÃ CHI")));
        }
    }

    private String toColumnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        while (index > 0) {
            int rem = (index - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            index = (index - 1) / 26;
        }
        return sb.toString();
    }

    @GetMapping(path = "/vietqr/banks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getBanks() {
        String apiUrl = EnvUtils.get("VIETQR_API_URL");
        String apiClientId = EnvUtils.get("VIETQR_CLIENT_ID");
        String apiKey = EnvUtils.get("VIETQR_API_KEY");
        List<Map<String, Object>> banks = new ArrayList<>();
        boolean hasApiConfig = apiUrl != null && !apiUrl.isEmpty()
                && apiClientId != null && !apiClientId.isEmpty()
                && apiKey != null && !apiKey.isEmpty();
        if (hasApiConfig) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/banks"))
                        .header("x-client-id", apiClientId)
                        .header("x-api-key", apiKey)
                        .GET()
                        .build();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
                    Object dataObj = body.get("data");
                    if (dataObj instanceof List<?> list) {
                        for (Object item : list) {
                            Map<String, Object> bank = objectMapper.convertValue(item, new TypeReference<>() {});
                            Map<String, Object> simplified = new LinkedHashMap<>();
                            simplified.put("bin", bank.get("bin"));
                            simplified.put("name", bank.get("name"));
                            simplified.put("shortName", bank.get("shortName"));
                            banks.add(simplified);
                        }
                        return ResponseEntity.ok(banks);
                    }
                }
            } catch (Exception ignore) {
            }
        }
        // Phòng khi chưa load xong thì hiển thị tạm 3 cái này thay vì lỗi
        Map<String, Object> bank1 = new LinkedHashMap<>();
        bank1.put("bin", "970436");
        bank1.put("shortName", "VCB");
        bank1.put("name", "Ngân hàng TMCP Ngoại thương Việt Nam");
        Map<String, Object> bank2 = new LinkedHashMap<>();
        bank2.put("bin", "970407");
        bank2.put("shortName", "TCB");
        bank2.put("name", "Ngân hàng TMCP Kỹ Thương Việt Nam");
        Map<String, Object> bank3 = new LinkedHashMap<>();
        bank3.put("bin", "970415");
        bank3.put("shortName", "VPB");
        bank3.put("name", "Ngân hàng TMCP Việt Nam Thịnh Vượng");
        banks.add(bank1);
        banks.add(bank2);
        banks.add(bank3);
        return ResponseEntity.ok(banks);
    }

    @GetMapping(path = "/fund/{fundId}/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFundBalance(@PathVariable String fundId) {
        if (sheetsService == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Google Sheets integration is not configured"));
        }
        try {
            long balance = sheetsService.computeBalance(thuSheetName, fundId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fundId", fundId);
            result.put("balance", balance);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
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
            throw new RuntimeException("Lỗi k kí đc: " + e.getMessage(), e);
        }
    }

    public static class PayoutRequest {
        private int amount;
        private String description;
        private String bankCode;
        private String accountNumber;

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getBankCode() {
            return bankCode;
        }

        public void setBankCode(String bankCode) {
            this.bankCode = bankCode;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }
    }
}