package tools.jackson.dataformat.csv.deser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.dataformat.csv.*;
import tools.jackson.dataformat.csv.impl.CsvParserBootstrapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for encoding auto-detection performed by {@code impl.CsvParserBootstrapper}
 * when reading from raw byte input: BOM handling plus heuristic detection of
 * fixed-width multi-byte encodings (UTF-16 / UTF-32, big- and little-endian).
 */
public class CsvEncodingDetectionTest extends ModuleTestBase
{
    private final static String CSV = "foo,bar\n";

    private final static byte[] BOM_UTF8 = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private final static byte[] BOM_UTF16BE = { (byte) 0xFE, (byte) 0xFF };
    private final static byte[] BOM_UTF16LE = { (byte) 0xFF, (byte) 0xFE };
    private final static byte[] BOM_UTF32BE = { 0, 0, (byte) 0xFE, (byte) 0xFF };
    private final static byte[] BOM_UTF32LE = { (byte) 0xFF, (byte) 0xFE, 0, 0 };

    private final CsvMapper MAPPER = mapperForCsv();

    /*
    /**********************************************************************
    /* Tests: detection via BOM
    /**********************************************************************
     */

    @Test
    public void testUTF8WithBOM() throws Exception {
        _verifyContents(concat(BOM_UTF8, encode(CSV, "UTF-8")));
    }

    @Test
    public void testUTF16BEWithBOM() throws Exception {
        _verifyContents(concat(BOM_UTF16BE, encode(CSV, "UTF-16BE")));
    }

    @Test
    public void testUTF16LEWithBOM() throws Exception {
        _verifyContents(concat(BOM_UTF16LE, encode(CSV, "UTF-16LE")));
    }

    @Test
    public void testUTF32BEWithBOM() throws Exception {
        _verifyContents(concat(BOM_UTF32BE, encode(CSV, "UTF-32BE")));
    }

    @Test
    public void testUTF32LEWithBOM() throws Exception {
        _verifyContents(concat(BOM_UTF32LE, encode(CSV, "UTF-32LE")));
    }

    /*
    /**********************************************************************
    /* Tests: heuristic detection without BOM (leading ASCII -> null bytes)
    /**********************************************************************
     */

    @Test
    public void testUTF16BENoBOM() throws Exception {
        _verifyContents(encode(CSV, "UTF-16BE"));
    }

    @Test
    public void testUTF16LENoBOM() throws Exception {
        _verifyContents(encode(CSV, "UTF-16LE"));
    }

    @Test
    public void testUTF32BENoBOM() throws Exception {
        _verifyContents(encode(CSV, "UTF-32BE"));
    }

    @Test
    public void testUTF32LENoBOM() throws Exception {
        _verifyContents(encode(CSV, "UTF-32LE"));
    }

    @Test
    public void testUTF8NoBOM() throws Exception {
        _verifyContents(encode(CSV, "UTF-8"));
    }

    // Short (2-byte) input that can only be probed via the ensureLoaded(2) branch
    @Test
    public void testShortUTF16Input() throws Exception {
        // UTF-16BE encoding of a single 'a' -> { 0x00, 0x61 }
        byte[] bytes = { 0x00, 0x61 };
        CsvSchema schema = CsvSchema.builder().addColumn("only").build();
        try (JsonParser p = MAPPER.reader(schema).createParser(bytes)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("a", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /*
    /**********************************************************************
    /* Tests: unsupported ("weird") UCS-4 byte orders should fail
    /**********************************************************************
     */

    @Test
    public void testWeirdUCS4_2143_viaBOM() throws Exception {
        // 0x0000FFFE
        _verifyWeirdUCS4(new byte[] { 0, 0, (byte) 0xFF, (byte) 0xFE });
    }

    @Test
    public void testWeirdUCS4_3412_viaBOM() throws Exception {
        // 0xFEFF0000
        _verifyWeirdUCS4(new byte[] { (byte) 0xFE, (byte) 0xFF, 0, 0 });
    }

    @Test
    public void testWeirdUCS4_3412_viaContent() throws Exception {
        // 0x00??0000 -> "3412" in-order UTF-32
        _verifyWeirdUCS4(new byte[] { 0, 0x41, 0, 0 });
    }

    @Test
    public void testWeirdUCS4_2143_viaContent() throws Exception {
        // 0x0000??00 -> "2143" in-order UTF-32
        _verifyWeirdUCS4(new byte[] { 0, 0, 0x41, 0 });
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _verifyContents(byte[] bytes) throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("name")
                .addColumn("value")
                .build();
        try (JsonParser p = MAPPER.reader(schema).createParser(bytes)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("name", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("foo", p.getString());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("value", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("bar", p.getString());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    private void _verifyWeirdUCS4(byte[] bytes) throws Exception
    {
        CsvParserBootstrapper bs = new CsvParserBootstrapper(_ioContext(),
                bytes, 0, bytes.length);
        try {
            // readCtxt is only used after successful detection, so null is fine here
            bs.constructParser(null, 0, 0, CsvSchema.emptySchema());
            fail("Should not pass with unsupported UCS-4 byte order");
        } catch (JacksonException e) {
            verifyException(e, "Unsupported UCS-4 endianness");
        }
    }

    private IOContext _ioContext() {
        return new IOContext(
                StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }

    private static byte[] encode(String str, String charset) {
        return str.getBytes(Charset.forName(charset));
    }

    private static byte[] concat(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(a);
        bytes.write(b);
        return bytes.toByteArray();
    }
}
