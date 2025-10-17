package com.github.deviceflow.repository;

import com.github.deviceflow.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    
    Optional<UserToken> findByUserId(String userId);
    
    Optional<UserToken> findByUsername(String username);
    
    void deleteByUserId(String userId);
}

