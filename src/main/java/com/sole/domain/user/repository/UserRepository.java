package com.sole.domain.user.repository;

import com.sole.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // /me 조회시 region까지 한번에 로딩
    @EntityGraph(attributePaths = "region")
    Optional<User> findWithRegionById(Long id);
}
