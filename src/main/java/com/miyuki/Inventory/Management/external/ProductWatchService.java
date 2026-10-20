package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.external.dto.CreateProductWatchRequest;
import com.miyuki.Inventory.Management.external.dto.ProductObservationResponse;
import com.miyuki.Inventory.Management.external.dto.ProductWatchResponse;
import com.miyuki.Inventory.Management.external.dto.UpdateProductWatchRequest;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductWatchService {

    private final ProductWatchRepository watchRepository;
    private final ProductObservationRepository observationRepository;
    private final ProductNotificationEventRepository notificationEventRepository;
    private final UserAccountRepository userRepository;
    private final CurrentUser currentUser;
    private final ProductUrlValidator urlValidator;

    public ProductWatchService(
            ProductWatchRepository watchRepository,
            ProductObservationRepository observationRepository,
            ProductNotificationEventRepository notificationEventRepository,
            UserAccountRepository userRepository,
            CurrentUser currentUser,
            ProductUrlValidator urlValidator) {
        this.watchRepository = watchRepository;
        this.observationRepository = observationRepository;
        this.notificationEventRepository = notificationEventRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.urlValidator = urlValidator;
    }

    @Transactional
    public ProductWatchResponse create(CreateProductWatchRequest request) {
        if (request == null || request.productName() == null
                || request.productName().isBlank() || request.productName().length() > 200) {
            throw new IllegalArgumentException("Product name is required and must be at most 200 characters");
        }
        validateTargetPrice(request.targetPrice());
        urlValidator.validate(request.productUrl());

        ProductWatch watch = new ProductWatch();
        watch.setOwner(userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found")));
        watch.setProductName(request.productName().trim());
        watch.setProductUrl(request.productUrl());
        watch.setTargetPrice(request.targetPrice());
        watch.setEnabled(true);
        return toResponse(watchRepository.save(watch));
    }

    @Transactional(readOnly = true)
    public List<ProductWatchResponse> list() {
        return watchRepository.findAllByOwner_IdOrderByCreatedAtDesc(currentUser.id())
                .stream().map(ProductWatchService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductWatchResponse get(Long id) {
        return toResponse(findOwnedWatch(id));
    }

    @Transactional
    public ProductWatchResponse update(Long id, UpdateProductWatchRequest request) {
        if (request == null || (request.targetPrice() == null && request.enabled() == null
                && !request.clearTargetPrice())) {
            throw new IllegalArgumentException("At least one watch setting must be provided");
        }
        if (request.clearTargetPrice() && request.targetPrice() != null) {
            throw new IllegalArgumentException("Provide targetPrice or clearTargetPrice, not both");
        }
        if (request.targetPrice() != null) {
            validateTargetPrice(request.targetPrice());
        }

        ProductWatch watch = findOwnedWatch(id);
        if (request.clearTargetPrice()) {
            watch.setTargetPrice(null);
        } else if (request.targetPrice() != null) {
            watch.setTargetPrice(request.targetPrice());
        }
        if (request.enabled() != null) {
            watch.setEnabled(request.enabled());
        }
        return toResponse(watchRepository.save(watch));
    }

    @Transactional
    public void delete(Long id) {
        ProductWatch watch = findOwnedWatch(id);
        notificationEventRepository.deleteAllByWatch_Id(watch.getId());
        observationRepository.deleteAllByWatch_Id(watch.getId());
        watchRepository.delete(watch);
    }

    @Transactional(readOnly = true)
    public List<ProductObservationResponse> history(Long id) {
        ProductWatch watch = findOwnedWatch(id);
        return observationRepository
                .findAllByWatch_IdAndWatch_Owner_IdOrderByObservedAtDescIdDesc(watch.getId(), currentUser.id())
                .stream()
                .map(observation -> new ProductObservationResponse(
                        observation.getId(),
                        observation.getObservedPrice(),
                        observation.getStockStatus(),
                        observation.getObservedAt()))
                .toList();
    }

    private ProductWatch findOwnedWatch(Long id) {
        return watchRepository.findByIdAndOwner_Id(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Product watch " + id + " not found"));
    }

    private void validateTargetPrice(BigDecimal targetPrice) {
        if (targetPrice != null && targetPrice.signum() <= 0) {
            throw new IllegalArgumentException("Target price must be greater than zero");
        }
    }

    private static ProductWatchResponse toResponse(ProductWatch watch) {
        return new ProductWatchResponse(
                watch.getId(),
                watch.getProductName(),
                watch.getProductUrl(),
                watch.getTargetPrice(),
                watch.getLatestObservedPrice(),
                watch.getLatestStockStatus(),
                watch.getLastSuccessfulCheck(),
                watch.isEnabled(),
                watch.getCreatedAt(),
                watch.getUpdatedAt());
    }
}
