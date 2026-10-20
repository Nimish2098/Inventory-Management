package com.miyuki.Inventory.Management.stock;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscordNotificationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void discordHttpFailureIsReportedAsDeliveryFailure() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        DiscordNotificationService service = new DiscordNotificationService(
                "https://discord.com/api/webhooks/test", httpClient);

        assertThrows(DiscordNotificationService.DiscordNotificationException.class,
                () -> service.sendLowStockAlert("LOW STOCK ALERT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void discordRateLimitIsReportedWithRetryAfter() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> response = mock(HttpResponse.class);
        java.net.http.HttpHeaders headers = mock(java.net.http.HttpHeaders.class);
        when(response.statusCode()).thenReturn(429);
        when(response.headers()).thenReturn(headers);
        when(headers.firstValue("Retry-After")).thenReturn(java.util.Optional.of("2"));
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        DiscordNotificationService service = new DiscordNotificationService(
                "https://discord.com/api/webhooks/test", httpClient);

        DiscordNotificationService.DiscordNotificationException exception = assertThrows(
                DiscordNotificationService.DiscordNotificationException.class,
                () -> service.sendLowStockAlert("LOW STOCK ALERT"));

        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.Optional.of(java.time.Duration.ofSeconds(2)), exception.getRetryAfter());
        org.junit.jupiter.api.Assertions.assertTrue(exception.isRetryable());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unauthorizedWebhookIsPermanentAndDoesNotExposeItsUrl() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        String webhook = "https://discord.com/api/webhooks/private-token";
        DiscordNotificationService service = new DiscordNotificationService(webhook, httpClient);

        DiscordNotificationService.DiscordNotificationException exception = assertThrows(
                DiscordNotificationService.DiscordNotificationException.class,
                () -> service.sendNotification("alert"));

        org.junit.jupiter.api.Assertions.assertFalse(exception.isRetryable());
        org.junit.jupiter.api.Assertions.assertFalse(exception.getMessage().contains(webhook));
    }

    @Test
    void missingWebhookFailsWithActionableConfigurationMessage() {
        DiscordNotificationService service = new DiscordNotificationService(
                "", mock(HttpClient.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.sendLowStockAlert("LOW STOCK ALERT"));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("DISCORD_WEBHOOK_URL"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void networkTimeoutOrFailureIsReported() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("connection failed"));
        DiscordNotificationService service = new DiscordNotificationService(
                "https://discord.com/api/webhooks/test", httpClient);

        assertThrows(DiscordNotificationService.DiscordNotificationException.class,
                () -> service.sendLowStockAlert("LOW STOCK ALERT"));
    }
}
