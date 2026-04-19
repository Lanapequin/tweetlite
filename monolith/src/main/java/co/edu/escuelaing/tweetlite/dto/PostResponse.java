package co.edu.escuelaing.tweetlite.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String content;
    private String authorName;
    private String authorEmail;
    private String authorPicture;
    private LocalDateTime createdAt;
}