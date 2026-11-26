package com.example.conversationHub.repository;

import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByCategory(ServiceCategory category);

    List<ServiceEntity> findByActiveTrue();

    List<ServiceEntity> findByCategoryAndActiveTrue(ServiceCategory category);

    Optional<ServiceEntity> findByCategoryAndServiceType(ServiceCategory category, String serviceType);
}
