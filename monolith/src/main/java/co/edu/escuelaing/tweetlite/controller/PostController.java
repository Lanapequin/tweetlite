package co.edu.escuelaing.tweetlite.controller;

import co.edu.escuelaing.tweetlite.dto.PostRequest;
import co.edu.escuelaing.tweetlite.dto.PostResponse;
import co.edu.escuelaing.tweetlite.model.User;
import co.edu.escuelaing.tweetlite.service.PostService;
import co.edu.escuelaing.tweetlite.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post and stream management")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping("/posts")
    @Operation(summary = "Get all posts (paginated)")
    public ResponseEntity<List<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(postService.getStream(page, size));
    }

    @GetMapping("/stream")
    @Operation(summary = "Get public stream")
    public ResponseEntity<List<PostResponse>> getStream(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(postService.getStream(page, size));
    }

    @PostMapping("/posts")
    @Operation(summary = "Create a new post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User user = userService.getOrCreateUser(jwt);
        return ResponseEntity.ok(postService.createPost(request, user));
    }
}