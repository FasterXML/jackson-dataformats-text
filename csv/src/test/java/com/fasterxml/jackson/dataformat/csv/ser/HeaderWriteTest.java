package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.*;

// Tests for verifying that headers are emitted
public class HeaderWriteTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();
    
    public void testNoLines() throws Exception
    {
        List<String> headers = Arrays.asList("TestHeader1", "TestHeader2");
        List<List<String>> dataSource = Arrays.asList();
        String result = runTest(headers, dataSource);
        
        assertEquals("Headers should have been written even with no other data", "TestHeader1,TestHeader2\n", result);
    }
    
    public void testOneLine() throws Exception
    {
        List<String> headers = Arrays.asList("TestHeader1", "TestHeader2");
        List<List<String>> dataSource = Arrays.asList(Arrays.asList("TestValue1", "TestValue2"));
        String result = runTest(headers, dataSource);
        
        assertEquals("Headers should have been written before line", "TestHeader1,TestHeader2\nTestValue1,TestValue2\n", result);
    }
    
    private String runTest(List<String> headers, List<List<String>> dataSource) throws IOException 
    {
        StringWriter writer = new StringWriter();
        
        CsvSchema.Builder builder = CsvSchema.builder();
        for (String nextHeader : headers) {
            builder = builder.addColumn(nextHeader);
        }
        
        CsvSchema schema = builder.setUseHeader(true).build();
        try (SequenceWriter csvWriter = MAPPER.writerWithDefaultPrettyPrinter()
                                              .with(schema)
                                              .forType(List.class)
                                              .writeValues(writer);) {
            for(List<String> nextRow : dataSource) {
                csvWriter.write(nextRow);
            }
        }
        
        return writer.toString();
    }
}
