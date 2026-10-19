package com.app.be.repository;

import com.app.be.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}
