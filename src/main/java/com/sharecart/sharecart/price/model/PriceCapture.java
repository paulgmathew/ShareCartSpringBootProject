package com.sharecart.sharecart.price.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_captures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCapture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    private Double latitude;

    private Double longitude;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
