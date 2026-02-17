package com.shin.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, name = "show_adult_content")
    @Builder.Default
    private Boolean showAdultContent = true;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 10)
    private String locale = "en";

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    public Locale getLocale() {
        return Locale.forLanguageTag(locale);
    }

    public String getLanguageTag() {
        return getLocale().getLanguage(); // "pt"
    }

    public String getDisplayLanguage(Locale displayLocale) {
        return getLocale().getDisplayLanguage(displayLocale);
    }

}
