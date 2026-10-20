package com.miyuki.Inventory.Management.external;

import java.net.URI;

public interface StoreChecker {

    boolean supports(URI productUri);

    StoreObservation check(ProductWatch watch);
}
