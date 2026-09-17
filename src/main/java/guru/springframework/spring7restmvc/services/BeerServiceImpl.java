package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-09-2026
 */
@Slf4j
@Service
public class BeerServiceImpl implements BeerService {

    private final Map<UUID, BeerDTO> beerMap;

    public BeerServiceImpl() {
        this.beerMap = new HashMap<>();

        BeerDTO beer1 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Galaxy Cat")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356")
                .price(new BigDecimal("12.99"))
                .quantityOnHand(122)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        BeerDTO beer2 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Crank")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356222")
                .price(new BigDecimal("11.99"))
                .quantityOnHand(392)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        BeerDTO beer3 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Sunshine City")
                .beerStyle(BeerStyle.IPA)
                .upc("12356")
                .price(new BigDecimal("13.99"))
                .quantityOnHand(144)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        beerMap.put(beer1.id(), beer1);
        beerMap.put(beer2.id(), beer2);
        beerMap.put(beer3.id(), beer3);
    }

    @Override
    public Optional<BeerDTO> patchBeerById(UUID beerId, BeerDTO beer) {
        BeerDTO.BeerDTOBuilder patched = beerMap.get(beerId).toBuilder();

        if (StringUtils.hasText(beer.beerName())){
            patched.beerName(beer.beerName());
        }

        if (beer.beerStyle() != null) {
            patched.beerStyle(beer.beerStyle());
        }

        if (beer.price() != null) {
            patched.price(beer.price());
        }

        if (beer.quantityOnHand() != null){
            patched.quantityOnHand(beer.quantityOnHand());
        }

        if (StringUtils.hasText(beer.upc())) {
            patched.upc(beer.upc());
        }

        BeerDTO existing = patched.build();
        beerMap.put(beerId, existing);

        return Optional.of(existing);
    }

    @Override
    public Boolean deleteById(UUID beerId) {
        beerMap.remove(beerId);

        return true;
    }

    @Override
    public Optional<BeerDTO> updateBeerById(UUID beerId, BeerDTO beer) {
        BeerDTO existing = beerMap.get(beerId).toBuilder()
                .beerName(beer.beerName())
                .price(beer.price())
                .upc(beer.upc())
                .quantityOnHand(beer.quantityOnHand())
                .build();

        beerMap.put(beerId, existing);
        return Optional.of(existing);
    }

    @Override
    public List<BeerDTO> listBeers(){
        return new ArrayList<>(beerMap.values());
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID id) {

        log.debug("Get Beer by Id - in service. Id: {}", id);

        return Optional.of(beerMap.get(id));
    }

    @Override
    public BeerDTO saveNewBeer(BeerDTO beer) {

        BeerDTO savedBeer = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .beerName(beer.beerName())
                .beerStyle(beer.beerStyle())
                .quantityOnHand(beer.quantityOnHand())
                .upc(beer.upc())
                .price(beer.price())
                .build();

        beerMap.put(savedBeer.id(), savedBeer);

        return savedBeer;
    }
}

















