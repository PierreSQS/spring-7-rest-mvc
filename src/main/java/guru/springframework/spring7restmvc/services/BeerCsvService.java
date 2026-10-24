package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;

import java.io.Reader;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 25-10-2026
 */
public interface BeerCsvService {
    List<BeerCSVRecord> convertCSV(Reader reader);
}
