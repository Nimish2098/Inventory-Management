package com.miyuki.Inventory.Management.external;

public class StoreCheckException extends RuntimeException {

    public StoreCheckException(String message) {
        super(message);
    }

    public StoreCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
