package guru.springframework.spring7restmvc.services;

import com.opencsv.bean.CsvToBeanBuilder;
import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 09-10-2026
 */
@Service
public class BeerCsvServiceImpl implements BeerCsvService {
    @Override
    public List<BeerCSVRecord> convertCSV(File csvFile) {

        try(FileReader fileReader = new FileReader(csvFile)) {
            return new CsvToBeanBuilder<BeerCSVRecord>(fileReader)
                    .withType(BeerCSVRecord.class)
                    .build().parse();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV file " + csvFile.getAbsolutePath(), e);
        }
    }
}
