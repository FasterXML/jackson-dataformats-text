package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.csv.*;

@SuppressWarnings("serial")
public class FilteringTest extends ModuleTestBase
{
    static class Entity {
        public String name;
        public String unusedFieldBetween;
        public String description;
        public String unusedField;

        public Entity(String name, String description, String unusedField) {
            this.name = name;
            this.description = description;
            this.unusedField = unusedField;
        }
    }

    private final static String CSV_FILTER_NAME = "csvFilter";

    static class CsvJacksonWriter {
        public void writeObjects(OutputStream outputStream,
                List<?> objects, CsvSchema csvSchema ) throws IOException
        {
            HashSet<String> columnNames = new HashSet<String>();
            for (CsvSchema.Column column : csvSchema) {
                columnNames.add( column.getName() );
            }

            SimpleBeanPropertyFilter csvReponseFilter =
                    new SimpleBeanPropertyFilter.FilterExceptFilter(columnNames);
            FilterProvider filterProvider = new SimpleFilterProvider().addFilter( CSV_FILTER_NAME, csvReponseFilter );

            CsvMapper csvMapper = new CsvMapper();
            csvMapper.setFilterProvider( filterProvider );
            csvMapper.setAnnotationIntrospector(new CsvAnnotationIntrospector());

            ObjectWriter objectWriter = csvMapper.writer(csvSchema);
            objectWriter.writeValue( outputStream, objects);
        }
    }

    static class CsvAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public Object findFilterId(Annotated a) {
            return CSV_FILTER_NAME;
        }
    }

    public void testWriteObjects() throws Exception {
        List<Entity> entities = new ArrayList<Entity>();
        entities.add( new Entity("Test entity 1", "Test description 1", "Test unused field"));
        entities.add(new Entity("Test entity 2", "Test description 2", "Test unused field"));

        CsvSchema csvSchema = CsvSchema.builder()
                .addColumn("name")
                .addColumn("description")
                .setUseHeader( true )
                .build()
                .withLineSeparator("\r\n");

        CsvJacksonWriter csvWriter = new CsvJacksonWriter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        csvWriter.writeObjects(outputStream, entities, csvSchema);

        StringBuffer expectedResults = new StringBuffer();
        expectedResults.append( "name,description\r\n" );
        expectedResults.append( "\"Test entity 1\",\"Test description 1\"\r\n" );
        expectedResults.append( "\"Test entity 2\",\"Test description 2\"\r\n");

        assertEquals( expectedResults.toString(), outputStream.toString() );
    }
}
