package com.example.conversationHub.config;

import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    CommandLineRunner bootstrapAdmin(AppUserRepository userRepository,
                                     AccountService accountService,
                                     @Value("${app.admin.bootstrap.enabled:true}") boolean enabled,
                                     @Value("${app.admin.name:Main Administrator}") String name,
                                     @Value("${app.admin.phone:+255700000000}") String phone,
                                     @Value("${app.admin.email:admin@conversationhub.local}") String email,
                                     @Value("${app.admin.password:ChangeMe123!}") String password) {
        return args -> {
            if (!enabled) {
                return;
            }
            if (userRepository.findByPhone(phone).isPresent()
                    || (email != null && !email.isBlank() && userRepository.findByEmail(email).isPresent())) {
                return;
            }
            accountService.createUser(
                    name,
                    phone,
                    email,
                    password,
                    AccountType.MAIN_ADMIN,
                    "MAIN_ADMIN",
                    LanguageCode.EN,
                    null,
                    true
            );
            log.info("Bootstrapped main admin account with phone {}", phone);
        };
    }
}
