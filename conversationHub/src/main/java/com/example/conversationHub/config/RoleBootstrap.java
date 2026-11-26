package com.example.conversationHub.config;

import com.example.conversationHub.domain.AppRole;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.repository.AppRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Seeds every role that the application can assign. This runner is ordered before the admin
 * bootstrap so account creation never fails because its role row is missing.
 */
@Configuration
public class RoleBootstrap {

    private static final Logger log = LoggerFactory.getLogger(RoleBootstrap.class);

    @Bean
    @Order(0)
    CommandLineRunner seedRoles(AppRoleRepository roleRepository) {
        return args -> {
            Set<String> roleNames = new LinkedHashSet<>();
            for (AccountType accountType : AccountType.values()) {
                roleNames.add(accountType.name());
            }
            roleNames.add("GUEST");

            for (String roleName : roleNames) {
                if (roleRepository.findByName(roleName).isPresent()) {
                    continue;
                }
                AppRole role = new AppRole();
                role.setName(roleName);
                role.setDescription("Platform role " + roleName);
                roleRepository.save(role);
                log.info("Seeded missing role {}", roleName);
            }
        };
    }
}
