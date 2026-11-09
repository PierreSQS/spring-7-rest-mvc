package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.mappers.BeerMapper;
import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 09-11-2026
 */
@Service
@RequiredArgsConstructor
public class BeerServiceJPA implements BeerService {
    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    /** A client must not be able to pull the whole table in one request. */
    private static final int MAX_PAGE_SIZE = 1000;

    @Override
    public Page<BeerDTO> listBeers(String beerName, BeerStyle beerStyle, Boolean showInventory,
                                   Integer pageNumber, Integer pageSize) {

        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize);

        Page<Beer> beerPage;

        if(StringUtils.hasText(beerName) && beerStyle == null) {
            beerPage = listBeersByName(beerName, pageRequest);
        } else if (!StringUtils.hasText(beerName) && beerStyle != null){
            beerPage = listBeersByStyle(beerStyle, pageRequest);
        } else if (StringUtils.hasText(beerName) && beerStyle != null){
            beerPage = listBeersByNameAndStyle(beerName, beerStyle, pageRequest);
        } else {
            beerPage = beerRepository.findAll(pageRequest);
        }

        Page<BeerDTO> beerDtoPage = beerPage.map(beerMapper::beerToBeerDto);

        // hide the stock on the DTOs the client gets, never on the entities: inside a transaction,
        // Hibernate would take the emptied field for a change and write the null to the database (#22)
        if (showInventory != null && !showInventory) {
            beerDtoPage.forEach(beerDto -> beerDto.setQuantityOnHand(null));
        }

        return beerDtoPage;

    }

    /**
     * Turns the paging parameters of a request into Spring Data's {@link PageRequest}. The API counts
     * pages from 1, Spring Data from 0, hence the shift. A missing or nonsensical value falls back to
     * the default, so a request can never make {@code PageRequest.of} throw.
     */
    public PageRequest buildPageRequest(Integer pageNumber, Integer pageSize) {
        int queryPageNumber = pageNumber != null && pageNumber > 0
                ? pageNumber - 1
                : DEFAULT_PAGE;

        int queryPageSize = pageSize != null && pageSize > 0
                ? Math.min(pageSize, MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;

        return PageRequest.of(queryPageNumber, queryPageSize, Sort.by("beerName").ascending());
    }

    private Page<Beer> listBeersByNameAndStyle(String beerName, BeerStyle beerStyle, Pageable pageable) {
        return beerRepository.findAllByBeerNameIsLikeIgnoreCaseAndBeerStyle("%" + beerName + "%",
                beerStyle, pageable);
    }

    public Page<Beer> listBeersByStyle(BeerStyle beerStyle, Pageable pageable) {
        return beerRepository.findAllByBeerStyle(beerStyle, pageable);
    }

    public Page<Beer> listBeersByName(String beerName, Pageable pageable){
        return beerRepository.findAllByBeerNameIsLikeIgnoreCase("%" + beerName + "%", pageable);
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID id) {
        return beerRepository.findById(id).map(beerMapper::beerToBeerDto);
    }

    @Override
    public BeerDTO saveNewBeer(BeerDTO beerDTO) {
        return beerMapper.beerToBeerDto(beerRepository.save(beerMapper.beerDtoToBeer(beerDTO)));
    }

    @Override
    public Optional<BeerDTO> updateBeerById(UUID beerId, BeerDTO beerDTO) {
        return beerRepository.findById(beerId).map(foundBeer -> {
            beerMapper.updateBeerFromDto(beerDTO, foundBeer);
            return beerMapper.beerToBeerDto(beerRepository.save(foundBeer));
        });
    }

    @Override
    public boolean deleteById(UUID beerId) {
        if (beerRepository.existsById(beerId)) {
            beerRepository.deleteById(beerId);
            return true;
        }
        return false;
    }

    @Override
    public Optional<BeerDTO> patchBeerById(UUID beerId, BeerDTO beerDTO) {

        return beerRepository.findById(beerId).map(foundBeer -> {
            beerMapper.patchBeerFromDto(beerDTO, foundBeer);
            return beerMapper.beerToBeerDto(beerRepository.save(foundBeer));
        });
    }
}
