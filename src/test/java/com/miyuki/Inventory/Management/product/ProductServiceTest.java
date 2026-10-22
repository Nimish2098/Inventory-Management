package com.miyuki.Inventory.Management.product;



import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.product.dto.CreateProductRequest;
import com.miyuki.Inventory.Management.product.dto.ProductResponse;
import com.miyuki.Inventory.Management.product.dto.UpdateProductRequest;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccount;
import com.miyuki.Inventory.Management.user.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductMapper productMapper;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private UserAccountRepository userRepository;

  @InjectMocks
  private ProductService productService;

  private UserAccount owner;

  @BeforeEach
  void setUp() {
    owner = mock(UserAccount.class);
  }

  private void mockCurrentUser() {
    when(currentUser.id()).thenReturn(1L);
  }

  @Test
  void createProduct_shouldSaveProductForCurrentUser() {
    mockCurrentUser();
    CreateProductRequest request =
        new CreateProductRequest(
            "SKU-001", "Laptop", "Electronics", 10L
        );

    Product product = Product.builder()
                             .id(100L)
                             .sku("SKU-001")
                             .name("Laptop")
                             .category("Electronics")
                             .reorderLevel(10L)
                             .build();

    when(productRepository.existsBySkuAndOwner_Id(
        "SKU-001", 1L
    )).thenReturn(false);

    when(productMapper.toEntity(request)).thenReturn(product);
    when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
    when(productRepository.save(any(Product.class)))
        .thenReturn(product);

    when(productMapper.toResponse(product))
        .thenReturn(new ProductResponse(
            100L, "SKU-001", "Laptop", "Electronics", 10L
        ));

    ProductResponse response = productService.createProduct(request);

    assertEquals(100L, response.id());
    assertEquals("SKU-001", response.sku());

    assertSame(owner, product.getOwner());

    verify(productRepository).save(product);
  }

  @Test
  void createProduct_shouldRejectDuplicateSku() {
    mockCurrentUser();
    CreateProductRequest request =
        new CreateProductRequest(
            "SKU-001", "Laptop", "Electronics", 10L
        );

    when(productRepository.existsBySkuAndOwner_Id(
        "SKU-001", 1L
    )).thenReturn(true);

    assertThrows(
        IllegalStateException.class,
        () -> productService.createProduct(request)
    );

    verify(productRepository, never()).save(any());
  }

  @Test
  void createProduct_shouldRejectInvalidReorderLevel() {
    CreateProductRequest request =
        new CreateProductRequest(
            "SKU-001", "Laptop", "Electronics", -1L
        );

    assertThrows(
        IllegalArgumentException.class,
        () -> productService.createProduct(request)
    );

    verify(productRepository, never()).save(any());
  }

  @Test
  void updateProduct_shouldUpdateExistingProduct() {
    mockCurrentUser();
    UpdateProductRequest request =
        new UpdateProductRequest(
            "SKU-002", "Updated Laptop",
            "Electronics", 20L
        );

    Product product = Product.builder()
                             .id(100L)
                             .sku("SKU-001")
                             .name("Laptop")
                             .reorderLevel(10L)
                             .build();

    when(productRepository.findByIdAndOwner_Id(100L, 1L))
        .thenReturn(Optional.of(product));

    when(productRepository.findBySkuAndOwner_Id("SKU-002", 1L))
        .thenReturn(Optional.empty());

    doAnswer(invocation -> {
      UpdateProductRequest update = invocation.getArgument(0);
      Product target = invocation.getArgument(1);

      target.setSku(update.sku());
      target.setName(update.name());
      target.setCategory(update.category());
      target.setReorderLevel(update.reorder_level());

      return null;
    }).when(productMapper).updateEntity(request, product);

    when(productRepository.save(product)).thenReturn(product);

    when(productMapper.toResponse(product))
        .thenReturn(new ProductResponse(
            100L, "SKU-002", "Updated Laptop",
            "Electronics", 20L
        ));

    ProductResponse response =
        productService.updateProduct(100L, request);

    assertEquals("SKU-002", response.sku());
    assertEquals("Updated Laptop", response.name());

    verify(productRepository).save(product);
  }

  @Test
  void updateProduct_shouldRejectDuplicateSku() {
    mockCurrentUser();
    UpdateProductRequest request =
        new UpdateProductRequest(
            "SKU-002", "Laptop", "Electronics", 10L
        );

    Product product = Product.builder()
                             .id(100L)
                             .sku("SKU-001")
                             .build();

    Product existing = Product.builder()
                              .id(200L)
                              .sku("SKU-002")
                              .build();

    when(productRepository.findByIdAndOwner_Id(100L, 1L))
        .thenReturn(Optional.of(product));

    when(productRepository.findBySkuAndOwner_Id("SKU-002", 1L))
        .thenReturn(Optional.of(existing));

    assertThrows(
        IllegalStateException.class,
        () -> productService.updateProduct(100L, request)
    );

    verify(productRepository, never()).save(any());
  }

  @Test
  void getProduct_shouldReturnProductForCurrentUser() {
    mockCurrentUser();
    Product product = Product.builder()
                             .id(100L)
                             .sku("SKU-001")
                             .name("Laptop")
                             .build();

    when(productRepository.findByIdAndOwner_Id(100L, 1L))
        .thenReturn(Optional.of(product));

    when(productMapper.toResponse(product))
        .thenReturn(new ProductResponse(
            100L, "SKU-001", "Laptop", "Electronics", 10L
        ));

    ProductResponse response = productService.getProduct(100L);

    assertEquals(100L, response.id());
    assertEquals("SKU-001", response.sku());
  }

  @Test
  void getProduct_shouldThrowWhenProductDoesNotExist() {
    mockCurrentUser();

    when(productRepository.findByIdAndOwner_Id(100L, 1L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> productService.getProduct(100L)
    );

    verify(productRepository)
        .findByIdAndOwner_Id(100L, 1L);
  }

  @Test
  void getProducts_shouldReturnCurrentUsersProducts() {
    mockCurrentUser();
    Product product = Product.builder()
                             .id(100L)
                             .sku("SKU-001")
                             .name("Laptop")
                             .build();

    when(productRepository.findAllByOwner_Id(1L))
        .thenReturn(List.of(product));

    when(productMapper.toResponse(product))
        .thenReturn(new ProductResponse(
            100L, "SKU-001", "Laptop", "Electronics", 10L
        ));

    List<ProductResponse> response = productService.getProducts();

    assertEquals(1, response.size());
    assertEquals("SKU-001", response.get(0).sku());

    verify(productRepository).findAllByOwner_Id(1L);
  }

  @Test
  void deleteProduct_shouldDeleteOwnedProduct() {
    mockCurrentUser();
    Product product = Product.builder()
                             .id(100L)
                             .sku("SKU-001")
                             .build();

    when(productRepository.findByIdAndOwner_Id(100L, 1L))
        .thenReturn(Optional.of(product));

    productService.deleteProduct(100L);

    verify(productRepository).delete(product);
  }

  @Test
  void deleteProduct_shouldNotDeleteAnotherUsersProduct() {
    mockCurrentUser();

    when(productRepository.findByIdAndOwner_Id(100L, 1L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> productService.deleteProduct(100L)
    );

    verify(productRepository)
        .findByIdAndOwner_Id(100L, 1L);

    verify(productRepository, never())
        .delete(any(Product.class));
  }
}
