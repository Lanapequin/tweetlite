package co.edu.escuelaing.tweetlite.service;

import co.edu.escuelaing.tweetlite.dto.PostRequest;
import co.edu.escuelaing.tweetlite.dto.PostResponse;
import co.edu.escuelaing.tweetlite.model.Post;
import co.edu.escuelaing.tweetlite.model.User;
import co.edu.escuelaing.tweetlite.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public PostResponse createPost(PostRequest request, User author) {
        Post post = new Post();
        post.setContent(request.getContent());
        post.setAuthor(author);
        return toResponse(postRepository.save(post));
    }

    public List<PostResponse> getStream(int page, int size) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PostResponse> getUserPosts(String auth0Id) {
        return postRepository.findByAuthorAuth0IdOrderByCreatedAtDesc(auth0Id)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PostResponse toResponse(Post post) {
        PostResponse r = new PostResponse();
        r.setId(post.getId());
        r.setContent(post.getContent());
        r.setAuthorName(post.getAuthor().getName());
        r.setAuthorEmail(post.getAuthor().getEmail());
        r.setAuthorPicture(post.getAuthor().getPicture());
        r.setCreatedAt(post.getCreatedAt());
        return r;
    }
}