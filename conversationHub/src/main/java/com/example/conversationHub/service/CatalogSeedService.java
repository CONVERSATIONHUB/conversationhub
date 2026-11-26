package com.example.conversationHub.service;

import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.ServiceCategory;
import com.example.conversationHub.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Seeds the launch service catalogue from app.catalog.services[*] configuration.
 * Existing service types are never overwritten, so operator edits survive restarts
 * and new types can be added with configuration or through /api/admin/services.
 */
@Component
public class CatalogSeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedService.class);
    private static final int MAX_CONFIGURED_SERVICES = 100;

    private final ServiceRepository serviceRepository;
    private final Environment environment;

    public CatalogSeedService(ServiceRepository serviceRepository, Environment environment) {
        this.serviceRepository = serviceRepository;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (int i = 0; i < MAX_CONFIGURED_SERVICES; i++) {
            String prefix = "app.catalog.services[" + i + "].";
            String categoryValue = environment.getProperty(prefix + "category");
            String serviceType = environment.getProperty(prefix + "service-type");
            if (categoryValue == null || categoryValue.isBlank()
                    || serviceType == null || serviceType.isBlank()) {
                break;
            }
            ServiceCategory category;
            try {
                category = ServiceCategory.valueOf(categoryValue.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping catalog seed [{}]: unknown category {}", i, categoryValue);
                continue;
            }

            if (serviceRepository.findByCategoryAndServiceType(category, serviceType).isPresent()) {
                continue;
            }
            ServiceEntity entity = new ServiceEntity();
            entity.setCategory(category);
            entity.setServiceType(serviceType);
            entity.setDurationMinutes(environment.getProperty(prefix + "duration-minutes", Integer.class, 30));
            String charge = environment.getProperty(prefix + "charge-information");
            entity.setChargeInformation(charge == null || charge.isBlank() ? null : charge);
            entity.setActive(true);
            serviceRepository.save(entity);
            log.info("Seeded service type {} ({})", serviceType, category);
        }
    }
}
