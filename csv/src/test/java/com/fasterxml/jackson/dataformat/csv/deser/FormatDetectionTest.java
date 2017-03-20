package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class FormatDetectionTest extends ModuleTestBase
{
    public void testSimpleObjectWithHeader() throws IOException
    {
        CsvFactory f = new CsvFactory();
        DataFormatDetector detector = new DataFormatDetector(f);
        byte[] doc = "name,place,town\nBob,home,Denver\n".getBytes("UTF-8");
        DataFormatMatcher matcher = detector.findFormat(doc);
        // should have match
        assertTrue(matcher.hasMatch());
        assertEquals("CSV", matcher.getMatchedFormatName());
        assertEquals(MatchStrength.SOLID_MATCH, matcher.getMatchStrength());
        assertSame(f, matcher.getMatch());

        // and also something that does NOT look like CSV
        doc = "{\"a\":3}".getBytes("UTF-8");
        matcher = detector.findFormat(doc);
        assertFalse(matcher.hasMatch());
    }
}
