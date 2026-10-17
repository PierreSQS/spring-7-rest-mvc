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
 * Modified by Pierrot on 18-10-2026
 */
@Mapper
public interface BeerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "beerOrderLines", ignore = true)
    @Mapping(target = "categories", ignore = true)
    Beer beerDtoToBeer(BeerDTO beerDTO);

    BeerDTO beerToBeerDto(Beer beer);

    /**
     * Copies the fields a PATCH request carries onto an existing beer. MapStruct generates one
     * null check per property ({@code IGNORE}), so a field added later needs no change in the service.
     * The id, the version, the timestamps and the relationships are owned by the persistence layer,
     * so a request body cannot overwrite them.
     * <p>
     * Difference with JT's code: his {@code StringUtils.hasText(...)} checks silently skipped an
     * empty {@code beerName} or {@code upc} and answered 204 although nothing had changed. Here only
     * {@code null} is skipped: an empty name or upc is copied, rejected by {@code @NotBlank} when the
     * beer is saved, and the client gets 400. The beer in the database stays unchanged.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "beerOrderLines", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchBeerFromDto(BeerDTO beerDTO, @MappingTarget Beer beer);

    /**
     * Replaces all the fields of an existing beer for a PUT: a {@code null} field of the DTO
     * becomes {@code null} on the beer (MapStruct's default), because a PUT sends the whole beer.
     * Deliberately NO {@code IGNORE} here, unlike {@link #patchBeerFromDto}: with it, a PUT without
     * {@code quantityOnHand} would keep the old value and behave like a PATCH.
     * The id, the version, the timestamps and the relationships stay protected as in the PATCH.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "beerOrderLines", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateBeerFromDto(BeerDTO beerDTO, @MappingTarget Beer beer);

}
