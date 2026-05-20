package com.fraud.detection.model;

import java.time.Instant;
import java.util.Map;

/**
 * Raw transaction data — the input to the fraud detection pipeline.
 * Stored temporarily in Redis (L1, 24h TTL).
 */
public class Transaction {
    private String transactionId;
    private String accountId;
    private double amount;
    private String currency;
    private String merchantId;
    private String channel;         // ONLINE, ATM, POS
    private String deviceId;
    private String ipAddress;
    private String geoLocation;     // "lat,lng" or city code
    private long timestamp;
    private Map<String, String> metadata;

    public Transaction() {}

    // --- builder-style setters ---
    public Transaction setTransactionId(String transactionId) { this.transactionId = transactionId; return this; }
    public Transaction setAccountId(String accountId) { this.accountId = accountId; return this; }
    public Transaction setAmount(double amount) { this.amount = amount; return this; }
    public Transaction setCurrency(String currency) { this.currency = currency; return this; }
    public Transaction setMerchantId(String merchantId) { this.merchantId = merchantId; return this; }
    public Transaction setChannel(String channel) { this.channel = channel; return this; }
    public Transaction setDeviceId(String deviceId) { this.deviceId = deviceId; return this; }
    public Transaction setIpAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
    public Transaction setGeoLocation(String geoLocation) { this.geoLocation = geoLocation; return this; }
    public Transaction setTimestamp(long timestamp) { this.timestamp = timestamp; return this; }
    public Transaction setMetadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

    // --- getters ---
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMerchantId() { return merchantId; }
    public String getChannel() { return channel; }
    public String getDeviceId() { return deviceId; }
    public String getIpAddress() { return ipAddress; }
    public String getGeoLocation() { return geoLocation; }
    public long getTimestamp() { return timestamp; }
    public Map<String, String> getMetadata() { return metadata; }

    public Instant getInstant() { return Instant.ofEpochMilli(timestamp); }
}
