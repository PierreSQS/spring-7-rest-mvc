package guru.springframework.spring7restmvc.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line of {@code classpath:csvdata/beers.csv}, mapped by header name.
 * <p>
 * OpenCSV matches {@link CsvBindByName} case-insensitively against the header, and falls back to the
 * field name when no {@code column} is given, so only the columns whose header is not a legal Java
 * identifier ({@code count.x}, {@code count.y}, {@code brewery_id}) need one.
 * <p>
 * {@code abv}, {@code ibu} and {@code style2} stay {@link String}: the file writes {@code NA} for a
 * missing value (62, 1005 and 1298 of the 2410 records), which no numeric converter accepts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BeerCSVRecord {

    @CsvBindByName(required = true)
    private Integer row;

    @CsvBindByName(column = "count.x", required = true)
    private Integer count;

    @CsvBindByName
    private String abv;

    @CsvBindByName
    private String ibu;

    @CsvBindByName(required = true)
    private Integer id;

    @CsvBindByName(required = true)
    private String beer;

    @CsvBindByName(required = true)
    private String style;

    @CsvBindByName(column = "brewery_id", required = true)
    private Integer breweryId;

    @CsvBindByName(required = true)
    private Float ounces;

    @CsvBindByName
    private String style2;

    @CsvBindByName(column = "count.y")
    private String countY;

    @CsvBindByName
    private String brewery;

    @CsvBindByName
    private String city;

    @CsvBindByName
    private String state;

    @CsvBindByName
    private String label;

}
