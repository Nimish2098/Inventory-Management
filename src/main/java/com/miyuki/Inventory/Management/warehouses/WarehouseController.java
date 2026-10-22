package com.miyuki.Inventory.Management.warehouses;


import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.miyuki.Inventory.Management.warehouses.dto.CreateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.UpdateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.WarehouseResponse;
import com.miyuki.Inventory.Management.user.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;



@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final CurrentUser currentUser;
    private final UserAccountRepository userRepository;

    private final WarehouseService warehouseService;

    public WarehouseController(
        WarehouseService warehouseService,
        CurrentUser currentUser,
        UserAccountRepository userRepository) {
        this.warehouseService = warehouseService;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse createWarehouse(@RequestBody CreateWarehouseRequest request){
        return warehouseService.createWarehouse(request);
    }

    @GetMapping
    public List<WarehouseResponse> getAllActiveWarehouse(){
        return warehouseService.getAllActiveWarehouses();
    }

    @PutMapping("/{id}")
    public WarehouseResponse updateWarehouse(@PathVariable Long id,@RequestBody UpdateWarehouseRequest request){
        return warehouseService.updateWarehouse(id,request);
    }


    @GetMapping("/{id}")
    public WarehouseResponse getWarehouseDetails(@PathVariable Long id){
        return warehouseService.getWarehouseById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
    }


    @PostMapping(
        value = "/import",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<WarehouseResponse>> importCsv(
        @RequestParam("file") MultipartFile file) {

        UserAccount owner = userRepository.findById(currentUser.id())
                                          .orElseThrow(() ->
                                                           new ResourceNotFoundException("User account not found"));

        List<WarehouseResponse> warehouses =
            warehouseService.importCsv(file, owner);

        return ResponseEntity.ok(warehouses);
    }
}