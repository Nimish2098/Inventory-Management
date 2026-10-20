package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.external.dto.CreateProductWatchRequest;
import com.miyuki.Inventory.Management.external.dto.ProductWatchResponse;
import com.miyuki.Inventory.Management.external.dto.UpdateProductWatchRequest;
import com.miyuki.Inventory.Management.user.UserAccount;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "monitor.external.allowed-hosts=shop.example",
        "monitor.external.interval-ms=3600000",
        "monitor.notification.interval-ms=3600000"
})
class ProductWatchIntegrationTest {

    @Autowired
    private ProductWatchService watchService;

    @Autowired
    private ProductObservationRecorder observationRecorder;

    @Autowired
    private ProductWatchRepository watchRepository;

    @Autowired
    private ProductNotificationEventRepository notificationRepository;

    @Autowired
    private ProductNotificationEventStatusService notificationStatusService;

    @Autowired
    private UserAccountRepository userRepository;

    private UserAccount owner;
    private UserAccount otherOwner;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        owner = user("watch-owner-" + unique + "@example.test");
        otherOwner = user("watch-other-" + unique + "@example.test");
        authenticate(owner);
    }

    @AfterEach
    void cleanUp() {
        authenticate(owner);
        for (ProductWatch watch : watchRepository.findAllByOwner_IdOrderByCreatedAtDesc(owner.getId())) {
            watchService.delete(watch.getId());
        }
        SecurityContextHolder.clearContext();
        userRepository.deleteById(owner.getId());
        userRepository.deleteById(otherOwner.getId());
    }

    @Test
    void accountCanManageWatchesAndReadPersistedHistory() {
        ProductWatchResponse created = watchService.create(new CreateProductWatchRequest(
                "Wireless Headphones", "https://shop.example/headphones", new BigDecimal("4000")));

        assertNotNull(created.id());
        assertEquals("Wireless Headphones", created.productName());
        assertEquals(1, watchService.list().size());
        assertEquals(created.id(), watchService.get(created.id()).id());

        observationRecorder.record(created.id(), new StoreObservation(
                new BigDecimal("4999"), StockStatus.IN_STOCK, Instant.parse("2026-05-01T10:00:00Z")));
        assertEquals(1, watchService.history(created.id()).size());

        ProductWatchResponse updated = watchService.update(created.id(),
                new UpdateProductWatchRequest(new BigDecimal("3900"), false, false));
        assertEquals(new BigDecimal("3900"), updated.targetPrice());
        assertFalse(updated.enabled());

        watchService.delete(created.id());
        assertEquals(0, watchService.list().size());
    }

    @Test
    void crossAccountWatchAndHistoryAccessAreNotFound() {
        ProductWatchResponse created = watchService.create(new CreateProductWatchRequest(
                "Private product", "https://shop.example/private", null));
        observationRecorder.record(created.id(), new StoreObservation(
                new BigDecimal("12.00"), StockStatus.UNKNOWN, Instant.now()));

        authenticate(otherOwner);

        assertThrows(ResourceNotFoundException.class, () -> watchService.get(created.id()));
        assertThrows(ResourceNotFoundException.class, () -> watchService.history(created.id()));
        assertEquals(0, watchService.list().size());
    }

    @Test
    void invalidUrlsAndPricesAreRejectedBeforePersistence() {
        assertThrows(IllegalArgumentException.class, () -> watchService.create(
                new CreateProductWatchRequest("Unsafe", "https://127.0.0.1/private", null)));
        assertThrows(IllegalArgumentException.class, () -> watchService.create(
                new CreateProductWatchRequest("Wrong host", "https://other.example/product", null)));
        assertThrows(IllegalArgumentException.class, () -> watchService.create(
                new CreateProductWatchRequest("Negative", "https://shop.example/product",
                        new BigDecimal("-1"))));
        assertEquals(0, watchService.list().size());
    }

    @Test
    void concurrentObservationAndDeliveryAttemptsPersistOneTransitionAndOneClaim() throws Exception {
        ProductWatchResponse created = watchService.create(new CreateProductWatchRequest(
                "Concurrent product", "https://shop.example/concurrent", new BigDecimal("4000")));
        observationRecorder.record(created.id(), new StoreObservation(
                new BigDecimal("5000"), StockStatus.IN_STOCK, Instant.parse("2026-06-01T10:00:00Z")));

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            var first = executor.submit(() -> {
                start.await();
                observationRecorder.record(created.id(), new StoreObservation(
                        new BigDecimal("3900"), StockStatus.IN_STOCK, Instant.parse("2026-06-01T10:01:00Z")));
                return true;
            });
            var second = executor.submit(() -> {
                start.await();
                observationRecorder.record(created.id(), new StoreObservation(
                        new BigDecimal("3900"), StockStatus.IN_STOCK, Instant.parse("2026-06-01T10:02:00Z")));
                return true;
            });
            start.countDown();
            first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            second.get(20, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        var events = notificationRepository.findAllByWatch_Id(created.id());
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(
                event -> event.getStatus() == NotificationDeliveryStatus.PENDING));
        assertEquals(2, events.stream().map(ProductNotificationEvent::getEventKey).distinct().count());

        ProductNotificationEvent duplicate = new ProductNotificationEvent(
                watchRepository.findById(created.id()).orElseThrow(),
                events.getFirst().getEventType(),
                events.getFirst().getPreviousPrice(),
                events.getFirst().getCurrentPrice(),
                events.getFirst().getTargetPrice(),
                Instant.now(),
                events.getFirst().getEventKey());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> notificationRepository.saveAndFlush(duplicate));

        Long eventId = events.getFirst().getId();
        java.util.concurrent.ExecutorService claims = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch claimStart = new java.util.concurrent.CountDownLatch(1);
        try {
            var firstClaim = claims.submit(() -> {
                claimStart.await();
                return notificationStatusService.claim(eventId, Instant.now());
            });
            var secondClaim = claims.submit(() -> {
                claimStart.await();
                return notificationStatusService.claim(eventId, Instant.now());
            });
            claimStart.countDown();
            assertEquals(1, (firstClaim.get(20, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
                    + (secondClaim.get(20, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0));
        } finally {
            claims.shutdownNow();
        }
        ProductNotificationEvent claimed = notificationRepository.findById(eventId).orElseThrow();
        assertEquals(NotificationDeliveryStatus.SENDING, claimed.getStatus());
        assertEquals(1, claimed.getAttemptCount());
    }

    private UserAccount user(String email) {
        UserAccount user = new UserAccount();
        user.setName("Product watch test");
        user.setEmail(email);
        user.setPassword("unused-test-hash");
        return userRepository.saveAndFlush(user);
    }

    private void authenticate(UserAccount user) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
}
