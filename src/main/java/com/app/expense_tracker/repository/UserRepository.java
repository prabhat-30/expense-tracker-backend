package com.app.expense_tracker.repository;

import com.app.expense_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
       Optional<User> findByUsername(String username);
       long countByIsActiveTrue();
       long countByIsActiveFalse();
       Optional<User> findByEmail(String email);

       @Query("SELECT u FROM User u WHERE u.phoneNo = :phoneNo")
       Optional<User> streamAllByPhoneNo(@Param("phoneNo") String phoneNo);

       // 🌟 NEW: Locate user via their specific recovery token node
       @Query("SELECT u FROM User u WHERE u.otp = :token")
       Optional<User> findByResetToken(@Param("token") String token);
}