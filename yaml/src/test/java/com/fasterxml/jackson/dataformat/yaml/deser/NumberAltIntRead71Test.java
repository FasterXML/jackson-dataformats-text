package com.fasterxml.jackson.dataformat.yaml.deser;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#71]: hex numbers
// [dataformats-text#233]: also binary, octal (with/without underscores)
public class NumberAltIntRead71Test extends ModuleTestBase
{
    static class IntHolder {
        public int value;
    }

    static class LongHolder {
        public Number value;
    }

    static class BigHolder {
        public Number value;
    }

    private final ObjectMapper MAPPER = newObjectMapper();
    
    // [dataformats-text#71]
    @Test
    public void testDeserHexInt71() throws Exception
    {
        _verifyNumber(Integer.parseInt("48", 16), "0x48");
        _verifyNumber(Integer.parseInt("48", 16), "+0x48");
        _verifyNumber(-Integer.parseInt("48", 16), "-0x48");

        _verifyNumber(Long.parseLong("12345678E0", 16), "0x12345678E0");
        _verifyNumber(-Long.parseLong("12345678c0", 16), "-0x12345678c0");

        _verifyNumber(new BigInteger("11112222333344445555abcd", 16), "0x11112222333344445555ABCD");
        _verifyNumber(new BigInteger("-11112222333344445555ACDC", 16), "-0x11112222333344445555acdc");
    }

    @Test
    public void testDeserHexUnderscores() throws Exception
    {
        _verifyNumber(Integer.parseInt("1F3", 16), "0x01_F3");
        _verifyNumber(Integer.parseInt("1F3", 16), "+0x01_F3");
        _verifyNumber(-Integer.parseInt("1F3", 16), "-0x01_F3");

        _verifyNumber(Long.parseLong("12345678E0", 16), "0x12_3456_78E0");
        _verifyNumber(-Long.parseLong("12345678c0", 16), "-0x12_3456_78c0");
    }

    @Test
    public void testDeserOctal() throws Exception
    {
        _verifyNumber(Integer.parseInt("24", 8), "024");
        _verifyNumber(Integer.parseInt("24", 8), "+024");
        _verifyNumber(-Integer.parseInt("24", 8), "-024");

        _verifyNumber(Long.parseLong("1234567712345677", 8), "01234567712345677");
        _verifyNumber(-Long.parseLong("1234567712345677", 8), "-01234567712345677");

        _verifyNumber(new BigInteger("123456771234567712345677", 8), "0123456771234567712345677");
        _verifyNumber(new BigInteger("-123456771234567712345677", 8), "-0123456771234567712345677");
    }

    @Test
    public void testDeserOctalUnderscores() throws Exception
    {
        _verifyNumber(Integer.parseInt("24", 8), "0_24");
        _verifyNumber(Integer.parseInt("24", 8), "+0_24");
        _verifyNumber(-Integer.parseInt("24", 8), "-0_24");

        _verifyNumber(Long.parseLong("1234567712345677", 8), "01_234_567_712_345_677");
        _verifyNumber(-Long.parseLong("1234567712345677", 8), "-01_234_567_712_345_677");
    }

    @Test
    public void testDeserBinary() throws Exception
    {
        _verifyNumber(Integer.parseInt("1010", 2), "0b1010");
        _verifyNumber(Integer.parseInt("1010", 2), "+0b1010");
        _verifyNumber(-Integer.parseInt("1010", 2), "-0b1010");
    }

    @Test
    public void testDeserBinaryUnderscores() throws Exception
    {
        _verifyNumber(Integer.parseInt("1010", 2), "0b10_10");
        _verifyNumber(Integer.parseInt("1010", 2), "+0b10_10");
        _verifyNumber(-Integer.parseInt("1010", 2), "-0b10_10");
    }

    // 23-Nov-2020, tatu: Decided not to add support for 60-based, at
    //    least not yet, due to likely backwards-compatibility issues
    //    with IP numbers
    /*
    @Test
    public void testDeserBase60() throws Exception
    {
        IntHolder result = MAPPER.readerFor(IntHolder.class)
                .readValue("value: 190:20:30");
        assertEquals((190 * 60 * 60) + (20 * 60) + 30, result.value);
    }
    */

    private void _verifyNumber(int expValue, String asString) throws Exception
    {
        IntHolder result = MAPPER.readerFor(IntHolder.class)
                .readValue("value: "+asString+"\n");
        assertEquals(expValue, result.value);
    }

    private void _verifyNumber(long expValue, String asString) throws Exception
    {
        LongHolder result = MAPPER.readerFor(LongHolder.class)
                .readValue("value: "+asString+"\n");
        assertEquals(Long.class, result.value.getClass());
        assertEquals(expValue, result.value.longValue());
    }

    private void _verifyNumber(BigInteger expValue, String asString) throws Exception
    {
        BigHolder result = MAPPER.readerFor(BigHolder.class)
                .readValue("value: "+asString+"\n");
        assertEquals(BigInteger.class, result.value.getClass());
        assertEquals(expValue, result.value);
    }
}
