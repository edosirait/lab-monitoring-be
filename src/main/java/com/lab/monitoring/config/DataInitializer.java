package com.lab.monitoring.config;

import com.lab.monitoring.entity.Enums;
import com.lab.monitoring.entity.User;
import com.lab.monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Jika tabel user masih kosong, otomatis seeding data master akun testing
        if (userRepository.count() == 0) {

            // 1. Akun khusus Staff Lapangan
            User staff = new User();
            staff.setName("Dewi Marlina Romauli Panjaitan");
            staff.setUsername("staff.dewi");
            staff.setPasswordHash("password123"); // Sesuai dengan field passwordHash di User entity Anda
            staff.setRole(Enums.UserRole.STAFF);   // Sesuai dengan enum STAFF di Enums Anda
            staff.setUpdatedAt(LocalDateTime.now());
            userRepository.save(staff);

            // 2. Akun khusus Supervisor / Manager
            User supervisor = new User();
            supervisor.setName("Marlina");
            supervisor.setUsername("spv.macan");
            supervisor.setPasswordHash("password123");
            supervisor.setRole(Enums.UserRole.SUPERVISOR); // Sesuai dengan enum SUPERVISOR di Enums Anda
            supervisor.setUpdatedAt(LocalDateTime.now());
            userRepository.save(supervisor);

            // 3. Akun Super Admin
            User admin = new User();
            admin.setName("Admin Utama Laboratorium");
            admin.setUsername("admin.lab");
            admin.setPasswordHash("adminsuper");
            admin.setRole(Enums.UserRole.ADMIN);          // Sesuai dengan enum ADMIN di Enums Anda
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);

            System.out.println("====================================================================");
            System.out.println("🚩 [LAB MONITORING] BERHASIL SEEDING 3 USER ROLE KE DATABASE POSTGRES");
            System.out.println("====================================================================");
        }
    }
}
