package co.edu.escuelaing.tweetlite.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String auth0Id;

    @Column(nullable = false)
    private String email;

    private String name;
    private String picture;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}