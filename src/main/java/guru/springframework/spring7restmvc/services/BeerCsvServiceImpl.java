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
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 09-10-2026
 */
@Service
public class BeerCsvServiceImpl implements BeerCsvService {
    @Override
    public List<BeerCSVRecord> convertCSV(Resource resource) {

        try( Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return new CsvToBeanBuilder<BeerCSVRecord>(reader)
                    .withType(BeerCSVRecord.class)
                    .build().parse();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV file " + resource.getFilename(), e);
        }
    }
}
