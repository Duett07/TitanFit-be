package com.app.be.config;

import com.app.be.entity.User;
import com.app.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    private static final List<String> FORBIDDEN_WEAK_PASSWORDS = List.of(
            "admin123", "123456", "password", "admin", "CHANGE_ME"
    );

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.init.admin.email:}")
    private String adminEmail;

    @Value("${app.init.admin.password:}")
    private String adminPassword;

    @Value("${app.init.admin.phone:0900000001}")
    private String adminPhone;

    @Value("${app.init.buyer.email:}")
    private String buyerEmail;

    @Value("${app.init.buyer.password:}")
    private String buyerPassword;

    @Value("${app.init.buyer.phone:0912345678}")
    private String buyerPhone;

    @Override
    @Transactional
    public void run(String... args) {
        initAdminAccount();
        if (isDevEnvironment()) {
            initBuyerAccount();
        } else {
            log.info("Non-dev environment active: Demo buyer account initialization is disabled.");
        }
    }

    private boolean isDevEnvironment() {
        return environment.acceptsProfiles(Profiles.of("dev")) || seedEnabled;
    }

    private void initAdminAccount() {
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            log.info("No initial admin email configured (INIT_ADMIN_EMAIL is empty). Skipping admin account initialization.");
            return;
        }

        String email = adminEmail.trim();

        if (!isDevEnvironment()) {
            validateProductionAdminPassword(adminPassword);
        } else if (adminPassword != null && isWeakPassword(adminPassword)) {
            log.warn("⚠️ DEVELOPMENT MODE: Default/weak admin password detected for [{}]. DO NOT USE IN PRODUCTION!", email);
        }

        if (!userRepository.existsByEmail(email)) {
            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                log.warn("Admin password is empty. Skipping admin account creation for: {}", email);
                return;
            }

            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(adminPassword.trim()));
            admin.setFullName("Quản trị viên TitanFit");
            admin.setPhone(adminPhone != null && !adminPhone.isBlank() ? adminPhone.trim() : "0900000001");
            admin.setRole("ROLE_ADMIN");

            userRepository.save(admin);
            log.info("Initialized admin account: {}", email);
        } else {
            log.debug("Admin account already exists: {}", email);
        }
    }

    private void initBuyerAccount() {
        if (buyerEmail == null || buyerEmail.trim().isEmpty()) {
            log.debug("No buyer email configured (INIT_BUYER_EMAIL is empty). Skipping buyer account initialization.");
            return;
        }

        String email = buyerEmail.trim();
        if (!userRepository.existsByEmail(email)) {
            if (buyerPassword == null || buyerPassword.trim().isEmpty()) {
                log.debug("Buyer password is empty. Skipping buyer creation for: {}", email);
                return;
            }

            User buyer = new User();
            buyer.setEmail(email);
            buyer.setPassword(passwordEncoder.encode(buyerPassword.trim()));
            buyer.setFullName("Nguyễn Hoàng");
            buyer.setPhone(buyerPhone != null && !buyerPhone.isBlank() ? buyerPhone.trim() : "0912345678");
            buyer.setRole("ROLE_CUSTOMER");

            userRepository.save(buyer);
            log.info("Initialized default demo buyer account: {}", email);
        } else {
            log.debug("Buyer account already exists: {}", email);
        }
    }

    private void validateProductionAdminPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: INIT_ADMIN_PASSWORD must be configured in production/non-dev mode!");
        }

        String trimmedPassword = password.trim();

        if (trimmedPassword.length() < 8) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: INIT_ADMIN_PASSWORD must be at least 8 characters in production/non-dev mode! Current length: " + trimmedPassword.length());
        }

        if (isWeakPassword(trimmedPassword)) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: INIT_ADMIN_PASSWORD contains weak or default pattern ('admin123', '123456', 'password', 'CHANGE_ME') in production/non-dev mode! Refusing to start.");
        }
    }

    private boolean isWeakPassword(String password) {
        String lower = password.toLowerCase();
        return FORBIDDEN_WEAK_PASSWORDS.stream().anyMatch(lower::contains);
    }
}
