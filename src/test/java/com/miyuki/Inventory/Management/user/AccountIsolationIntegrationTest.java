package com.miyuki.Inventory.Management.user;

import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.customer.CustomerRepository;
import com.miyuki.Inventory.Management.order.OrderRepository;
import com.miyuki.Inventory.Management.stock.InventoryitemRepository;
import com.miyuki.Inventory.Management.stock.StockMovementRepository;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AccountIsolationIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryitemRepository inventoryRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    private Long firstUserId;
    private Long secondUserId;
    private Long productId;
    private Long warehouseId;
    private Long customerId;
    private Long orderId;

    @AfterEach
    void cleanUp() {
        if (orderId != null) {
            orderRepository.deleteById(orderId);
        }
        if (productId != null && warehouseId != null) {
            movementRepository.deleteAll(movementRepository.findByProductAndWarehouse(productId, warehouseId));
            inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                    .ifPresent(inventoryRepository::delete);
        }
        if (customerId != null) {
            customerRepository.deleteById(customerId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (warehouseId != null) {
            warehouseRepository.deleteById(warehouseId);
        }
        if (firstUserId != null) {
            userRepository.deleteById(firstUserId);
        }
        if (secondUserId != null) {
            userRepository.deleteById(secondUserId);
        }
    }

    @Test
    void registeredUsersCanOnlyAccessTheirOwnProducts() throws Exception {
        String unique = UUID.randomUUID().toString();
        String firstUser = register("first-" + unique + "@example.test");
        firstUserId = Long.valueOf(jsonNumber(firstUser, "id"));
        String secondUser = register("second-" + unique + "@example.test");
        secondUserId = Long.valueOf(jsonNumber(secondUser, "id"));
        String firstEmail = "first-" + unique + "@example.test";
        String loginResponse = login(firstEmail);
        HttpResponse<String> profileResponse = send(
                "GET", "/users/me", null, jsonString(loginResponse, "accessToken"));
        assertEquals(200, profileResponse.statusCode());
        assertTrue(profileResponse.body().contains(firstEmail));

        HttpResponse<String> createResponse = send(
                "POST",
                "/product",
                "{\"sku\":\"SKU-" + unique + "\",\"name\":\"Private product\","
                        + "\"category\":\"Test\",\"reorder_level\":2}",
                jsonString(firstUser, "accessToken"));
        assertEquals(201, createResponse.statusCode());
        productId = Long.valueOf(jsonNumber(createResponse.body(), "id"));

        HttpResponse<String> warehouseResponse = send(
                "POST",
                "/warehouse",
                "{\"code\":\"WH-" + unique + "\",\"name\":\"Private warehouse\",\"capacity\":100}",
                jsonString(firstUser, "accessToken"));
        assertEquals(201, warehouseResponse.statusCode());
        warehouseId = Long.valueOf(jsonNumber(warehouseResponse.body(), "id"));

        HttpResponse<String> listResponse = send(
                "GET", "/product", null, jsonString(secondUser, "accessToken"));
        assertEquals(200, listResponse.statusCode());
        assertEquals("[]", listResponse.body().trim());

        HttpResponse<String> detailResponse = send(
                "GET", "/product/" + productId, null, jsonString(secondUser, "accessToken"));
        assertEquals(404, detailResponse.statusCode());

        HttpResponse<String> warehouseListResponse = send(
                "GET", "/warehouse", null, jsonString(secondUser, "accessToken"));
        assertEquals(200, warehouseListResponse.statusCode());
        assertEquals("[]", warehouseListResponse.body().trim());
        assertEquals(404, send(
                "GET", "/warehouse/" + warehouseId, null, jsonString(secondUser, "accessToken")).statusCode());

        String firstToken = jsonString(firstUser, "accessToken");
        HttpResponse<String> inboundStock = send(
                "POST",
                "/inventory/adjustments",
                "{\"product_id\":" + productId + ",\"warehouse_id\":" + warehouseId
                        + ",\"quantity\":3,\"movementType\":\"INBOUND\"}",
                firstToken);
        assertEquals(200, inboundStock.statusCode());

        HttpResponse<String> customerResponse = send(
                "POST",
                "/customer",
                "{\"name\":\"Private customer\",\"email\":\"customer-" + unique + "@example.test\"}",
                firstToken);
        assertEquals(201, customerResponse.statusCode());
        customerId = Long.valueOf(jsonNumber(customerResponse.body(), "id"));

        HttpResponse<String> orderResponse = send(
                "POST",
                "/order",
                "{\"customer_id\":" + customerId + ",\"items\":[{\"product_id\":" + productId
                        + ",\"warehouse_id\":" + warehouseId + ",\"quantity\":1}]}",
                firstToken);
        assertEquals(201, orderResponse.statusCode());
        orderId = Long.valueOf(jsonNumber(orderResponse.body(), "id"));
        assertEquals("[]", send("GET", "/order", null, jsonString(secondUser, "accessToken"))
                .body().trim());
        assertEquals(404, send(
                "GET", "/order/" + orderId, null, jsonString(secondUser, "accessToken")).statusCode());

        HttpResponse<String> crossAccountAdjustment = send(
                "POST",
                "/inventory/adjustments",
                "{\"product_id\":" + productId + ",\"warehouse_id\":" + warehouseId
                        + ",\"quantity\":3,\"movementType\":\"INBOUND\"}",
                jsonString(secondUser, "accessToken"));
        assertEquals(404, crossAccountAdjustment.statusCode());

        HttpResponse<String> anonymousResponse = send("GET", "/product", null, null);
        assertEquals(401, anonymousResponse.statusCode());
    }

    @Test
    void corsPreflightAllowsFrontendLoginRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/auth/login")))
                .header("Origin", "http://127.0.0.1:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("http://127.0.0.1:5173", response.headers().firstValue("Access-Control-Allow-Origin")
                .orElse(null));
        assertTrue(response.headers().firstValue("Access-Control-Allow-Methods")
                .orElse("").contains("POST"));
    }

    private String register(String email) throws Exception {
        HttpResponse<String> response = send(
                "POST",
                "/auth/register",
                "{\"name\":\"Test User\",\"email\":\"" + email
                        + "\",\"password\":\"a-secure-test-password\"}",
                null);
        assertEquals(201, response.statusCode());
        assertNotNull(jsonString(response.body(), "accessToken"));
        return response.body();
    }

    private String login(String email) throws Exception {
        HttpResponse<String> response = send(
                "POST",
                "/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"a-secure-test-password\"}",
                null);
        assertEquals(200, response.statusCode());
        return response.body();
    }

    private String jsonString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing string field " + key + " in response");
        }
        return matcher.group(1);
    }

    private String jsonNumber(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing number field " + key + " in response");
        }
        return matcher.group(1);
    }

    private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
