package guru.springframework.spring7restmvc.services;

import com.opencsv.bean.CsvToBeanBuilder;
import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Takes a {@link Resource} rather than a {@link java.io.File} so the CSV is also found inside a
 * packaged jar, and decodes it as UTF-8 instead of the platform default charset.
 */
@Service
public class BeerCsvServiceImpl implements BeerCsvService {

    @Override
    public List<BeerCSVRecord> convertCSV(Resource csvResource) {
        try (Reader reader = new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8)) {
            return new CsvToBeanBuilder<BeerCSVRecord>(reader)
                    .withType(BeerCSVRecord.class)
                    .build()
                    .parse();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + csvResource.getDescription(), e);
        }
    }
}
