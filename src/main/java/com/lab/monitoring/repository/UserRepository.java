package com.lab.monitoring.repository;

import com.lab.monitoring.entity.Enums;
import com.lab.monitoring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    List<User> findByRole(Enums.UserRole role);
}
