package guru.springframework.spring7restmvc.mappers;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.model.BeerDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 26-10-2026
 */
@Mapper
public interface BeerMapper {

    @Mapping(target = "beerOrderLines", ignore = true)
    @Mapping(target = "categories", ignore = true)
    Beer beerDtoToBeer(BeerDTO dto);

    BeerDTO beerToBeerDto(Beer beer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "beerOrderLines", ignore = true)
    @Mapping(target = "categories", ignore = true)
    void updateBeerFromDto(BeerDTO dto, @MappingTarget Beer beer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "beerOrderLines", ignore = true)
    @Mapping(target = "categories", ignore = true)
    void patchBeerFromDto(BeerDTO dto, @MappingTarget Beer beer);

}
