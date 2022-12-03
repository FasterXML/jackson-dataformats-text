package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;

public class FastParserStreamingCSVReadTest extends StreamingCSVReadTest {
    private final CsvFactory CSV_F = CsvFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
            .build();

    @Override
    protected CsvFactory csvFactory() {
        return CSV_F;
    }
}
