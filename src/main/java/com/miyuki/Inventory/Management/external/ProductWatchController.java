package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.external.dto.CreateProductWatchRequest;
import com.miyuki.Inventory.Management.external.dto.ProductObservationResponse;
import com.miyuki.Inventory.Management.external.dto.ProductWatchResponse;
import com.miyuki.Inventory.Management.external.dto.UpdateProductWatchRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watches")
public class ProductWatchController {

    private final ProductWatchService watchService;

    public ProductWatchController(ProductWatchService watchService) {
        this.watchService = watchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductWatchResponse create(@Valid @RequestBody CreateProductWatchRequest request) {
        return watchService.create(request);
    }

    @GetMapping
    public List<ProductWatchResponse> list() {
        return watchService.list();
    }

    @GetMapping("/{id}")
    public ProductWatchResponse get(@PathVariable Long id) {
        return watchService.get(id);
    }

    @PatchMapping("/{id}")
    public ProductWatchResponse update(
            @PathVariable Long id,
            @RequestBody UpdateProductWatchRequest request) {
        return watchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        watchService.delete(id);
    }

    @GetMapping("/{id}/history")
    public List<ProductObservationResponse> history(@PathVariable Long id) {
        return watchService.history(id);
    }
}
