package com.miyuki.Inventory.Management.external;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SafeHttpClientFactory {

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_REDIRECTS = 3;

    private final ProductUrlValidator urlValidator;

    public SafeHttpClientFactory(ProductUrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    public HttpClient createSafeClient() {
        return HttpClient.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public HttpResponse<String> sendWithSafeRedirects(
            HttpClient client,
            URI targetUri,
            Duration timeout) throws Exception {
        URI currentUri = targetUri;
        urlValidator.validate(currentUri.toString());

        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpRequest request = HttpRequest.newBuilder(currentUri)
                    .timeout(timeout != null ? timeout : DEFAULT_REQUEST_TIMEOUT)
                    .header("User-Agent", "InventoryManagement/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location == null || location.isBlank()) {
                    return response;
                }
                URI nextUri = currentUri.resolve(location);
                // Validate the redirect target with our SSRF & IP validator
                urlValidator.validate(nextUri.toString());
                currentUri = nextUri;
                continue;
            }

            return response;
        }

        throw new IllegalStateException("Too many redirects (exceeded " + MAX_REDIRECTS + ")");
    }
}
