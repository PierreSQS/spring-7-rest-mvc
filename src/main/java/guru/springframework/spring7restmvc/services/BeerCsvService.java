package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Reads a beer CSV file into {@link BeerCSVRecord}s.
 */
public interface BeerCsvService {

    List<BeerCSVRecord> convertCSV(Resource csvResource);
}
