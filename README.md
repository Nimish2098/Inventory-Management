# Inventory Management System

A Spring Boot REST API for managing products and inventory and for processing customer orders. The application uses Spring Data JPA with PostgreSQL and applies transactional business rules so that order creation and cancellation keep order state and inventory synchronized.

## Features

- Create products with a name, price, and available stock.
- Update product stock and delete products.
- Create orders for existing customers.
- Validate customers, products, quantities, and available inventory before placing an order.
- Deduct inventory automatically when an order is confirmed.
- Cancel confirmed orders and restore the exact purchased quantities.
- Roll back inventory and order changes when a transactional operation fails.
- Persist order item quantities so cancellation remains accurate for multi-item orders.

## Technology Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Maven Wrapper
- JUnit and Spring Boot test support

## Architecture

The project follows a conventional layered architecture:

```text
Controller  ->  Service  ->  Repository  ->  PostgreSQL
```

- **Controllers** expose HTTP endpoints and translate requests into service calls.
- **Services** contain validation and inventory/order business rules.
- **Repositories** provide Spring Data JPA persistence operations.
- **Models** represent customers, products, orders, and order item quantities.

The main packages are located under `src/main/java/com/miyuki/Inventory/Management`:

```text
Controller/
Model/
Repository/
Service/
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

Hibernate is currently configured with `spring.jpa.hibernate.ddl-auto=update`, which allows Hibernate to update the schema during development. Use an explicit migration tool and a stricter schema strategy for production deployments.

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

## API Reference

### Create a Product

```http
POST /product
Content-Type: application/json
```

Request body:

```json
{
	"product_name": "Wireless Mouse",
	"product_stock": 10,
	"price": 29.99
}
```

### Update Product Stock

```http
PUT /product/update-stock/{productId}?stock=25
```

Example:

```bash
curl -X PUT "http://localhost:8080/product/update-stock/1?stock=25"
```

### Delete a Product

```http
DELETE /product/{productId}
```

Example:

```bash
curl -X DELETE http://localhost:8080/product/1
```

### Create an Order

```http
POST /orders
Content-Type: application/json
```

Request body:

```json
{
	"customerId": 1,
	"items": [
		{
			"productId": 1,
			"quantity": 3
		}
	]
}
```

When the request succeeds, the order is saved with status `CONFIRMED` and the product stock is reduced by the requested quantity.

### Get an Order

```http
GET /orders/{id}
```

Example:

```bash
curl http://localhost:8080/orders/1
```

### Cancel an Order

```http
POST /orders/{id}/cancel
```

Example:

```bash
curl -X POST http://localhost:8080/orders/1/cancel
```

Only orders with status `CONFIRMED` can be cancelled. A successful cancellation changes the status to `CANCELLED` and restores the quantities originally purchased.

## Transaction Behavior

Order creation and cancellation are wrapped in Spring transactions using `@Transactional(rollbackFor = Exception.class)`.

For order creation:

1. The customer and requested products are validated.
2. Stock availability is checked.
3. Product stock is reduced.
4. The order and its item quantities are saved.
5. All changes are committed together.

If any step fails, the transaction is rolled back so that a partial order or partial stock update is not left in the database.

For cancellation:

1. The order is loaded and its status is checked.
2. The recorded quantity for each product is restored.
3. The order status is changed to `CANCELLED`.
4. The inventory and status change are committed together.

## Testing

Run the full test suite with the Maven Wrapper:

### Windows

```powershell
./mvnw.cmd test
```

### Linux or macOS

```bash
./mvnw test
```

The order service tests cover successful order creation, insufficient-inventory rollback, and cancellation with inventory restoration.

## Project Structure

```text
src/
├── main/
│   ├── java/com/miyuki/Inventory/Management/
│   │   ├── Controller/
│   │   ├── Model/
│   │   ├── Repository/
│   │   └── Service/
│   └── resources/
│       ├── application.properties
│       ├── static/
│       └── templates/
└── test/
		└── java/com/miyuki/Inventory/Management/
```

## Current Limitations

- Customer persistence exists, but customer HTTP endpoints are not currently exposed by `CustomerController`.
- Product creation, stock updates, and deletion are exposed; product retrieval endpoints are not currently exposed by `ProductController`.
- Error responses currently rely on Spring's default exception handling rather than a dedicated API error format.
- Database credentials are currently configured directly in `application.properties` and should be externalized for production.
- Concurrent stock updates do not yet use pessimistic or optimistic locking.

## License

No license has been specified for this project.