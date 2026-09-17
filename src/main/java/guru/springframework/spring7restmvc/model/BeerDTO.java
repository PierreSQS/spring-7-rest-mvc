package guru.springframework.spring7restmvc.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-09-2026
 */
@Builder(toBuilder = true)
public record BeerDTO(

        UUID id,

        Integer version,

        @NotBlank
        @NotNull
        String beerName,

        @NotNull
        BeerStyle beerStyle,

        @NotNull
        @NotBlank
        String upc,

        Integer quantityOnHand,

        @NotNull
        BigDecimal price,

        LocalDateTime createdDate,

        LocalDateTime updateDate) {
}
