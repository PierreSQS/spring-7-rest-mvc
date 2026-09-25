package guru.springframework.spring7restmvc.mappers;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.model.BeerDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 25-09-2026
 */
@Mapper
public interface BeerMapper {

    // the DTO carries no order lines; they are never set from a request body
    @Mapping(target = "beerOrderLines", ignore = true)
    Beer beerDtoToBeer(BeerDTO dto);

    BeerDTO beerToBeerDto(Beer beer);

    /**
     * Copies the fields a PATCH request carries onto an existing beer: MapStruct generates one null
     * check per property ({@code IGNORE}), so a new field needs no change in the service.
     * <p>
     * The id, the version, the timestamps and the order lines are owned by the persistence layer, so a
     * request body cannot overwrite them.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "beerOrderLines", ignore = true)
    void updateBeerFromDto(BeerDTO dto, @MappingTarget Beer beer);

}
