package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 09-10-2026
 */
public interface BeerCsvService {
    List<BeerCSVRecord> convertCSV(Resource resource);
}
