package guru.springframework.spring7restmvc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 29-10-2026
 */
@JsonDeserialize(builder = CustomerDTO.CustomerDTOBuilder.class)
@Data
@Builder
public class CustomerDTO {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("name")
    @NotBlank
    private String name;

    @JsonProperty("email")
    @Size(min = 1, max = 255)
    private String email;

    @JsonProperty("version")
    private Integer version;

    private LocalDateTime createdDate;
    private LocalDateTime updateDate;
}
