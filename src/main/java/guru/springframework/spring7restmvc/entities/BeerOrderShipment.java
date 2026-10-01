package guru.springframework.spring7restmvc.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 30-09-2026
 */
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class BeerOrderShipment {

    @Id
    @UuidGenerator
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Version
    private Integer version;

    @Column(length = 50)
    private String trackingNumber;

    @OneToOne(mappedBy = "beerOrderShipment")
    private BeerOrder beerOrder;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
