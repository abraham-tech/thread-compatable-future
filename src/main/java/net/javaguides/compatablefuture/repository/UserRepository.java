package net.javaguides.compatablefuture.repository;

import net.javaguides.compatablefuture.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}