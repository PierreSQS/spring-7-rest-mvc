package guru.springframework.spring7restmvc.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-09-2026
 */
@Builder(toBuilder = true)
public record CustomerDTO(

        UUID id,

        String name,

        String email,

        Integer version,

        LocalDateTime createdDate,

        LocalDateTime updateDate) {
}
