package guru.springframework.spring7restmvc.services;

import com.opencsv.bean.CsvToBeanBuilder;
import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 25-10-2026
 */
@Service
public class BeerCsvServiceImpl implements BeerCsvService {
    @Override
    public List<BeerCSVRecord> convertCSV(Reader reader) {

        try(Reader r = reader) {
            return new CsvToBeanBuilder<BeerCSVRecord>(r)
                    .withType(BeerCSVRecord.class)
                    .build().parse();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
