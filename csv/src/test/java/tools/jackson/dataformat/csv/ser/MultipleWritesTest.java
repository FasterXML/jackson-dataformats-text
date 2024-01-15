package tools.jackson.dataformat.csv.ser;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;

import tools.jackson.dataformat.csv.CsvGenerator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class MultipleWritesTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "a", "b", "c" })
    public static class Pojo {
        public int a, b, c;

        public Pojo(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private final CsvMapper MAPPER = mapperForCsv();

    /**
     * To reproduce [dataformat-csv#71]: Although ideally one uses
     * `ObjectWriter.writeValues()` for sequences, we should not
     * write headers more than once regardless, as long as target
     * is CsvGenerator which tracks state.
     */
    public void testMultipleListWrites() throws Exception
    {
        StringWriter sw = new StringWriter();

        CsvSchema.Builder builder = CsvSchema.builder();
        builder.addColumn("col1");
        builder.addColumn("col2");

        CsvSchema csvSchema = builder.build().withHeader();

        JsonGenerator gen = MAPPER
                .writer()
                .with(csvSchema)
                .createGenerator(sw);

        ObjectWriter csvWriter = MAPPER.writer();

        List<String> line1 = new ArrayList<String>();
        line1.add("line1-val1");
        line1.add("line1-val2");

        csvWriter.writeValue(gen, line1);

        List<String> line2 = new ArrayList<String>();
        line2.add("line2-val1");
        line2.add("line2-val2");

        csvWriter.writeValue(gen, line2);

        gen.close();

        String csv = sw.toString().trim();
        // may get different linefeed on different OSes?
        csv = csv.replaceAll("[\\r\\n]", "/");
        assertEquals("col1,col2/line1-val1,line1-val2/line2-val1,line2-val2", csv);
    }

    public void testWriteValuesWithPOJOs() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(Pojo.class).withUseHeader(true);

        ObjectWriter writer = MAPPER.writer(schema);
        StringWriter sw = new StringWriter();
        try (SequenceWriter seqw = writer.writeValues(sw)) {
            seqw.write(new Pojo(1, 2, 3));
            seqw.write(new Pojo(0, 15, 9));
            seqw.write(new Pojo(7, 8, 9));
        }
        assertEquals("a,b,c\n1,2,3\n0,15,9\n7,8,9\n",
                sw.toString());

        // 14-Jan-2024, tatu: [dataformats-text#45] allow suppressing trailing LF.
        // NOTE! Any form of `flush()` will prevent ability to "remove" trailing LF so...
        writer = writer
                .without(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)
                .without(CsvGenerator.Feature.WRITE_LINEFEED_AFTER_LAST_ROW);
        sw = new StringWriter();
        try (SequenceWriter seqw = writer.writeValues(sw)) {
            seqw.write(new Pojo(1, 2, 3));
            seqw.write(new Pojo(0, 15, 9));
            seqw.write(new Pojo(7, 8, 9));
        }
        assertEquals("a,b,c\n1,2,3\n0,15,9\n7,8,9",
                sw.toString());
    }
}
