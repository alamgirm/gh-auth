package com.github.deviceflow.repository;

import com.github.deviceflow.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, String> {
    
    Optional<UserToken> findByUserId(String userId);
    
    Optional<UserToken> findByUsername(String username);
    
    @Transactional
    @Modifying
    void deleteByUserId(String userId);
}

