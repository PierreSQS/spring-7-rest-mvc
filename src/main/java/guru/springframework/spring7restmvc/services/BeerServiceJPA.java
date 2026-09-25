package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.mappers.BeerMapper;
import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 22-09-2026
 */
@Service
@Primary
@RequiredArgsConstructor
public class BeerServiceJPA implements BeerService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    /** A client must not be able to pull the whole table in one request. */
    private static final int MAX_PAGE_SIZE = 1000;

    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

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

        // a Page maps its content and keeps the totals, so the client learns how many pages exist
        Page<BeerDTO> beerDtoPage = beerPage.map(beerMapper::beerToBeerDto);

        // hide the stock on the copies the client gets, never on the entities: inside a transaction
        // Hibernate would take the emptied field for a change and write the nulls to the database
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
    PageRequest buildPageRequest(Integer pageNumber, Integer pageSize) {
        int queryPageNumber = pageNumber != null && pageNumber > 0
                ? pageNumber - 1
                : DEFAULT_PAGE;

        int queryPageSize = pageSize != null && pageSize > 0
                ? Math.min(pageSize, MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;

        return PageRequest.of(queryPageNumber, queryPageSize, Sort.by("beerName").ascending());
    }

    private Page<Beer> listBeersByNameAndStyle(String beerName, BeerStyle beerStyle, Pageable pageable) {
        return beerRepository.findAllByBeerNameIsLikeIgnoreCaseAndBeerStyle("%" + beerName + "%", beerStyle, pageable);
    }

    private Page<Beer> listBeersByStyle(BeerStyle beerStyle, Pageable pageable) {
        return beerRepository.findAllByBeerStyle(beerStyle, pageable);
    }

    private Page<Beer> listBeersByName(String beerName, Pageable pageable) {
        return beerRepository.findAllByBeerNameIsLikeIgnoreCase("%" + beerName + "%", pageable);
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID id) {
        return beerRepository.findById(id).map(beerMapper::beerToBeerDto);
    }

    @Override
    public BeerDTO saveNewBeer(BeerDTO beer) {
        return beerMapper.beerToBeerDto(beerRepository.save(beerMapper.beerDtoToBeer(beer)));
    }

    @Transactional
    @Override
    public Optional<BeerDTO> updateBeerById(UUID beerId, BeerDTO beer) {
        return beerRepository.findById(beerId).map(foundBeer -> {
            foundBeer.setBeerName(beer.getBeerName());
            foundBeer.setBeerStyle(beer.getBeerStyle());
            foundBeer.setUpc(beer.getUpc());
            foundBeer.setPrice(beer.getPrice());
            foundBeer.setQuantityOnHand(beer.getQuantityOnHand());
            return beerMapper.beerToBeerDto(beerRepository.save(foundBeer));
        });
    }

    @Override
    public Boolean deleteById(UUID beerId) {
        if (beerRepository.existsById(beerId)) {
            beerRepository.deleteById(beerId);
            return true;
        }
        return false;
    }

    // The patching itself lives in BeerMapper.updateBeerFromDto: MapStruct copies every field the
    // request carries and skips the null ones. Compare with JT's version, which does it by hand with
    // one if-block per field (JT-Origin/102-rel-beer-order-repository, BeerServiceJPA).
    @Transactional
    @Override
    public Optional<BeerDTO> patchBeerById(UUID beerId, BeerDTO beer) {
        return beerRepository.findById(beerId).map(foundBeer -> {
            beerMapper.updateBeerFromDto(beer, foundBeer);
            return beerMapper.beerToBeerDto(beerRepository.save(foundBeer));
        });
    }
}
