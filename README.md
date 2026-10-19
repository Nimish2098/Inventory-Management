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
- Return consistent HTTP errors for missing resources, invalid requests, and conflicts.

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

### Create a Product

```http
POST /product
## API Reference

All endpoints use the base URL `http://localhost:8080`. Except for registration and login, requests require a bearer token. Request and response fields retain the existing snake_case convention where applicable.

### Authentication and users

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/auth/register` | Create an account and receive an access token |
| `POST` | `/auth/login` | Authenticate and receive an access token |
| `GET` | `/users/me` | Get the authenticated user's profile |

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

The `inventory_alert_state` table stores each inventory item's current low-stock episode and delivery status. A successful notification is not resent on later checks while stock remains low. When stock recovers to or above the reorder level, the episode is cleared; a subsequent drop can notify again. If delivery fails or Discord rate-limits the request, the episode remains pending and the next monitoring cycle retries it. Discord delivery and database persistence cannot form one atomic transaction, so an ambiguous network failure after Discord accepts a message can result in a retry.

To verify locally, set a test webhook URL and optionally `MONITOR_INVENTORY_INTERVAL_MS=60000`, start the app using the instructions above, and adjust an item's stock below its reorder level. Confirm the message in the configured channel, check that another interval does not repeat it, restore stock, then lower it again. Automated tests mock the HTTP client and never post to Discord. Run them with `./mvnw.cmd test` on Windows (or `./mvnw test` on Linux/macOS).

## Testing

Run the full test suite with the Maven Wrapper and JDK 21:

### Windows

```powershell
./mvnw.cmd test
```

### Linux or macOS

```bash
./mvnw test
```

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