package tools.jackson.dataformat.csv.deser;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.CsvMapper;

public class FastParserStreamingCSVReadTest extends StreamingCSVReadTest {
    private final CsvFactory CSV_F = CsvFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
            .build();

    @Override
    protected CsvMapper csvMapper() {
        return new CsvMapper(CSV_F);
    }
}
