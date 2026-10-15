package com.app.be.config;

import com.app.be.entity.User;
import com.app.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin.email}")
    private String adminEmail;

    @Value("${app.init.admin.password}")
    private String adminPassword;

    @Value("${app.init.buyer.email}")
    private String buyerEmail;

    @Value("${app.init.buyer.password}")
    private String buyerPassword;

    @Override
    @Transactional
    public void run(String... args) {
        initAdminAccount();
        initBuyerAccount();
    }

    private void initAdminAccount() {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("Quản trị viên TitanFit");
            admin.setPhone("0900000001");
            admin.setRole("ROLE_ADMIN");

            userRepository.save(admin);
            log.info("Initialized default admin account: {}", adminEmail);
        } else {
            log.debug("Admin account already exists: {}", adminEmail);
        }
    }

    private void initBuyerAccount() {
        if (!userRepository.existsByEmail(buyerEmail)) {
            User buyer = new User();
            buyer.setEmail(buyerEmail);
            buyer.setPassword(passwordEncoder.encode(buyerPassword));
            buyer.setFullName("Nguyễn Hoàng");
            buyer.setPhone("0912345678");
            buyer.setRole("ROLE_CUSTOMER");

            userRepository.save(buyer);
            log.info("Initialized default buyer account: {}", buyerEmail);
        } else {
            log.debug("Buyer account already exists: {}", buyerEmail);
        }
    }
}
