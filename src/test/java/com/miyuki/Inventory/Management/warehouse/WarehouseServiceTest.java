package com.miyuki.Inventory.Management.warehouse;

import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccount;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import com.miyuki.Inventory.Management.warehouses.dto.WarehouseResponse;
import com.miyuki.Inventory.Management.warehouses.WarehouseService;
import com.miyuki.Inventory.Management.warehouses.WarehouseMapper;
import com.miyuki.Inventory.Management.warehouses.Warehouse;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

  @Mock
  private WarehouseRepository warehouseRepository;

  @Mock
  private WarehouseMapper warehouseMapper;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private UserAccountRepository userRepository;

  private WarehouseService warehouseService;
  private UserAccount owner;

  @BeforeEach
  void setUp() {
    warehouseService = new WarehouseService(
        warehouseRepository,
        warehouseMapper,
        currentUser,
        userRepository
    );

    owner = mock(UserAccount.class);
  }

  @Test
  void importCsv_validCsv_savesAndReturnsWarehouses() {
    MockMultipartFile file = csvFile("""
                code,name,capacity
                WH-00001,Mumbai Warehouse,25000
                WH-00002,Bengaluru Warehouse,18000
                """);

    Warehouse first = warehouse(
        1L, "WH-00001", "Mumbai Warehouse", 25000, owner);
    Warehouse second = warehouse(
        2L, "WH-00002", "Bengaluru Warehouse", 18000, owner);

    when(owner.getId()).thenReturn(1L);
    when(warehouseRepository.findAllByOwner_Id(1L)).thenReturn(List.of());

    when(warehouseMapper.toEntity(any()))
        .thenAnswer(invocation -> {
          var request = invocation.getArgument(
              0,
              com.miyuki.Inventory.Management.warehouses.dto.CreateWarehouseRequest.class
          );

          Warehouse warehouse = new Warehouse();
          warehouse.setCode(request.code());
          warehouse.setName(request.name());
          warehouse.setCapacity(request.capacity());
          warehouse.setActive(true);
          return warehouse;
        });

    when(warehouseRepository.saveAll(any()))
        .thenReturn(List.of(first, second));

    when(warehouseMapper.toResponse(first))
        .thenReturn(new WarehouseResponse(
            1L, "WH-00001", "Mumbai Warehouse", 25000, true));

    when(warehouseMapper.toResponse(second))
        .thenReturn(new WarehouseResponse(
            2L, "WH-00002", "Bengaluru Warehouse", 18000, true));

    List<WarehouseResponse> result =
        warehouseService.importCsv(file, owner);

    assertEquals(2, result.size());
    assertEquals("WH-00001", result.get(0).code());
    assertEquals("Mumbai Warehouse", result.get(0).name());
    assertEquals(25000, result.get(0).capacity());
    assertTrue(result.get(0).isActive());

    assertEquals("WH-00002", result.get(1).code());

    verify(warehouseRepository).findAllByOwner_Id(1L);
    verify(warehouseRepository).saveAll(any());
    verify(warehouseMapper, times(2)).toEntity(any());
  }

  @Test
  void importCsv_emptyFile_throwsException() {
    MockMultipartFile file = new MockMultipartFile(
        "file", "warehouses.csv", "text/csv", new byte[0]);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, owner)
    );

    verifyNoInteractions(warehouseRepository);
  }

  @Test
  void importCsv_missingHeaders_throwsException() {
    MockMultipartFile file = csvFile("""
                code,name
                WH-00001,Mumbai Warehouse
                """);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, owner)
    );

    verify(warehouseRepository, never()).saveAll(any());
  }

  @Test
  void importCsv_invalidCapacity_throwsException() {
    MockMultipartFile file = csvFile("""
                code,name,capacity
                WH-00001,Mumbai Warehouse,invalid
                """);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, owner)
    );

    verify(warehouseRepository, never()).saveAll(any());
  }

  @Test
  void importCsv_zeroCapacity_throwsException() {
    MockMultipartFile file = csvFile("""
                code,name,capacity
                WH-00001,Mumbai Warehouse,0
                """);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, owner)
    );

    verify(warehouseRepository, never()).saveAll(any());
  }

  @Test
  void importCsv_duplicateCodesInFile_throwsException() {
    MockMultipartFile file = csvFile("""
                code,name,capacity
                WH-00001,Mumbai Warehouse,25000
                WH-00001,Another Warehouse,18000
                """);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, owner)
    );

    verify(warehouseRepository, never()).saveAll(any());
  }

  @Test
  void importCsv_existingCodeForOwner_throwsException() {
    MockMultipartFile file = csvFile("""
        code,name,capacity
        WH-00001,Mumbai Warehouse,25000
        """);

    when(owner.getId()).thenReturn(1L);

    Warehouse existing = new Warehouse();
    existing.setCode("WH-00001");

    when(warehouseRepository.findAllByOwner_Id(1L))
        .thenReturn(List.of(existing));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, owner)
    );

    assertTrue(exception.getMessage().contains("Warehouse code already exists"));
  }

  @Test
  void importCsv_nullOwner_throwsException() {
    MockMultipartFile file = csvFile("""
                code,name,capacity
                WH-00001,Mumbai Warehouse,25000
                """);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, null)
    );

    verifyNoInteractions(warehouseRepository);
  }

  @Test
  void importCsv_ownerWithoutId_throwsException() {
    MockMultipartFile file = csvFile("""
        code,name,capacity
        WH-00001,Mumbai Warehouse,25000
        """);

    UserAccount ownerWithoutId = mock(UserAccount.class);
    when(ownerWithoutId.getId()).thenReturn(null);

    assertThrows(
        IllegalArgumentException.class,
        () -> warehouseService.importCsv(file, ownerWithoutId)
    );

    verifyNoInteractions(warehouseRepository, warehouseMapper);
  }

  private MockMultipartFile csvFile(String content) {
    return new MockMultipartFile(
        "file",
        "warehouses.csv",
        "text/csv",
        content.getBytes(StandardCharsets.UTF_8)
    );
  }

  private Warehouse warehouse(
      Long id,
      String code,
      String name,
      Integer capacity,
      UserAccount owner
  ) {
    Warehouse warehouse = new Warehouse();
    warehouse.setId(id);
    warehouse.setCode(code);
    warehouse.setName(name);
    warehouse.setCapacity(capacity);
    warehouse.setActive(true);
    warehouse.setOwner(owner);
    return warehouse;
  }
}
