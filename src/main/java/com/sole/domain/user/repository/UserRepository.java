package com.sole.domain.user.repository;

import com.sole.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findbyEmail(String email);
    boolean existsByEmail(String email);
}
