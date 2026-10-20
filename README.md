# Inventory Management System

A Spring Boot REST API for managing products, warehouses, inventory levels, stock transfers, and stock movement history. The application uses Spring Data JPA with PostgreSQL and applies transactional business rules so inventory changes and their movement records stay synchronized.

## Features

- Create, retrieve, update, and delete products.
- Create, retrieve, update, and delete warehouses.
- Track a product's quantity independently at each warehouse.
- Adjust stock with typed inbound, outbound, and adjustment movements.
- Transfer stock between warehouses in one transaction.
- View low-stock locations and per-location movement history.
- Monitor warehouse inventory on a schedule and receive Discord alerts when a low-stock condition begins.
- Manage external product watches and record store observations independently from warehouse stock.
- Return consistent HTTP errors for missing resources, invalid requests, and conflicts.

## Implemented Phases

### Phase 1 — Scheduled internal warehouse alerts

The application periodically checks account-owned product/warehouse inventory using the existing low-stock rules. A transition below the reorder level creates a durable Discord notification; repeated checks during the same low-stock episode do not create duplicates. Stock recovery allows a later low-stock transition to alert again. Monitoring does not change inventory, orders, reservations, or transfers.

### Phase 2 — External product watches

Authenticated users can create and manage account-scoped product watches, configure target prices, and view successfully saved observation history. HTTPS URLs are checked against the exact-host allowlist. **Automated retailer fetching is not active:** no retailer/API was specified and no production store checker is registered. The watch API and observation logic are implemented, but the scheduled monitor does not fetch a retailer product until a verified store-specific checker is added.

### Phase 3 — Reliable notification delivery

Internal and external notifications use a persistent outbox with unique event keys, atomic delivery claims, retry/backoff handling, Discord rate-limit support, and recovery of stale delivery claims. Delivery is at-least-once, not exactly-once. The scheduled jobs prevent overlapping runs in one application instance; they are not a distributed scheduler.

## Technology Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Spring Security with stateless JWT bearer authentication
- PostgreSQL
- Maven Wrapper
- JUnit and Spring Boot test support

## Architecture

The project follows a layered architecture:

```text
Controller (DTO)  ->  Service  ->  Repository  ->  PostgreSQL
							  |                 |
						  Mapper          JPA Entity
```

- **Controllers** expose HTTP endpoints using request and response DTOs.
- **Mappers** convert between DTOs and JPA entities.
- **Services** contain validation and inventory business rules.
- **Repositories** provide Spring Data JPA persistence operations.
- **Entities** represent products, warehouses, warehouse-specific inventory, and stock movements.

The main packages are located under `src/main/java/com/miyuki/Inventory/Management`:

```text
product/
warehouses/
stock/
common/
```

## Prerequisites

Install the following before running the application:

- JDK 21 or later
- PostgreSQL
- A PostgreSQL database named `inventory`

The Maven Wrapper is included, so a separate Maven installation is not required.

## Database Configuration

The default configuration is stored in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/inventory
spring.datasource.username=postgres
spring.datasource.password=2003
```

Create the database before starting the application:

```sql
CREATE DATABASE inventory;
```

Update the username, password, host, port, or database name in `application.properties` to match the local PostgreSQL installation. For shared or production environments, do not commit credentials; provide them through environment-specific configuration or secret management.

Set `JWT_SECRET` to a Base64-encoded random value containing at least 32 bytes before starting the application. For example, in PowerShell:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
./mvnw.cmd spring-boot:run
```

Use a stable secret from a secret manager for deployed environments. Tokens are signed with HS256 and expire after one hour. Registration issues a token immediately; passwords must contain at least 12 characters and fit within bcrypt's 72-byte input limit.

Hibernate is currently configured with `spring.jpa.hibernate.ddl-auto=update`, which allows Hibernate to update the schema during development. Use an explicit migration tool and a stricter schema strategy for production deployments. When upgrading an existing database, review the existing global email/SKU/code unique constraints: Hibernate does not automatically replace them with the new per-account uniqueness constraints.

## Running the Application

From the project root, run:

### Windows

```powershell
./mvnw.cmd spring-boot:run
```

### Linux or macOS

```bash
./mvnw spring-boot:run
```

The application starts on the default Spring Boot port:

```text
http://localhost:8080
```

## Authentication and account isolation

Create an account using `POST /auth/register`:

```json
{
  "name": "Sam Example",
  "email": "sam@example.com",
  "password": "a-long-unique-password"
}
```

The response contains an `accessToken` and a user profile. Sign in with `POST /auth/login` using the same email and password. Send the returned token on every other endpoint using the `Authorization: Bearer <accessToken>` header:

```http
Authorization: Bearer <accessToken>
```

In Postman, first call `POST /auth/register` or `POST /auth/login` with a JSON body and copy `accessToken` from the successful response. For protected requests, open the **Authorization** tab, choose **Bearer Token**, and paste the token value without the `Bearer` prefix (Postman adds it). Alternatively, add an `Authorization` header with the bearer token. Registration and login do not require a token.

`GET /users/me` returns the authenticated user's profile. New accounts receive the `USER` role. User identity and ownership are taken from the signed token, never from a request-supplied user ID. Products, warehouses, customers, inventory, movements, and orders are all isolated by account; requests for another account's resource return not found (or omit it from collections). An order can only use that account's customer, products, and warehouses.

The application can add nullable `owner_id` columns to an existing schema, but existing rows are not assigned to an account automatically and remain unavailable through account-scoped APIs. Back up the database and assign existing customer, product, and warehouse rows to an account before deployment if that data must be retained. New records created through the authenticated API are always assigned to the current account.

## API Reference

All endpoints use the base URL `http://localhost:8080`. Except for registration and login, requests require a bearer token. Request and response fields retain the existing snake_case convention where applicable.

### Authentication and users

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/auth/register` | Create an account and receive an access token |
| `POST` | `/auth/login` | Authenticate and receive an access token |
| `GET` | `/users/me` | Get the authenticated user's profile |

### Customers

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/customer` | Create a customer |
| `GET` | `/customer` | List the current account's customers |
| `GET` | `/customer/{id}` | Get one customer |

### Products

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/product` | Create a product (`sku`, `name`, `category`, `reorder_level`) |
| `GET` | `/product` | List products |
| `GET` | `/product/{id}` | Get one product |
| `PUT` | `/product/{id}` | Replace product details |
| `DELETE` | `/product/{id}` | Delete a product |

### Warehouses

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/warehouse` | Create a warehouse (`code`, `name`, `capacity`) |
| `GET` | `/warehouse` | List active warehouses |
| `GET` | `/warehouse/{id}` | Get one warehouse |
| `PUT` | `/warehouse/{id}` | Update `code`, `name`, `capacity`, and `isActive` |
| `DELETE` | `/warehouse/{id}` | Delete a warehouse when it has no inventory locations |

### Inventory

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/inventory/adjustments` | Apply an inbound, outbound, or adjustment movement |
| `POST` | `/inventory/transfers` | Transfer a positive quantity between warehouses |
| `GET` | `/inventory` | List inventory; optionally filter with `?warehouse_id={id}` |
| `GET` | `/inventory/low-stock` | List locations below their product reorder level |
| `GET` | `/inventory/{productId}/{warehouseId}` | Get stock at a location |
| `GET` | `/inventory/{productId}/{warehouseId}/movements` | Get movement history for a location |

`POST /inventory` is also accepted as an alias for `POST /inventory/adjustments`.

### Orders

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/order` | Create an order and reserve stock |
| `GET` | `/order` | List the current account's orders |
| `GET` | `/order/{id}` | Get one order |
| `GET` | `/order/pending` | List pending orders |
| `PUT` | `/order/{id}/cancel` | Cancel an order and release its reservation |

### External product watches

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/watches` | Create an external product watch |
| `GET` | `/api/watches` | List the current account's watches |
| `GET` | `/api/watches/{id}` | Get a watch owned by the current account |
| `PATCH` | `/api/watches/{id}` | Change `targetPrice`, `enabled`, or clear the target |
| `DELETE` | `/api/watches/{id}` | Delete a watch and its observation/notification history |
| `GET` | `/api/watches/{id}/history` | List successfully persisted observations |

Example inbound adjustment:

```json
{
  "product_id": 1,
  "warehouse_id": 1,
  "quantity": 25,
  "movementType": "INBOUND"
}
```

Outbound adjustments use a negative `quantity`; inbound adjustments use a positive quantity. Use `ADJUSTMENT` for either direction. Transfers use `product_id`, `from_warehouse_id`, `to_warehouse_id`, and positive `quantity`; the source and destination must differ.

## Transaction Behavior

Stock adjustments and transfers use Spring transactions. Each balance change and its corresponding stock movement are committed together or rolled back together. Outbound changes and transfers cannot reduce a location below zero.

## Scheduled low-stock monitoring

The application checks each inventory item at a fixed delay (five minutes by default) and sends a Discord incoming-webhook message when its quantity first falls below the product's reorder level. Monitoring considers only inventory whose product and warehouse belong to the same account. It reads inventory but never changes quantities, reservations, orders, or transfers.

Set `DISCORD_WEBHOOK_URL` to the Discord incoming webhook URL to enable delivery. Without it, the application still starts, but each attempted alert is reported as a configuration error and is not marked delivered. Set `MONITOR_INVENTORY_INTERVAL_MS` to a positive delay in milliseconds to change the interval; for example, `60000` checks every minute. Do not commit webhook URLs or expose them in logs.

To create the webhook in Discord, open the target channel's **Edit Channel → Integrations → Webhooks**, choose **New Webhook**, then **Copy Webhook URL** and provide it as `DISCORD_WEBHOOK_URL`. Restart the application after setting environment variables.

The `inventory_alert_state` table stores each inventory item's current low-stock episode. A successful notification is not recreated on later checks while stock remains low. When stock recovers to or above the reorder level, the episode is cleared; a subsequent drop increments its episode key and can notify again. The transition and its durable notification event are written in one database transaction.

To verify locally, set a test webhook URL and optionally `MONITOR_INVENTORY_INTERVAL_MS=60000`, start the app using the instructions above, and adjust an item's stock below its reorder level. Confirm the message in the configured channel, check that another interval does not repeat it, restore stock, then lower it again. Automated tests mock the HTTP client and never post to Discord. Run them with `./mvnw.cmd test` on Windows (or `./mvnw test` on Linux/macOS).

## External product watches

External products are stored in a separate watch/observation domain. They never change internal inventory, stock movements, orders, reservations, or transfers. The API endpoints are listed in the API Reference above.

Example create request:

```json
{
  "productName": "Wireless Headphones",
  "productUrl": "https://your-supported-store.example/product/123",
  "targetPrice": 4000
}
```

Requests require the same bearer token as the other protected APIs. The account owner is derived from the authenticated principal; no account ID is accepted from the client. Watch URLs must be HTTPS and use an exact host in `EXTERNAL_MONITOR_ALLOWED_HOSTS` (comma-separated). Local, private-IP literals, credential-bearing URLs, alternate ports, and lookalike subdomains are rejected. The allowlist is empty by default.

`EXTERNAL_MONITOR_INTERVAL_MS` configures the fixed delay (default 900000 ms); `EXTERNAL_MONITOR_BATCH_SIZE` controls bounded database pages (default 50, maximum 500). `DISCORD_WEBHOOK_URL` is the existing shared Discord webhook configuration. Price values are sent as observed numeric values; the application does not infer or add currency symbols.

**Retailer integration status:** this repository does not identify a retailer or provide an official product API/page structure, so no production `StoreChecker` or scraper is enabled. The `StoreChecker` interface and test-only deterministic fixtures establish the extension point, while watch CRUD, exact-host validation, persisted observation/change handling, scheduler batching, and Discord notification outbox are implemented. Until a concrete checker is added and its official API or real page structure is verified, configured watches cannot be automatically fetched; the monitor logs that no checker is registered and does not treat the product as out of stock. Do not add arbitrary domains to the allowlist as a substitute. A store-specific checker must enforce connection/read timeouts, public DNS/IP validation at connection time, and equivalent validation for every redirect to prevent DNS-rebinding and redirect-based SSRF.

The first successful check is a baseline and generates no alert. Later checks alert on a price decrease, an out-of-stock to in-stock transition, and a target crossing from above to at/below the configured target. Setting or changing a target does not itself generate an immediate target alert; the first observation using the new target establishes its baseline. Failed checks do not alter the last successful price, stock state, or observation history.

Internal and external alerts share the `product_notification_event` durable outbox. Events use a unique logical event key, start as `PENDING`, are conditionally claimed as `SENDING` with a two-minute lease, then become `SENT`, retryable `FAILED`, or terminal `EXHAUSTED`. The database claim is an atomic conditional update, so only one worker sends a particular event at a time; expired claims can be recovered after a process crash. Network/5xx failures and Discord 429 responses retry with exponential backoff and jitter (15 seconds initially, capped at six hours); a valid `Retry-After` value is honored. Configuration and other permanent 4xx errors are not retried. Delivery stops after eight attempts. A delivery cycle logs counts of pending, exhausted, sent, failed, and skipped events, and logs the latest safe failure summary for diagnosis.

The `NOTIFICATION_DELIVERY_INTERVAL_MS` setting (default 10000 ms) controls the independent outbox dispatcher. Pending records survive application restarts and are retried when the dispatcher runs again. To recover an exhausted event, correct the webhook/configuration issue, inspect the `product_notification_event` row and `last_error_summary`, then explicitly reset that row to `PENDING`, set `attempt_count` to `0`, set `next_attempt_at` to the current database time, and clear `last_error_summary`. Delivery and database commit are not atomic, so a network failure after Discord accepts a message can result in a duplicate retry; exactly-once delivery is not claimed.

The internal and external scheduled methods prevent overlap within one application instance. Atomic outbox claims protect delivery across instances, and external watch rows are pessimistically locked only during the short observation transaction (never while contacting the retailer or Discord). Internal episode creation is guarded by persistent state and the event-key uniqueness constraint. This is not a distributed scheduler: multi-instance deployments can repeat retailer checks and internal scan work; use one active scheduler instance or introduce a shared job-claiming/locking strategy if multiple instances are deployed.

Hibernate's existing `ddl-auto=update` creates new tables and indexes in development. This repository has no migration runner. For an existing Phase 2 PostgreSQL database, review and manually apply [V3__durable_notification_outbox.sql](./src/main/resources/db/migration/V3__durable_notification_outbox.sql) before deploying; it backfills old event rows, adds delivery state and unique event keys, and initializes internal episode counters. Keep the new columns and data when rolling back application code; do not drop the outbox table or episode state if pending notifications must be retained. Automated tests use deterministic checker fixtures and mock Discord; they do not depend on a live retailer or webhook.

## Testing

### Automated tests

Use JDK 21 and make sure the configured PostgreSQL test database is available. From the repository root run the full suite:

### Windows

```powershell
.\mvnw.cmd clean test
```

### Linux or macOS

```bash
./mvnw clean test
```

To run selected feature tests on Windows:

```powershell
.\mvnw.cmd "-Dtest=LowStockAlertServiceTest,LowStockAlertIsolationIntegrationTest,DiscordNotificationServiceTest,ProductNotificationOutboxTest,ProductNotificationEventStatusServiceTest,ProductObservationRecorderTest,ExternalProductMonitoringServiceTest,ProductWatchIntegrationTest" test
```

These tests cover low-stock thresholds and episode deduplication, recovery/new episodes, account isolation, per-record failure handling, notification HTTP errors and retries, outbox persistence, and external watch observations. Discord requests are mocked; tests do not post to a real webhook. The full suite also covers order reservation/cancellation and concurrent stock allocation.

### Manual API smoke test

1. Start PostgreSQL and the application, then register with `POST /auth/register` or sign in with `POST /auth/login`. Copy the returned `accessToken`; use it as a bearer token for all following requests.
2. Create a product with a reorder level (for example `10`) using `POST /product`, and create a warehouse using `POST /warehouse`. Save their returned IDs.
3. Add stock above the threshold using `POST /inventory/adjustments`, replacing the example IDs with the IDs returned in step 2:

   ```json
   {
     "product_id": 1,
     "warehouse_id": 1,
     "quantity": 12,
     "movementType": "INBOUND"
   }
   ```

4. Call `GET /inventory/low-stock` to confirm it is not listed. Set `MONITOR_INVENTORY_INTERVAL_MS=60000` before starting the application if you want to test without waiting for the five-minute default.
5. Reduce the stock below the threshold with another adjustment, for example `quantity: -3` and `movementType: "OUTBOUND"`. Confirm `GET /inventory/low-stock` lists it. If `DISCORD_WEBHOOK_URL` is configured, check the destination channel for the alert. Do not use a production webhook for a smoke test.
6. Wait for another monitoring cycle: the same low-stock episode should not alert again. Restore stock to the reorder level or above, then reduce it below the threshold again to verify a new episode can alert.
7. For external watches, `POST /api/watches` and `GET /api/watches` exercise watch management. Since no production `StoreChecker` is registered, retailer fetching cannot be manually verified against a real store yet; the automated tests use deterministic fixtures for observation and transition behavior.

Run the application with the same database configuration described above. Configure a Discord webhook only when you intend to send an actual test notification; leaving it unset does not prevent startup, but attempted deliveries will be reported as configuration failures.

## Project Structure

```text
src/
├── main/
│   ├── java/com/miyuki/Inventory/Management/
│   │   ├── common/
│   │   ├── user/
│   │   ├── product/
│   │   ├── stock/
│   │   └── warehouses/
│   └── resources/
│       ├── application.properties
│       ├── static/
│       └── templates/
└── test/
		└── java/com/miyuki/Inventory/Management/
```

## Operational Notes

- Database credentials are currently configured directly in `application.properties`; externalize them for shared or production environments.
- Inventory is now keyed by product and warehouse, and the inventory/movement table names have been normalized. Back up and migrate existing database data before applying this model to a database containing earlier inventory records.
- Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update` for development. Use explicit database migrations and a stricter schema strategy for production.

## License

No license has been specified for this project.