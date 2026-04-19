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
                    user.setEmail(resolveEmail(jwt));
                    user.setName(resolveName(jwt));
                    user.setPicture(jwt.getClaimAsString("picture"));
                    return userRepository.save(user);
                });
    }

    private String resolveEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        if (preferred != null && !preferred.isBlank()) {
            return preferred.contains("@") ? preferred : preferred + "@users.auth0.local";
        }
        String nickname = jwt.getClaimAsString("nickname");
        if (nickname != null && !nickname.isBlank()) {
            return nickname.contains("@") ? nickname : nickname + "@users.auth0.local";
        }
        return jwt.getSubject().replace("|", "_") + "@users.auth0.local";
    }

    private String resolveName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String nickname = jwt.getClaimAsString("nickname");
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        if ((given != null && !given.isBlank()) || (family != null && !family.isBlank())) {
            return ((given != null ? given : "") + " " + (family != null ? family : "")).trim();
        }
        return "User";
    }
}