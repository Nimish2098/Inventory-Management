package com.miyuki.Inventory.Management.Service;

import com.miyuki.Inventory.Management.Model.Customer;
import com.miyuki.Inventory.Management.Model.Order;
import com.miyuki.Inventory.Management.Model.Product;
import com.miyuki.Inventory.Management.Repository.CustomerRepository;
import com.miyuki.Inventory.Management.Repository.OrderRepository;
import com.miyuki.Inventory.Management.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long customerId, List<OrderItemRequest> items) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer id is required.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required.");
        }

        customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));

        List<Product> cart = new ArrayList<>();
        Map<Long, Integer> itemQuantities = new HashMap<>();
        double totalAmount = 0.0;

        for (OrderItemRequest item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each item must include a valid productId and quantity.");
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + item.getProductId()));

            if (product.getProduct_stock() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getProduct_name());
            }

            product.setProduct_stock(product.getProduct_stock() - item.getQuantity());
            productRepository.save(product);

            if (!cart.contains(product)) {
                cart.add(product);
            }
            itemQuantities.merge(product.getProduct_id(), item.getQuantity(), Integer::sum);
            totalAmount += (product.getPrice() == null ? 0.0 : product.getPrice()) * item.getQuantity();
        }

        Order order = new Order();
        order.setCustomer_id(customerId);
        order.setCart(cart);
        order.setItemQuantities(itemQuantities);
        order.setOrder_status("CONFIRMED");
        order.setTotalAmount(totalAmount);

        return orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order buyOrder(Long customer_id, Long product_id, Integer quantity) {
        if (customer_id == null || product_id == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Customer id, product id and quantity are required.");
        }

        customerRepository.findById(customer_id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customer_id));

        Product product = productRepository.findById(product_id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + product_id));

        if (product.getProduct_stock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getProduct_name());
        }

        product.setProduct_stock(product.getProduct_stock() - quantity);
        productRepository.save(product);

        Order order = new Order();
        order.setCustomer_id(customer_id);
        order.setCart(Collections.singletonList(product));
        order.setItemQuantities(Map.of(product_id, quantity));
        order.setOrder_status("CONFIRMED");
        order.setTotalAmount((product.getPrice() == null ? 0.0 : product.getPrice()) * quantity);

        return orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order buyOrder(Long customer_id, Long product_id) {
        return buyOrder(customer_id, product_id, 1);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (!"CONFIRMED".equals(order.getOrder_status())) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getOrder_status());
        }

        for (Map.Entry<Long, Integer> item : order.getItemQuantities().entrySet()) {
            Product product = productRepository.findById(item.getKey())
                    .orElseThrow(() -> new IllegalStateException("Product not found with id: " + item.getKey()));
            product.setProduct_stock(product.getProduct_stock() + item.getValue());
            productRepository.save(product);
        }

        order.setOrder_status("CANCELLED");
        return orderRepository.save(order);
    }

    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}