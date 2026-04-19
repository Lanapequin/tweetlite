package co.edu.escuelaing.tweetlite.service;

import co.edu.escuelaing.tweetlite.model.User;
import co.edu.escuelaing.tweetlite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getOrCreateUser(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        return userRepository.findByAuth0Id(auth0Id)
                .orElseGet(() -> {
                    User user = new User();
                    user.setAuth0Id(auth0Id);
                    user.setEmail(jwt.getClaimAsString("email"));
                    user.setName(jwt.getClaimAsString("name"));
                    user.setPicture(jwt.getClaimAsString("picture"));
                    return userRepository.save(user);
                });
    }
}