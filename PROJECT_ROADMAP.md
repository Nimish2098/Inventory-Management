# Inventory Management — Focused Roadmap

**Reviewed:** 2026-10-18

## What already exists

- Product and warehouse management.
- Warehouse-level inventory, stock adjustments, low-stock alerts using a fixed reorder level, and stock movement history.
- Transactional warehouse transfers.
- Customer orders with stock reservation, cancellation, and concurrency protection.
- JWT authentication and account-level data isolation.

These are existing capabilities, not new roadmap scope. The order workflow is implemented. See [CHANGELOG.md](./CHANGELOG.md) for recorded changes.

## Goal

Deliver only the three requested capabilities, reusing the current API and domain wherever possible:

1. **Dynamic Safety Stock Generator** — recommend a reorder point using actual demand and replenishment lead time.
2. **Multi-Warehouse FEFO Transfer Engine** — identify eligible stock nearing expiry and recommend a transfer to a location that needs it.


## Minimum roadmap

### 1. Add only the data these capabilities need

- [ ] Record dated demand that represents actual consumption. Do not count cancelled orders or assume a stock reservation is a completed sale.
- [ ] Store replenishment lead time for the relevant product.
- [ ] Track stock by lot/batch with an optional expiry date and remaining quantity. Keep lot totals consistent with existing warehouse inventory.
- [ ] Preserve account isolation and transactional stock/movement behavior.

**Done when:** Demand, lead time, and expiry information can be saved and retrieved accurately for each account, and lot quantities reconcile with current inventory.

### 2. Build and validate the two recommendation capabilities

- [ ] **Safety stock:** Calculate an explainable reorder point from demand and lead time. Mark insufficient history clearly and use the existing fixed reorder level as the fallback.
- [ ] **FEFO:** Rank eligible lots by earliest expiry and suggest transfers only when another warehouse needs the stock and the move is feasible.
- [ ] Provide a preview with the recommendation and its rationale. Recheck stock and eligibility before an approved transfer is executed.
- [ ] Keep recommendations read-only; do not automatically purchase or transfer stock.
- [ ] Add focused tests for normal cases, missing data, expiry order, insufficient stock, concurrent changes, and account isolation.

**Done when:** Both recommendations are useful and explainable, and an approved transfer safely updates inventory and movement history.

### 3. Add the natural-language assistant

- [ ] Add Spring AI with one selected provider; keep its credentials in environment configuration.
- [ ] Expose only the tools needed to query inventory, get recommendations, preview a transfer, and execute an approved transfer.
- [ ] Use the signed-in user's account context for every tool call. Validate tool arguments and recheck inventory rules in backend services.
- [ ] Require explicit user confirmation before any stock-changing action. The assistant cannot bypass existing authorization or transfer logic.
- [ ] Add focused tests for tool calls, invalid or ambiguous requests, cross-account access, confirmation, and provider/tool errors.

**Done when:** A manager can ask supported questions and request a transfer in plain language, with execution still controlled by the existing secure backend.

### 4. Verify and release

- [ ] Run the existing test suite and the focused new tests.
- [ ] Demonstrate the complete path: record demand and expiring lots → view recommendations → ask the assistant → confirm a transfer → verify stock and movement history.
- [ ] Document the minimum setup and usage for the new capabilities.

**Done when:** The end-to-end demonstration passes without breaking existing inventory, order, or account-isolation behavior.

## API and required data

All examples use `http://localhost:8080`. Existing protected routes require:

```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

The API examples below distinguish **existing** routes from a **proposed contract** for features not implemented yet. Proposed names and fields can be adjusted during implementation, but should stay limited to this roadmap.

Existing authentication payloads:

```json
{
  "name": "Sam Example",
  "email": "sam@example.com",
  "password": "a-long-unique-password"
}
```

Use that body for `POST /auth/register`; `POST /auth/login` uses `email` and `password` only. Both return an access token for the protected API examples.

### Existing APIs to reuse

| Method and path | Purpose | Request |
| --- | --- | --- |
| `POST /auth/register` / `POST /auth/login` | Obtain a token | Existing auth request payloads |
| `POST /product` | Create a product | `{"sku":"SKU-100","name":"Tea","category":"Grocery","reorder_level":20}` |
| `GET /product` | Read products | No body |
| `POST /inventory/adjustments` | Add or adjust stock | `{"product_id":1,"warehouse_id":1,"quantity":100,"movementType":"INBOUND"}` |
| `GET /inventory` | Read current warehouse stock | No body; optional `?warehouse_id=1` |
| `GET /inventory/low-stock` | Read existing fixed-threshold alerts | No body |
| `POST /inventory/transfers` | Existing manual warehouse transfer | `{"product_id":1,"from_warehouse_id":1,"to_warehouse_id":2,"quantity":10}` |
| `GET /inventory/{productId}/{warehouseId}/movements` | Read stock movement history | No body |
| `POST /order` | Create an order; only completed consumption should feed demand | `{"customer_id":1,"items":[{"product_id":1,"warehouse_id":1,"quantity":2}]}` |

For example, an existing inventory response has this shape:

```json
{
  "id": 11,
  "product_id": 1,
  "product_sku": "SKU-100",
  "warehouse_id": 1,
  "warehouse_code": "WH-EAST",
  "quantity": 100,
  "bin": null,
  "reorder_level": 20,
  "low_stock": false
}
```

### Minimum data to add

| Data | Required fields | Used by |
| --- | --- | --- |
| Actual demand event | `product_id`, `warehouse_id`, positive `quantity`, `occurred_at` timestamp | Safety stock |
| Replenishment policy | `lead_time_days`, `target_service_level` per product (or product/warehouse if lead time varies by location) | Safety stock |
| Stock lot | `product_id`, `warehouse_id`, `lot_code`, `quantity`, `expires_on` | FEFO |
| Existing stock | Current warehouse quantity, reconciled against lot quantities | Both recommendations |

The server supplies account ownership from the authenticated user; clients must not send an `owner_id`. `target_service_level` is a fraction between 0 and 1 (for example `0.95`). `expires_on` is required for FEFO-eligible lots; stock without a known expiry remains untracked for FEFO and must not be assigned a made-up date. Orders count as demand only when they represent completed consumption, not when reserved or later cancelled.

### Proposed APIs — not implemented

#### Record demand

`POST /inventory/demand-events` records realized consumption. Do not use this for inbound stock, reservations, or cancelled orders.

```json
{
  "product_id": 1,
  "warehouse_id": 1,
  "quantity": 2,
  "occurred_at": "2026-10-18T10:30:00Z"
}
```

All four fields are required; quantity must be positive. The response should identify the saved event:

```json
{
  "id": 501,
  "product_id": 1,
  "warehouse_id": 1,
  "quantity": 2,
  "occurred_at": "2026-10-18T10:30:00Z"
}
```

#### Set planning data

Extend the existing `POST /product` and `PUT /product/{id}` payloads with `lead_time_days` and `target_service_level`. They are required to calculate a dynamic recommendation; existing `reorder_level` remains the fallback.

```json
{
  "sku": "SKU-100",
  "name": "Tea",
  "category": "Grocery",
  "reorder_level": 20,
  "lead_time_days": 7,
  "target_service_level": 0.95
}
```

#### Get dynamic safety-stock recommendation

`GET /inventory/safety-stock?product_id=1&warehouse_id=1`

The response should show the calculation inputs and whether there is enough demand history. Example:

```json
{
  "product_id": 1,
  "warehouse_id": 1,
  "status": "READY",
  "average_daily_demand": 4.2,
  "lead_time_days": 7,
  "target_service_level": 0.95,
  "safety_stock": 12,
  "reorder_point": 42,
  "current_quantity": 35,
  "fallback_reorder_level": 20
}
```

When history is insufficient, return a status such as `INSUFFICIENT_DATA` and the existing fixed reorder level as the clearly labeled fallback; do not imply the fallback is a forecast.

#### Add and list expiry lots

`POST /inventory/lots` adds a received lot and updates the corresponding aggregate inventory and movement history atomically. `GET /inventory/lots` may filter by `product_id` and `warehouse_id`.

```json
{
  "product_id": 1,
  "warehouse_id": 1,
  "lot_code": "BATCH-2026-10-A",
  "quantity": 40,
  "expires_on": "2027-02-01"
}
```

All fields are required for an FEFO-eligible lot; quantity must be positive. The response should include the generated `lot_id`, remaining quantity, and the supplied lot fields. When introducing lot tracking, reconcile existing warehouse stock into lots with unknown expiry rather than inventing expiry dates.

#### Get FEFO transfer recommendations

`GET /inventory/fefo/recommendations`

No request body. Use lot expiry dates, available quantities, current destination stock, and the reorder point to identify feasible transfers. Existing order creation reserves stock by deducting it from inventory, so current lot quantity is the available quantity. Example response:

```json
[
  {
    "recommendation_id": "rec-901",
    "product_id": 1,
    "lot_id": 301,
    "lot_code": "BATCH-2026-10-A",
    "from_warehouse_id": 1,
    "to_warehouse_id": 2,
    "quantity": 15,
    "expires_on": "2027-02-01",
    "rationale": "This lot expires first; the destination is below its reorder point."
  }
]
```

An empty array means no eligible transfer was found. A recommendation does not move stock.

#### Preview and confirm a FEFO transfer

`POST /inventory/fefo-transfers/preview` rechecks eligibility and returns a preview without changing stock:

```json
{
  "recommendation_id": "rec-901",
  "lot_id": 301,
  "from_warehouse_id": 1,
  "to_warehouse_id": 2,
  "quantity": 15
}
```

The response should return a short-lived `preview_id` and the confirmed lot, quantity, expiry, source, and destination. Execute only after a user confirms that preview:

`POST /inventory/fefo-transfers/{preview_id}/execute`

```json
{
  "confirmed": true
}
```

Execution must recheck lot quantity, reservations, expiry eligibility, and destination need inside the stock-changing transaction. A stale or already executed preview must return a conflict rather than move stock twice.

#### Ask the natural-language assistant

`POST /assistant/messages` accepts a plain-language question or transfer request:

```json
{
  "message": "Which tea lots expire soon, and can you suggest a transfer to WH-WEST?"
}
```

The response may answer a question directly or return a proposed action requiring confirmation:

```json
{
  "answer": "Batch BATCH-2026-10-A is the earliest-expiring eligible lot. I can preview a transfer of 15 units to WH-WEST.",
  "action": {
    "action_id": "act-704",
    "type": "PREVIEW_FEFO_TRANSFER",
    "confirmation_required": true
  }
}
```

For a confirmed transfer, the client calls `POST /assistant/actions/{action_id}/confirm`:

```json
{
  "confirmed": true
}
```

The assistant must call the same authenticated domain services as the APIs above. It must not accept account IDs from the prompt, invent missing values, or execute a transfer without explicit confirmation and server-side validation. If information is ambiguous, it should ask the manager to clarify instead of creating an action.

## Explicitly out of scope for this roadmap

Avoid adding work unless it is required to deliver one of the three capabilities:

- A full frontend/dashboard rewrite or a new reporting suite.
- Automatic purchase orders or unapproved stock transfers.
- Supplier, shipping, payment, or advanced warehouse-planning workflows.
- Multiple AI providers, model training, or predictive features beyond the safety-stock calculation.
- Broad platform/operations work such as a new deployment platform or generalized observability project.

Keep existing configuration and testing practices unless a specific change is necessary for safe delivery. Do not expand the project into a production-platform rebuild as part of these features.

## Decisions to make

1. What is the source of actual demand: completed orders, manual consumption records, or imports?
2. What replenishment lead time and safety-stock policy should be used?
3. Which stock categories require expiry dates, and what minimum remaining shelf life makes a transfer eligible?
4. Which Spring AI provider should be used?

Changes beyond this list should be added only when they are essential to one of the three capabilities and explicitly approved.
