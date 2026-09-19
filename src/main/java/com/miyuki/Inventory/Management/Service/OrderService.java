package com.miyuki.Inventory.Management.Service;

import com.miyuki.Inventory.Management.Model.Order;
import com.miyuki.Inventory.Management.Repository.OrderRepository;
import com.miyuki.Inventory.Management.Repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService{

        private final OrderRepository orderRepository;
        private final ProductRepository productRepository;

        public OrderService(OrderRepository orderRepository,ProductRepository productRepository){
            this.orderRepository = orderRepository;
            this.productRepository = productRepository;
        }


        public Order buyOrder(Long customer_id,Long product_id){

        }

}