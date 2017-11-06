package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Tests to try to see if Unicode writing, reading work as expected.
 */
public class UnicodeWritingTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    final CsvMapper MAPPER = mapperForCsv();

    public void testSimpleStringSequence() throws Exception
    {
        // 16-Mar-2017, tatu: Actually, this assumes that read/write defaults are the same,
        //    and for real Unicode support, UTF-8 (or UTF-16). Whereas many CSV impls assume
        //    Latin-1 default. Oh well. We'll go UTF-8 unless punched in the face.
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SequenceWriter w = MAPPER.writer()
                .writeValues(bytes);
        // turns out this writes 2 columns... fine
        final String STRING1 = "R\u00F6ck!";
        final String STRING2 = "Smile (\u263A)...";
        w.write(STRING1);
        w.write(STRING2);
        w.close();
        byte[] b = bytes.toByteArray();

        String[] stuff = MAPPER.readerFor(String[].class)
                .readValue(b);
        assertEquals(2, stuff.length);
        assertEquals(STRING1, stuff[0]);
        assertEquals(STRING2, stuff[1]);
    }
}
