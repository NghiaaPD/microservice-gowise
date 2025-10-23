package org.example.updateplan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payos")
public class PayOSProperties {

    /**
     * Client ID assigned by PayOS.
     */
    private String clientId;

    /**
     * API key used to sign payloads sent to PayOS.
     */
    private String apiKey;

    /**
     * Checksum key used to verify webhook payloads.
     */
    private String checksumKey;

    /**
     * Optional base URL for PayOS API; defaults to SDK value when empty.
     */
    private String baseUrl;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChecksumKey() {
        return checksumKey;
    }

    public void setChecksumKey(String checksumKey) {
        this.checksumKey = checksumKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

