package perf;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Manual test for checking how fast a F-5500 file
 * (from http:///www.dol.gov/) can be read from a file
 */
public final class F5500Reader
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java .... [input file]");
            System.exit(1);
        }
        new F5500Reader().read(new File(args[0]));
    }

    private void read(File inputFile) throws IOException, InterruptedException
    {
        int x = 1;
        while (true) {
            Class<?> cls = ((x & 1) == 0) ? Map.class : F5500Entry.class;
            ++x;
            long now = System.currentTimeMillis();
            int count = readAll(inputFile, cls);
            long time = System.currentTimeMillis() - now;
            System.out.printf("DONE! Read %d rows as %s in %.1f seconds.\n",
                    count, cls.getName(), time / 1000.0);
            Thread.sleep(500L); 
        }
    }

    private <T> int readAll(File inputFile, Class<T> cls) throws IOException
    {
        System.out.print("Reading input as "+cls.getName()+" instances: ");
        
        int count = 0;
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.builder()
            .setUseHeader(true)
            .build();

        MappingIterator<T> it = mapper.readerFor(cls)
            .with(schema).readValues(inputFile);
        while (it.hasNext()) {
            @SuppressWarnings("unused")
            T row = it.nextValue();
            ++count;
            if ((count & 0x3FFF) == 0) {
                System.out.print('.');
            }
        }
        System.out.println();
        it.close();
        return count;
    }
}
