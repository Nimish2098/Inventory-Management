package com.miyuki.Inventory.Management.external;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductWatchRepository extends JpaRepository<ProductWatch, Long> {

    List<ProductWatch> findAllByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    Optional<ProductWatch> findByIdAndOwner_Id(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select watch from ProductWatch watch where watch.id = :id")
    Optional<ProductWatch> findByIdForUpdate(@Param("id") Long id);

    Page<ProductWatch> findAllByEnabledTrue(Pageable pageable);
}
