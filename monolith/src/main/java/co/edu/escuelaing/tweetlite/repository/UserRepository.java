package co.edu.escuelaing.tweetlite.repository;

import co.edu.escuelaing.tweetlite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuth0Id(String auth0Id);
    Optional<User> findByEmail(String email);
}