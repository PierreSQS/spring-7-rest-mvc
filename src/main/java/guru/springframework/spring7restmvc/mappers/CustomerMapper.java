package guru.springframework.spring7restmvc.mappers;

import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.model.CustomerDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 22-09-2026
 */
@Mapper
public interface CustomerMapper {

    Customer customerDtoToCustomer(CustomerDTO dto);

    CustomerDTO customerToCustomerDto(Customer customer);

    /**
     * Copies the fields a PATCH request carries onto an existing customer: MapStruct generates one
     * null check per property ({@code IGNORE}), so a new field needs no change in the service.
     * <p>
     * The id, the version and the timestamps are owned by the persistence layer, so a request body
     * cannot overwrite them.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    void updateCustomerFromDto(CustomerDTO dto, @MappingTarget Customer customer);

}
