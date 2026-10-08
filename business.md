/*
    Inventory-Management
*/

1. Problem : How to manage a inventory ?
2. Actors : Customer, Inventory Manager, System
3. Entities : Customer Products
4. Inventory states : 
    Total Inventory = Available Inventory + Consumed Inventory
6. Operations
    Add Products
    Update Product-stock
    Delete Product
    Buy Order
    Cancel Order
    Pending Order
7. Business rules
    No more buy order than available inventory
    First come first Buy
8. Failure cases
       if consumed inventory > available  inventory : failed
       if available inventory : 1
        a buys and b buys then a accepted but b failed
      if available inventory : 5
        a buys 3 and b buys 4 then only a buys b failed
      if same customer buys twice will it get recognized
10. Concurrency scenarios
    if two people ordered at the same time
    if available is 10 and orders came in 100 : 10 accepted 90 rejected
    if same custome buys twice

## Implementation change log and decisions

### 2026-10-08

- File: `src/main/java/com/miyuki/Inventory/Management/order/OrderService.java`
  - Accepted orders now stay `PENDING` while their stock is reserved in the same transaction.
  - Inventory rows are pessimistically locked in product/warehouse order. The first transaction to acquire a row lock is evaluated first; insufficient stock returns a conflict and does not create an order.
  - Repeated lines for the same product and warehouse are combined. Separate orders from the same customer are allowed while stock remains.
  - Cancellation locks the order, restores stock only for pending orders, and is idempotent for already-cancelled orders.
  - Order reservations and cancellations are recorded as outbound and inbound stock movements.

- File: `src/main/java/com/miyuki/Inventory/Management/order/OrderRepository.java`
  - Added a pessimistic order lock so concurrent cancellation requests cannot restore stock more than once.

- File: `src/main/java/com/miyuki/Inventory/Management/common/exception/GlobalExceptionHandler.java`
  - Insufficient-stock failures now return HTTP 409 Conflict.

- File: `src/test/java/com/miyuki/Inventory/Management/order/OrderServiceTest.java`
  - Added tests for pending order creation, insufficient stock rejection, repeated-line aggregation, cancellation/idempotency, and stock movement directions.

- File: `src/test/java/com/miyuki/Inventory/Management/order/OrderConcurrencyIntegrationTest.java`
  - Added a PostgreSQL-backed test that submits 100 concurrent one-unit orders against 10 units and verifies exactly 10 orders succeed without overselling.

### 2026-10-01

- File: `src/main/java/com/miyuki/Inventory/Management/customer/Customer.java`
  - Added a customer entity so orders can be tied to a real customer record.
  - Decision: keep the customer model minimal and focused on identity (name and email) because the project requirement is centered on order handling rather than advanced user management.

- File: `src/main/java/com/miyuki/Inventory/Management/customer/CustomerController.java`
  - Added create and list endpoints for customer records.
  - Decision: keep customer endpoints simple and aligned with the existing project structure, which uses singular route naming like `/product` and `/warehouse`.

- File: `src/main/java/com/miyuki/Inventory/Management/customer/CustomerService.java`
  - Added validation for required customer fields and duplicate email protection.
  - Decision: reject duplicate customer emails early to preserve a clean ownership model for order records.

- File: `src/main/java/com/miyuki/Inventory/Management/order/Order.java`
  - Added the order aggregate with status tracking and item listing.
  - Decision: use a single order record with a status enum so the project can represent pending, confirmed, and cancelled orders using the business rules already described.

- File: `src/main/java/com/miyuki/Inventory/Management/order/OrderItem.java`
  - Added order items linked to a product and warehouse.
  - Decision: store the real product and warehouse references so each order item can be validated against the current inventory record.

- File: `src/main/java/com/miyuki/Inventory/Management/order/OrderService.java`
  - Added the main order-processing logic, including inventory validation and stock reduction during order creation.
  - Decision: apply stock checks and reduce quantity inside a single transaction to reduce overselling risk when orders are created concurrently.

- File: `src/main/java/com/miyuki/Inventory/Management/order/OrderController.java`
  - Added create, list, detail, pending, and cancel endpoints.
  - Decision: match the project’s established API style and support the minimal lifecycle expected by the business requirement: place order, inspect it, and cancel it.

- File: `src/main/java/com/miyuki/Inventory/Management/order/dto/CreateOrderRequest.java`
  - Added the request payload for placing an order.
  - Decision: require the customer ID and a list of items so the order is valid without hidden assumptions.

- File: `src/main/java/com/miyuki/Inventory/Management/order/dto/CreateOrderItemRequest.java`
  - Added the per-item purchase payload.
  - Decision: include product, warehouse, and quantity because inventory in this project is tracked per product and warehouse.

- File: `src/main/java/com/miyuki/Inventory/Management/order/dto/OrderResponse.java`
  - Added the response model for order details.
  - Decision: return the order contents with the customer ID and status so clients can track the transaction life cycle clearly.

- File: `src/main/java/com/miyuki/Inventory/Management/order/dto/OrderItemResponse.java`
  - Added item-level order output.
  - Decision: keep item responses lightweight while exposing enough data to identify product, warehouse, and quantity.

- File: `src/main/java/com/miyuki/Inventory/Management/customer/dto/CreateCustomerRequest.java`
  - Added the customer creation payload.
  - Decision: keep this minimal by validating only fundamental identity fields.

- File: `src/main/java/com/miyuki/Inventory/Management/customer/dto/CustomerResponse.java`
  - Added the customer read model.
  - Decision: keep response objects consistent with the rest of the project’s DTO strategy.

### Summary of implementation decisions

- Orders are created against a customer and are validated against warehouse-level inventory before stock is reduced.
- Accepted orders remain pending while reserving stock; cancelling a pending order restores the stock and records the movement.
- Transactional inventory and order locks prevent concurrent requests from allocating the same units or cancelling the same order twice.
- Repeated lines in one order are combined by product and warehouse. Separate orders from one customer are allowed if stock remains.
- The implementation intentionally focuses on the order lifecycle described in the business document rather than adding unrelated features like payment or shipping.
- Service-level tests cover the main workflow, and a PostgreSQL-backed test verifies concurrent allocation against the configured database.