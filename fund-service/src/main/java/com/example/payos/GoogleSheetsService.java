package com.example.payos;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleSheetsService {

    private final Sheets sheets;
    private final String spreadsheetId;
    public GoogleSheetsService(String spreadsheetId) throws GeneralSecurityException, IOException {
        this.spreadsheetId = spreadsheetId;
        GoogleCredentials credentials;
        String credPath = EnvUtils.get("GOOGLE_APPLICATION_CREDENTIALS");
        if (credPath != null && !credPath.isEmpty() && Files.exists(Paths.get(credPath))) {
            try (FileInputStream in = new FileInputStream(credPath)) {
                credentials = GoogleCredentials.fromStream(in)
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            }
        } else {
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        }
        this.sheets = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("PayOS Java Demo")
                .build();
    }

    public List<List<Object>> readSheet(String range) throws IOException {
        ValueRange response = sheets.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }

    public void appendRow(String sheetName, List<Object> row) throws IOException {
        ValueRange body = new ValueRange().setValues(Collections.singletonList(row));
        sheets.spreadsheets().values()
                .append(spreadsheetId, sheetName, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public void updateCells(String range, List<List<Object>> values) throws IOException {
        ValueRange body = new ValueRange().setValues(values);
        sheets.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public long computeBalance(String sheetName, String fundId) throws IOException {
        List<List<Object>> rows = readSheet(sheetName);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        List<Object> header = rows.get(0);
        int descIdx = header.indexOf("Mô tả");
        int amountIdx = header.indexOf("Số tiền");
        int statusIdx = header.indexOf("Trạng thái");
        if (descIdx < 0 || amountIdx < 0 || statusIdx < 0) {
            return 0;
        }
        long total = 0;
        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.size() <= Math.max(descIdx, Math.max(amountIdx, statusIdx))) {
                continue;
            }
            String desc = Objects.toString(row.get(descIdx), "");
            String status = Objects.toString(row.get(statusIdx), "").trim();
            boolean unprocessed = status.isEmpty()
                    || (!status.equalsIgnoreCase("ĐÃ CHI") && !status.equalsIgnoreCase("DA CHI"));
            boolean matchesFund = java.util.regex.Pattern
                    .compile("\\bQ" + fundId + "\\b")
                    .matcher(desc)
                    .find();
            if (matchesFund && unprocessed) {
                String moneyStr = Objects.toString(row.get(amountIdx), "0");
                try {
                    total += Long.parseLong(moneyStr.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return total;
    }
}