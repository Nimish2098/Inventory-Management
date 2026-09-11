package com.miyuki.Inventory.Management;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestAPI {

    @GetMapping
    public String hello(){
        return "Hello World";
    }
}
