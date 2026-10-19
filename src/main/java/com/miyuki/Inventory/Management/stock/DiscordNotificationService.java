package com.miyuki.Inventory.Management.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class DiscordNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotificationService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String webhookUrl;
    private final HttpClient httpClient;

    @Autowired
    public DiscordNotificationService(@Value("${discord.webhook-url:}") String webhookUrl) {
        this(webhookUrl, HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build());
    }

    DiscordNotificationService(String webhookUrl, HttpClient httpClient) {
        this.webhookUrl = webhookUrl;
        this.httpClient = httpClient;
    }

    public void sendLowStockAlert(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalStateException(
                    "Discord webhook is not configured. Set DISCORD_WEBHOOK_URL to enable notifications.");
        }

        String payload = "{\"content\":\"" + escapeJson(message) + "\"}";

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "DISCORD_WEBHOOK_URL is invalid. Provide a valid Discord incoming webhook URL.");
        }

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status == 429) {
                String retryAfter = response.headers().firstValue("Retry-After").orElse("unspecified");
                throw new DiscordNotificationException(
                        "Discord rate-limited the notification; Retry-After: " + retryAfter + ".");
            }
            if (status < 200 || status >= 300) {
                throw new DiscordNotificationException(
                        "Discord rejected the notification with HTTP status " + status + ".");
            }
        } catch (DiscordNotificationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DiscordNotificationException("Discord notification was interrupted.");
        } catch (IOException exception) {
            throw new DiscordNotificationException(
                    "Discord notification failed due to an HTTP or timeout error.");
        }
        logger.info("Discord low-stock notification delivered");
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    public static class DiscordNotificationException extends RuntimeException {
        public DiscordNotificationException(String message) {
            super(message);
        }

    }
}
