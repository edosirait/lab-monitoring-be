package com.lab.monitoring.controller;

import com.lab.monitoring.dto.AuthResponseDTO;
import com.lab.monitoring.dto.LoginRequestDTO;
import com.lab.monitoring.entity.User;
import com.lab.monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        // 1. Cari user berdasarkan username di database
        User user = userRepository.findByUsername(dto.getUsername())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Username tidak ditemukan!");
        }

        // Ubah bagian pengecekan password menjadi passwordHash sesuai entitas Anda
        if (!user.getPasswordHash().equals(dto.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Password yang Anda masukkan salah!");
        }

        // 3. Generate token sesi unik jika login sukses
        String dummyJwtToken = "JWT-SESSION-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 4. Kirim respon balik lengkap dengan ROLE-nya ke Angular
        AuthResponseDTO response = new AuthResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole().name(),
                dummyJwtToken
        );

        return ResponseEntity.ok(response);
    }
}
