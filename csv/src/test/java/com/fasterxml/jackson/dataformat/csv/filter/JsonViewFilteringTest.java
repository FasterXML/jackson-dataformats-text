package com.fasterxml.jackson.dataformat.csv.filter;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.ser.std.SimpleBeanPropertyFilter.FilterExceptFilter;
import com.fasterxml.jackson.databind.ser.std.SimpleFilterProvider;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvReadException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class JsonViewFilteringTest extends ModuleTestBase
{
    // Classes that represent views
    static class ViewA { }
    static class ViewAA extends ViewA { }
    static class ViewB { }
    static class ViewBB extends ViewB { }
    
    @JsonPropertyOrder({ "a", "aa", "b" })
    static class Bean
    {
        @JsonView({ ViewA.class, ViewB.class })
        public String a = "1";

        @JsonView({ViewAA.class })
        public String aa = "2";

        @JsonView(ViewB.class)
        public String b = "3";
    }

    static final String COMPANY_FILTER = "COMPANY_FILTER";

    @JsonPropertyOrder({ "id", "name", "ticker" })
    @JsonFilter(COMPANY_FILTER)
    public static class Company {
        public int id;
        public String name;
        public String ticker;

        Company() { }
        Company(int id, String name, String ticker) {
            this.id = id;
            this.name = name;
            this.ticker = ticker;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();
    
    public void testWithJsonView() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(Bean.class).withLineSeparator("\n").withHeader();
        String actual = MAPPER.writer(schema).withView(ViewB.class).writeValueAsString(new Bean());
//      System.out.println(actual);

        BufferedReader br = new BufferedReader(new StringReader(actual.trim()));
        assertEquals("a,aa,b", br.readLine());
        assertEquals("1,,3", br.readLine());
        assertNull(br.readLine());

        // plus read back?
        final String INPUT = "a,aa,b\n5,6,7\n";
        Bean result = MAPPER.readerFor(Bean.class).with(schema).withView(ViewB.class).readValue(INPUT);
        assertEquals("5", result.a);
        // due to filtering, ought to use default
        assertEquals("2", result.aa);
        assertEquals("7", result.b);
    }

    public void testSchemaWithJsonViewSerialization() throws Exception
    {
        CsvSchema schema = MAPPER.schemaForWithView(Bean.class, ViewB.class).withLineSeparator("\n").withHeader();
        String actual = MAPPER.writer(schema).withView(ViewB.class).writeValueAsString(new Bean());
        MAPPER.writer(schema).withView(ViewB.class);

        BufferedReader br = new BufferedReader(new StringReader(actual.trim()));
        assertEquals("a,b", br.readLine());
        assertEquals("1,3", br.readLine());
        assertNull(br.readLine());
    }

    public void testSchemaWithJsonViewDeserialization() throws Exception
    {
        CsvSchema schema = MAPPER.schemaForWithView(Bean.class, ViewB.class).withLineSeparator("\n").withHeader();
        final String input = "a,b\n5,7\n";
        Bean result = MAPPER.readerFor(Bean.class).with(schema).withView(ViewB.class).readValue(input);

        assertEquals("5", result.a);
        // due to filtering, ought to use default
        assertEquals("2", result.aa);
        assertEquals("7", result.b);
    }

    public void testSchemaWithJsonViewDeserializationFail() throws Exception
    {
        CsvSchema schema = MAPPER.schemaForWithView(Bean.class, ViewB.class).withLineSeparator("\n").withHeader();
        final String input = "a,aa,b\n5,6,7\n";
        try {
            MAPPER.readerFor(Bean.class).with(schema).withView(ViewB.class).readValue(input);
            fail();
        } catch (CsvReadException e) {
            verifyException(e, "Too many entries: expected at most 2");
        }
    }
    
    public void testWithJsonFilter() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(Company.class).withLineSeparator("\n").withHeader();

        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter(COMPANY_FILTER, FilterExceptFilter.filterOutAllExcept("name", "ticker"));

        List<Company> companies = Arrays.asList(
                new Company(1, "name1", "ticker1")
                , new Company(2, "name2", "ticker2")
                , new Company(3, "name3", "ticker3"));
        String actual = MAPPER.writer(filterProvider).with(schema).writeValueAsString(companies);
//        System.out.println(actual);

        BufferedReader br = new BufferedReader(new StringReader(actual.trim()));
        assertEquals("id,name,ticker", br.readLine());
        assertEquals(",name1,ticker1", br.readLine());
        assertEquals(",name2,ticker2", br.readLine());
        assertEquals(",name3,ticker3", br.readLine());
        assertNull(br.readLine());
    }    

    public void testWithJsonFilterFieldSuppressed() throws Exception
    {
        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("name")
                .addColumn("ticker")
                .setLineSeparator("\n").setUseHeader(true)
                .build();

        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter(COMPANY_FILTER, FilterExceptFilter.filterOutAllExcept("name", "ticker"));

        List<Company> companies = Arrays.asList(
                new Company(1, "name1", "ticker1")
                , new Company(2, "name2", "ticker2")
                , new Company(3, "name3", "ticker3"));
        String actual = MAPPER.writer(filterProvider).with(schema).writeValueAsString(companies);

        BufferedReader br = new BufferedReader(new StringReader(actual.trim()));
        assertEquals("name,ticker", br.readLine());
        assertEquals("name1,ticker1", br.readLine());
        assertEquals("name2,ticker2", br.readLine());
        assertEquals("name3,ticker3", br.readLine());
        assertNull(br.readLine());
    }
}
