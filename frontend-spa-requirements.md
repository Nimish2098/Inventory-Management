# Simple SPA Frontend Requirements for the Inventory Management App

This document outlines the minimum frontend features needed for a simple single-page application that works with the current backend.

## 1. Goal of the frontend

The frontend should let an inventory manager:
- manage products
- manage warehouses
- view stock levels
- adjust stock
- transfer stock between warehouses
- view low-stock items
- manage customers and orders
- quickly understand inventory status from one dashboard

It should stay simple, fast, and focused on operational work.

---

## 2. Recommended frontend stack

For a simple SPA, use:
- React
- Vite
- TypeScript
- React Router
- Axios or Fetch
- CSS module or simple Tailwind CSS
- Optional state library: React Context or Zustand

This is enough without adding unnecessary complexity.

---

## 3. Main pages/screens

### A. Dashboard

Purpose:
- show top-level inventory summary
- show low-stock warnings
- show recent movement or alerts

Sections:
- total products
- total warehouses
- total inventory units
- low-stock items count
- quick action buttons:
  - Add Product
  - Add Warehouse
  - Adjust Stock
  - Transfer Stock

---

### B. Products page

Purpose:
- create, view, update, and delete products

Features:
- product table with columns:
  - id
  - sku
  - name
  - category
  - reorder level
- search by name or sku
- add product form
- edit product form
- delete confirmation

API used:
- GET /product
- GET /product/{id}
- POST /product
- PUT /product/{id}
- DELETE /product/{id}

---

### C. Warehouses page

Purpose:
- manage warehouses

Features:
- warehouse table with columns:
  - id
  - code
  - name
  - capacity
  - active status
- add warehouse form
- edit warehouse form
- soft toggle or active/inactive status if needed
- delete warehouse if safe

API used:
- GET /warehouse
- GET /warehouse/{id}
- POST /warehouse
- PUT /warehouse/{id}
- DELETE /warehouse/{id}

---

### D. Inventory page

Purpose:
- view stock by product and warehouse

Features:
- filter by warehouse
- table with columns:
  - product name
  - sku
  - warehouse name
  - quantity
  - bin (if available)
- ability to open stock details
- button to adjust stock
- button to transfer stock

API used:
- GET /inventory
- GET /inventory?warehouse_id={id}
- GET /inventory/{productId}/{warehouseId}
- GET /inventory/low-stock

---

### E. Stock adjustment page or modal

Purpose:
- add inbound/outbound/adjustment movement

Inputs:
- product
- warehouse
- quantity
- movement type:
  - INBOUND
  - OUTBOUND
  - ADJUSTMENT

Validation:
- quantity must be non-zero
- outbound cannot go below zero
- movement type must match quantity sign

API used:
- POST /inventory/adjustments

---

### F. Transfer stock page or modal

Purpose:
- move stock between warehouses

Inputs:
- product
- from warehouse
- to warehouse
- quantity

Validation:
- source cannot equal destination
- quantity must be positive
- source warehouse must have enough stock

API used:
- POST /inventory/transfers

---

### G. Stock movement history page

Purpose:
- view movement timeline for a product in a warehouse

Features:
- product selector
- warehouse selector
- table with columns:
  - timestamp
  - movement type
  - quantity change
  - current location or item summary

API used:
- GET /inventory/{productId}/{warehouseId}/movements

---

### H. Customers page

Purpose:
- manage customer records for orders

Features:
- list customers
- create customer
- view customer details

API used:
- GET /customer
- GET /customer/{id}
- POST /customer

---

### I. Orders page

Purpose:
- place and manage customer orders

Features:
- create order form
- customer selection
- add multiple order items
- product + warehouse + quantity per line item
- order summary before submit
- list orders
- cancel order
- show pending orders

API used:
- GET /order
- GET /order/{id}
- GET /order/pending
- POST /order
- PUT /order/{id}/cancel

---

## 4. Shared frontend components

These components are enough for a clean SPA without overbuilding:

- App shell / layout
- Top navigation bar
- Sidebar menu
- Page header with title and actions
- Data table component
- Search/filter bar
- Form field components
- Select dropdowns
- Modal dialog
- Confirmation dialog
- Toast notification
- Empty state component
- Loading spinner / skeleton
- Error banner

---

## 5. Core frontend state

The app needs a simple store or context for:
- products
- warehouses
- inventory
- low-stock data
- customers
- orders

Simple pattern:
- local component state for forms
- shared API state in a context or store
- cached fetches for lists

No advanced state management is required for a simple SPA.

---

## 6. Data models needed in the frontend

### Product
```ts
{
  id: number,
  sku: string,
  name: string,
  category: string,
  reorder_level: number
}
```

### Warehouse
```ts
{
  id: number,
  code: string,
  name: string,
  capacity: number,
  active: boolean
}
```

### Inventory item
```ts
{
  id: number,
  product_id: number,
  warehouse_id: number,
  quantity: number,
  product_name?: string,
  warehouse_name?: string
}
```

### Customer
```ts
{
  id: number,
  name: string,
  email: string
}
```

### Order
```ts
{
  id: number,
  customer_id: number,
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'REJECTED',
  createdAt: string,
  items: [
    {
      id: number,
      product_id: number,
      warehouse_id: number,
      quantity: number
    }
  ]
}
```

---

## 7. API service layer needed

Create a single API service module, for example:
- `api/products.ts`
- `api/warehouses.ts`
- `api/inventory.ts`
- `api/customers.ts`
- `api/orders.ts`

Each module should contain:
- getAll
- getById
- create
- update
- delete
- custom actions like adjustStock, transferStock

This keeps the frontend clean and avoids repeating fetch logic across pages.

---

## 8. Minimal routing

Use simple routes:
- `/`
- `/products`
- `/warehouses`
- `/inventory`
- `/orders`
- `/customers`
- `/low-stock`

This is enough for a small operational dashboard app.

---

## 9. UX requirements

Keep the UI simple and functional:
- table-first layout
- forms clearly separated by page or modal
- validation messages inline
- success and error alerts
- no heavy animations
- no complex charts unless required later

---

## 10. Recommended first version scope

For a first SPA version, include only these features:
1. Dashboard
2. Products
3. Warehouses
4. Inventory overview
5. Low-stock view
6. Stock adjustment
7. Stock transfer
8. Customer and order management

Do not add advanced features in version 1 like:
- multi-user auth
- reporting dashboards
- advanced charts
- audit trails
- role management
- payment flows

---

## 11. Summary

A simple SPA for this app should focus on operational inventory work, not a full business suite. The most important frontend screens are:
- Dashboard
- Products
- Warehouses
- Inventory
- Stock adjustments
- Transfers
- Orders
- Customers

With a simple API layer, basic forms, and table-based views, the frontend will stay easy to build and maintain while covering the real business use cases of this backend.
