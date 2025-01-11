package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.format.*;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class FormatDetectionTest extends ModuleTestBase
{
    @Test
    public void testSimpleObjectWithHeader() throws IOException
    {
        CsvFactory f = new CsvFactory();
        DataFormatDetector detector = new DataFormatDetector(f);
        byte[] doc = utf8("name,place,town\nBob,home,Denver\n");
        DataFormatMatcher matcher = detector.findFormat(doc);
        // should have match
        assertTrue(matcher.hasMatch());
        assertEquals("CSV", matcher.getMatchedFormatName());
        assertEquals(MatchStrength.SOLID_MATCH, matcher.getMatchStrength());
        assertSame(f, matcher.getMatch());

        // and also something that does NOT look like CSV
        doc = utf8("{\"a\":3}");
        matcher = detector.findFormat(doc);
        assertFalse(matcher.hasMatch());
    }
}
