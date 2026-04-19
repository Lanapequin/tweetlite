package co.edu.escuelaing.tweetlite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRequest {
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 140, message = "Post cannot exceed 140 characters")
    private String content;
}