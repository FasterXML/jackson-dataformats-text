package perf;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Simple manual performance micro-benchmark that compares compress and
 * decompress speeds of this LZF implementation with other codecs.
 */
@SuppressWarnings("resource")
public final class ManualPerfComparison
{
    private ObjectMapper jsonMapper;

    private ObjectReader csvReader;

    private ObjectWriter csvWriter;
    
    public ManualPerfComparison()
    {
        jsonMapper = new ObjectMapper();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(RequestEntry.class)
            .withColumnSeparator('|')
            .withUseHeader(true)
            .withSkipFirstDataRow(true)
            ;
        
        csvReader = mapper.readerFor(RequestEntry.class).with(schema);
        csvWriter = mapper.writer(schema);
    }
    
    private RequestEntry[] readCsv(byte[] csvInput) throws IOException
    {
        ArrayList<RequestEntry> entries = new ArrayList<RequestEntry>();
        Iterator<RequestEntry> it = csvReader.readValues(new ByteArrayInputStream(csvInput));
        while (it.hasNext()) {
            entries.add(it.next());
        }
        return entries.toArray(new RequestEntry[entries.size()]);
    }

    private byte[] writeAsJson(RequestEntry[] entries) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(256 + entries.length * 100);
        JsonGenerator jgen = jsonMapper.getFactory().createGenerator(bytes);
        for (RequestEntry entry : entries) {
            jsonMapper.writeValue(jgen, entry);
        }
        jgen.close();
        return bytes.toByteArray();
    }
    
    private void test(byte[] csvInput) throws IOException
    {
        final RequestEntry[] entries = readCsv(csvInput);
        final byte[] jsonInput = writeAsJson(entries);
        
        // Let's try to guestimate suitable size... to get to 10 megs to process
        final int REPS = (int) ((double) (10 * 1000 * 1000) / (double) csvInput.length);

        System.out.printf("Input: %d entries; %d bytes as CSV, %d bytes as JSON\n",
                entries.length, csvInput.length, jsonInput.length);
        System.out.printf("Will do %d repetitions per test.\n\n", REPS);

        int i = 0;
        
        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 4);

// if (true) round = 0;
            
            String msg;
            boolean lf = (round == 0);

            long msecs;
            
            switch (round) {

            case 0:
                msg = "CSV, read";
                msecs = testCsvRead(REPS, csvInput);
                break;
            case 1:
                msg = "CSV, write";
                msecs = testCsvWrite(REPS, entries);
                break;
            case 2:
                msg = "JSON, read";
                msecs = testJsonRead(REPS, jsonInput);
                break;
            case 3:
                msg = "JSON, write";
                msecs = testJsonWrite(REPS, entries);
                break;
            default:
                throw new Error();
            }
            
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+msecs+" msecs");
        }
    }

    private final long testJsonRead(int REPS, byte[] input) throws IOException
    {
        long start = System.currentTimeMillis();
        while (--REPS >= 0) {
            Iterator<RequestEntry> it = jsonMapper.readerFor(RequestEntry.class).readValues(
                    input, 0, input.length);
            while (it.hasNext()) {
                it.next();
            }
        }
        return System.currentTimeMillis() - start;
    }

    private final long testCsvRead(int REPS, byte[] input) throws IOException
    {
        long start = System.currentTimeMillis();
        while (--REPS >= 0) {
            Iterator<RequestEntry> it = csvReader.readValues(input, 0, input.length);
            while (it.hasNext()) {
                it.next();
            }
        }
        return System.currentTimeMillis() - start;
    }
    
    private final long testJsonWrite(int REPS, RequestEntry[] entries) throws IOException
    {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unused")
        int size = 0;
        while (--REPS >= 0) {
            BogusOutputStream bogus = new BogusOutputStream();
            jsonMapper.writeValue(bogus, entries);
            size = bogus.length();
        }
        return System.currentTimeMillis() - start;
    }
    
    private final long testCsvWrite(int REPS, RequestEntry[] entries) throws IOException
    {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unused")
        int size = 0;
        while (--REPS >= 0) {
            BogusOutputStream bogus = new BogusOutputStream();
            csvWriter.writeValue(bogus, entries);
            size = bogus.length();
        }
        return System.currentTimeMillis() - start;
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [file]");
            System.exit(1);
        }
        new ManualPerfComparison().test(readAll(args[0]));
    }

    public static byte[] readAll(String filename) throws IOException
    {
        File f = new File(filename);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) f.length());
        byte[] buffer = new byte[4000];
        int count;
        FileInputStream in = new FileInputStream(f);
        
        while ((count = in.read(buffer)) > 0) {
            bytes.write(buffer, 0, count);
        }
        in.close();
        return bytes.toByteArray();
    }
}
