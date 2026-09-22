package guru.springframework.spring7restmvc.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-09-2026
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    @Id
    @UuidGenerator
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    /** A PATCH or PUT carrying an empty name must fail rather than silently keep the old one. */
    @NotBlank
    private String name;

    private String email;

    @Version
    private Integer version;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
