package com.example.conversationHub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final LocaleAwareAuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(LocaleAwareAuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public FilterRegistrationBean<HttpsEnforcementFilter> httpsEnforcementFilter(
            @Value("${app.security.require-https:false}") boolean requireHttps) {
        FilterRegistrationBean<HttpsEnforcementFilter> registration =
                new FilterRegistrationBean<>(new HttpsEnforcementFilter(requireHttps));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/services", "/providers", "/register", "/register/**",
                                "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("MAIN_ADMIN")
                        // Institution reports are available to assigned officers as well as managers.
                        .requestMatchers(HttpMethod.GET,
                                "/api/institutions/*/reports",
                                "/api/institutions/*/reports/bookings.csv")
                        .hasAnyRole("CENTER_MANAGER", "APPOINTMENT_OFFICER", "MAIN_ADMIN")
                        .requestMatchers("/api/institutions/**")
                        .hasAnyRole("CENTER_MANAGER", "MAIN_ADMIN")
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
