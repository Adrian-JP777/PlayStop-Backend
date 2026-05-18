package com.playstop.backend.repository;

import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    List<User> findTop5ByRoleOrderByCreatedAtDesc(Role role);
}