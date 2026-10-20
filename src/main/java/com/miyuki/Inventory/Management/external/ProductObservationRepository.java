package com.miyuki.Inventory.Management.external;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductObservationRepository extends JpaRepository<ProductObservation, Long> {

    Optional<ProductObservation> findTopByWatch_IdOrderByObservedAtDescIdDesc(Long watchId);

    List<ProductObservation> findAllByWatch_IdAndWatch_Owner_IdOrderByObservedAtDescIdDesc(
            Long watchId, Long ownerId);

    void deleteAllByWatch_Id(Long watchId);
}
