package guru.springframework.spring7restmvc.entities;

import guru.springframework.spring7restmvc.model.BeerStyle;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
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
@NoArgsConstructor
@AllArgsConstructor
public class Beer {

    @Id
    @UuidGenerator
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Version
    private Integer version;

    @NotBlank
    @Size(max = 50, message = "Beer name must not exceed 50 characters")
    @Column(length = 50)
    private String beerName;

    @NotNull
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private BeerStyle beerStyle;

    @NotBlank
    @Size(max = 255)
    private String upc;
    private Integer quantityOnHand;

    @NotNull
    private BigDecimal price;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
