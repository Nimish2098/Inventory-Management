package com.miyuki.Inventory.Management;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloAPI {

    @GetMapping
    public String hello(){
        return "Hello World";
    }
}
