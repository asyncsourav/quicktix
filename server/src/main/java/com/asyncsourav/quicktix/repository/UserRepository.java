

package com.asyncsourav.quicktix.repository;


import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;




@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find out the User by the Email id
    Optional<User> findByEmail(String email);

    // If user exist by that email
    boolean existsByEmail(String email);

    // List of users by the specific role
    List<User> findByRole(Role role);
}
