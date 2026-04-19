package co.edu.escuelaing.tweetlite;

import co.edu.escuelaing.tweetlite.dto.PostRequest;
import co.edu.escuelaing.tweetlite.dto.PostResponse;
import co.edu.escuelaing.tweetlite.service.PostService;
import co.edu.escuelaing.tweetlite.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private UserService userService;

    @Test
    void getStream_ShouldReturnPublicPosts() throws Exception {
        PostResponse post = new PostResponse();
        post.setId(1L);
        post.setContent("Hello world!");
        post.setAuthorName("Test User");
        post.setCreatedAt(LocalDateTime.now());

        when(postService.getStream(0, 20)).thenReturn(List.of(post));

        mockMvc.perform(get("/api/stream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello world!"));
    }

    @Test
    void getStream_ShouldBePublicNoAuth() throws Exception {
        when(postService.getStream(0, 20)).thenReturn(List.of());
        mockMvc.perform(get("/api/stream"))
                .andExpect(status().isOk());
    }

    @Test
    void createPost_WithoutAuth_ShouldReturn401() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("Test post");

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_WithAuth_ShouldReturn200() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("Test post from authenticated user");

        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setContent(request.getContent());
        response.setCreatedAt(LocalDateTime.now());

        when(userService.getOrCreateUser(any(Jwt.class))).thenReturn(null);
        when(postService.createPost(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/posts")
                        .with(jwt().jwt(j -> j
                                .subject("auth0|123")
                                .claim("email", "test@test.com")
                                .claim("name", "Test User")
                                .audience(List.of("YOUR_AUTH0_AUDIENCE"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(request.getContent()));
    }

    @Test
    void createPost_ExceedingLimit_ShouldReturn400() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("A".repeat(141)); // 141 chars - exceeds limit

        mockMvc.perform(post("/api/posts")
                        .with(jwt().jwt(j -> j
                                .subject("auth0|123")
                                .audience(List.of("YOUR_AUTH0_AUDIENCE"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMe_WithAuth_ShouldReturnUserInfo() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(jwt().jwt(j -> j
                                .subject("auth0|123")
                                .claim("email", "test@test.com")
                                .claim("name", "Test User")
                                .audience(List.of("YOUR_AUTH0_AUDIENCE")))))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}