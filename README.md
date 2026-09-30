# Inventory Management System

A Spring Boot REST API for managing products, warehouses, inventory levels, stock transfers, and stock movement history. The application uses Spring Data JPA with PostgreSQL and applies transactional business rules so inventory changes and their movement records stay synchronized.

## Features

- Create, retrieve, update, and delete products.
- Create, retrieve, update, and delete warehouses.
- Track a product's quantity independently at each warehouse.
- Adjust stock with typed inbound, outbound, and adjustment movements.
- Transfer stock between warehouses in one transaction.
- View low-stock locations and per-location movement history.
- Return consistent HTTP errors for missing resources, invalid requests, and conflicts.

## Technology Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
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
## API Reference

All endpoints use the base URL `http://localhost:8080`. Request and response fields retain the existing snake_case convention where applicable.

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

The current `OrderServiceTest` references customer/order packages that are not present in `src/main`; update or remove that obsolete test before relying on a Maven test run.

## Project Structure

```text
src/
├── main/
│   ├── java/com/miyuki/Inventory/Management/
│   │   ├── common/
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