package com.miyuki.Inventory.Management.product;


import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.product.dto.ProductImportResponse;
import com.miyuki.Inventory.Management.product.dto.ProductImportResponse.RowError;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccountRepository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductCsvImportService {

  private static final int BATCH_SIZE = 500;

  private static final Set<String> REQUIRED_HEADERS = Set.of(
      "sku",
      "name",
      "category",
      "reorder_level"
  );

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;
  private final CurrentUser currentUser;
  private final UserAccountRepository userRepository;

  public ProductCsvImportService(
      ProductRepository productRepository,
      ProductMapper productMapper,
      CurrentUser currentUser,
      UserAccountRepository userRepository
  ) {
    this.productRepository = productRepository;
    this.productMapper = productMapper;
    this.currentUser = currentUser;
    this.userRepository = userRepository;
  }

  public ProductImportResponse importProducts(MultipartFile file) {

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException(
          "Please upload a non-empty CSV file"
      );
    }

    if (file.getSize() > 10 * 1024 * 1024) {
      throw new IllegalArgumentException(
          "CSV file must not exceed 10 MB"
      );
    }

    Long ownerId = currentUser.id();

    var owner = userRepository.findById(ownerId)
                              .orElseThrow(() ->
                                               new ResourceNotFoundException(
                                                   "User account not found"
                                               )
                              );

    List<RowError> errors = new ArrayList<>();
    Set<String> seenSkus = new HashSet<>();

    int totalRows = 0;
    int imported = 0;
    int batchStartRow = 2;

    List<CSVRecord> batch = new ArrayList<>(BATCH_SIZE);

    try (
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(
                file.getInputStream(),
                StandardCharsets.UTF_8
            )
        );
        PushbackReader reader = new PushbackReader(
            bufferedReader, 1
        )
    ) {
      // Support UTF-8 files containing a BOM.
      int firstChar = reader.read();

      if (firstChar != 0xFEFF && firstChar != -1) {
        reader.unread(firstChar);
      }

      CSVFormat format = CSVFormat.DEFAULT.builder()
                                          .setHeader()
                                          .setSkipHeaderRecord(true)
                                          .setIgnoreEmptyLines(true)
                                          .setTrim(true)
                                          .get();

      try (CSVParser parser = format.parse(reader)) {

        Set<String> headers = new HashSet<>(
            parser.getHeaderMap().keySet()
        );

        if (!headers.containsAll(REQUIRED_HEADERS)) {
          throw new IllegalArgumentException(
              "CSV must contain these columns: "
              + REQUIRED_HEADERS
          );
        }

        for (CSVRecord record : parser) {
          totalRows++;

          if (record.size() != headers.size()) {
            errors.add(new RowError(
                (int) record.getRecordNumber() + 1,
                "",
                "Incorrect number of columns"
            ));
            continue;
          }

          batch.add(record);

          if (batch.size() == BATCH_SIZE) {
            imported += processBatch(
                batch,
                ownerId,
                owner,
                seenSkus,
                errors
            );

            batch.clear();
          }
        }

        if (!batch.isEmpty()) {
          imported += processBatch(
              batch,
              ownerId,
              owner,
              seenSkus,
              errors
          );
        }
      }

    } catch (IOException ex) {
      throw new IllegalArgumentException(
          "Unable to read CSV file",
          ex
      );
    }

    return new ProductImportResponse(
        totalRows,
        imported,
        errors.size(),
        List.copyOf(errors)
    );
  }

  private int processBatch(
      List<CSVRecord> records,
      Long ownerId,
      com.miyuki.Inventory.Management.user.UserAccount owner,
      Set<String> seenSkus,
      List<RowError> errors
  ) {
    List<ParsedProduct> validProducts = new ArrayList<>();

    // Validate each row before querying the database.
    for (CSVRecord record : records) {
      int rowNumber = (int) record.getRecordNumber() + 1;
      String sku = "";

      try {
        sku = record.get("sku").trim();
        String name = record.get("name").trim();
        String category = record.get("category").trim();
        String reorderValue = record.get("reorder_level").trim();

        if (sku.isBlank() || name.isBlank()) {
          throw new IllegalArgumentException(
              "SKU and name are required"
          );
        }

        if (reorderValue.isBlank()) {
          throw new IllegalArgumentException(
              "Reorder level is required"
          );
        }

        long reorderLevel;

        try {
          reorderLevel = Long.parseLong(reorderValue);
        } catch (NumberFormatException ex) {
          throw new IllegalArgumentException(
              "Reorder level must be a valid integer"
          );
        }

        if (reorderLevel < 0) {
          throw new IllegalArgumentException(
              "Reorder level cannot be negative"
          );
        }

        if (!seenSkus.add(sku)) {
          throw new IllegalArgumentException(
              "Duplicate SKU in CSV: " + sku
          );
        }

        validProducts.add(new ParsedProduct(
            record,
            rowNumber,
            sku,
            name,
            category,
            reorderLevel
        ));

      } catch (IllegalArgumentException ex) {
        errors.add(new RowError(
            rowNumber, sku, ex.getMessage()
        ));
      }
    }

    if (validProducts.isEmpty()) {
      return 0;
    }

    // Check existing SKUs for this user with one query per batch.
    List<String> skus = validProducts.stream()
                                     .map(ParsedProduct::sku)
                                     .toList();

    Set<String> existingSkus = new HashSet<>();

    productRepository
        .findAllByOwner_IdAndSkuIn(ownerId, skus)
        .forEach(product -> existingSkus.add(product.getSku()));

    List<Product> productsToSave = new ArrayList<>();

    for (ParsedProduct parsed : validProducts) {
      if (existingSkus.contains(parsed.sku())) {
        errors.add(new RowError(
            parsed.rowNumber(),
            parsed.sku(),
            "SKU already exists for this user"
        ));
        continue;
      }

      Product product = productMapper.toEntity(
          new com.miyuki.Inventory.Management.product.dto
              .CreateProductRequest(
              parsed.sku(),
              parsed.name(),
              parsed.category(),
              parsed.reorderLevel()
          )
      );

      product.setOwner(owner);
      productsToSave.add(product);
    }

    if (productsToSave.isEmpty()) {
      return 0;
    }

    try {
      productRepository.saveAll(productsToSave);
      return productsToSave.size();

    } catch (DataIntegrityViolationException ex) {
      // The database may reject a concurrent duplicate SKU.
      // The failed batch is rolled back by the repository transaction.
      for (Product product : productsToSave) {
        errors.add(new RowError(
            product == null ? 0 :
            validProducts.stream()
                         .filter(p -> p.sku().equals(
                             product.getSku()))
                         .mapToInt(ParsedProduct::rowNumber)
                         .findFirst()
                         .orElse(0),
            product.getSku(),
            "Database rejected this product; check SKU uniqueness"
        ));
      }

      return 0;
    }
  }

  private record ParsedProduct(
      CSVRecord record,
      int rowNumber,
      String sku,
      String name,
      String category,
      Long reorderLevel
  ) {}
}
