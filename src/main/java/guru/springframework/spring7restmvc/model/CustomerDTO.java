package guru.springframework.spring7restmvc.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-09-2026
 */
@Data
@Builder
public class CustomerDTO {
    private UUID id;
    private String name;
    private String email;
    private Integer version;
    private LocalDateTime createdDate;
    private LocalDateTime updateDate;
}
