package com.miyuki.Inventory.Management.warehouses;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.warehouses.dto.CreateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.UpdateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.WarehouseResponse;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import org.springframework.stereotype.Service;

import com.miyuki.Inventory.Management.user.UserAccount;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.List;

@Service
public class WarehouseService {


    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final CurrentUser currentUser;
    private final UserAccountRepository userRepository;

    public WarehouseService(WarehouseRepository repository, WarehouseMapper warehouseMapper,
                            CurrentUser currentUser, UserAccountRepository userRepository){
        this.warehouseRepository =repository;
        this.warehouseMapper = warehouseMapper;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }

    public WarehouseResponse createWarehouse(CreateWarehouseRequest request){
        validate(request.code(), request.name(), request.capacity());
        if(warehouseRepository.existsByCodeAndOwner_Id(request.code(), currentUser.id())){
            throw new IllegalStateException("Warehouse code already exists");
        }
        Warehouse warehouse = warehouseMapper.toEntity(request);
        warehouse.setActive(true);
        warehouse.setOwner(userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found")));
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public WarehouseResponse getWarehouseById(Long warehouseId){
        return warehouseMapper.toResponse(getEntity(warehouseId));
    }

    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request){
        validate(request.code(), request.name(), request.capacity());
        Warehouse warehouse = getEntity(id);
        warehouseRepository.findByCodeAndOwner_Id(request.code(), currentUser.id())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalStateException("Warehouse code already exists"); });
        warehouseMapper.updateEntity(request, warehouse);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public List<WarehouseResponse> getAllActiveWarehouses(){
        return warehouseRepository.findAllByOwner_Id(currentUser.id()).stream()
                .map(warehouseMapper::toResponse).toList();
    }

    public void deleteWarehouse(Long id) {
        warehouseRepository.delete(getEntity(id));
    }

    private Warehouse getEntity(Long id) {
        return warehouseRepository.findByIdAndOwner_Id(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse " + id + " not found"));
    }

    private void validate(String code, String name, Integer capacity) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Warehouse code and name are required");
        }
        if (capacity == null || capacity < 0) {
            throw new IllegalArgumentException("Capacity must be zero or greater");
        }
    }


    @Transactional
    public List<WarehouseResponse> importCsv(
        MultipartFile file,
        UserAccount owner
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        if (owner == null || owner.getId() == null) {
            throw new IllegalArgumentException("Authenticated owner is required");
        }

        try (Reader reader = new InputStreamReader(
            file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                                                 .setHeader()
                                                 .setSkipHeaderRecord(true)
                                                 .setIgnoreEmptyLines(true)
                                                 .setTrim(true)
                                                 .get()
                                                 .parse(reader)) {

            Set<String> requiredHeaders =
                Set.of("code", "name", "capacity");

            Set<String> actualHeaders = parser.getHeaderMap()
                                              .keySet()
                                              .stream()
                                              .map(h -> h.trim().toLowerCase(Locale.ROOT))
                                              .collect(Collectors.toSet());

            if (!actualHeaders.containsAll(requiredHeaders)) {
                throw new IllegalArgumentException(
                    "CSV must contain headers: code,name,capacity"
                );
            }

            List<CreateWarehouseRequest> requests = new ArrayList<>();
            Set<String> uploadedCodes = new HashSet<>();

            for (CSVRecord row : parser) {
                String code = row.get("code").trim()
                                 .toUpperCase(Locale.ROOT);
                String name = row.get("name").trim();
                String capacityText = row.get("capacity").trim();

                if (code.isBlank() || name.isBlank()) {
                    throw new IllegalArgumentException(
                        "Missing code or name at CSV row " + row.getRecordNumber()
                    );
                }

                int capacity;
                try {
                    capacity = Integer.parseInt(capacityText);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                        "Invalid capacity at CSV row " + row.getRecordNumber()
                    );
                }

                if (capacity <= 0) {
                    throw new IllegalArgumentException(
                        "Capacity must be positive at CSV row "
                        + row.getRecordNumber()
                    );
                }

                if (!uploadedCodes.add(code)) {
                    throw new IllegalArgumentException(
                        "Duplicate code in CSV: " + code
                    );
                }

                requests.add(new CreateWarehouseRequest(code, name, capacity));
            }

            if (requests.isEmpty()) {
                throw new IllegalArgumentException(
                    "CSV contains no warehouse records"
                );
            }

            Set<String> existingCodes = warehouseRepository
                .findAllByOwner_Id(owner.getId())
                .stream()
                .map(Warehouse::getCode)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

            for (String code : uploadedCodes) {
                if (existingCodes.contains(code)) {
                    throw new IllegalArgumentException(
                        "Warehouse code already exists: " + code
                    );
                }
            }

            List<Warehouse> warehouses = requests.stream()
                                                 .map(warehouseMapper::toEntity)
                                                 .peek(warehouse -> warehouse.setOwner(owner))
                                                 .toList();

            return warehouseRepository.saveAll(warehouses)
                                      .stream()
                                      .map(warehouseMapper::toResponse)
                                      .toList();

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                "Could not read warehouse CSV: " + ex.getMessage(), ex
            );
        }
    }
}
