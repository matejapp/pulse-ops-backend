package com.mateja.pulseops.config;

import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.auth.domain.UserAccount;
import com.mateja.pulseops.auth.persistence.UserAccountRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserSeeder implements ApplicationRunner {
    private final UserAccountRepo  userAccountRepo;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.admin.default-email}")
    private String adminUserEmail;
    @Value("${app.admin.default-password}")
    private String adminUserPassword;

    public  AdminUserSeeder(UserAccountRepo  userAccountRepo, PasswordEncoder passwordEncoder) {
        this.userAccountRepo = userAccountRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        boolean adminUserExists = userAccountRepo.existsByRole(Role.ADMIN);

        if(adminUserExists) {
            return;
        }

        String hashedPassword = passwordEncoder.encode(adminUserPassword);

        UserAccount newUser = new UserAccount(adminUserEmail,hashedPassword, Role.ADMIN);
        userAccountRepo.save(newUser);


    }
}
