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

   9. Failure cases
       if consumed inventory > available  inventory : failed
       if available inventory : 1
        a buys and b buys then a accepted but b failed
      if available inventory : 5
        a buys 3 and b buys 4 then only a buys b failed
      if same customer buys twice
   

10. Concurrency scenarios