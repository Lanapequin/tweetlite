package co.edu.escuelaing.tweetlite.repository;

import co.edu.escuelaing.tweetlite.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Post> findByAuthorAuth0IdOrderByCreatedAtDesc(String auth0Id);
}