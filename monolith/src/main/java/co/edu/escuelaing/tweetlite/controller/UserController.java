package co.edu.escuelaing.tweetlite.controller;

import co.edu.escuelaing.tweetlite.dto.PostResponse;
import co.edu.escuelaing.tweetlite.dto.UserResponse;
import co.edu.escuelaing.tweetlite.model.User;
import co.edu.escuelaing.tweetlite.service.PostService;
import co.edu.escuelaing.tweetlite.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;
    private final PostService postService;

    @GetMapping("/me")
    @Operation(summary = "Get current user info", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setAuth0Id(user.getAuth0Id());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setPicture(user.getPicture());
        response.setCreatedAt(user.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/posts")
    @Operation(summary = "Get my posts", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<PostResponse>> getMyPosts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(postService.getUserPosts(jwt.getSubject()));
    }
}