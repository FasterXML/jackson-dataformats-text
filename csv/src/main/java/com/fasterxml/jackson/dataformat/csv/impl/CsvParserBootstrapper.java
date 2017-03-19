package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.MergedStream;
import com.fasterxml.jackson.core.io.UTF32Reader;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

/**
 * This class is used to determine the encoding of byte stream
 * that is to contain CSV document.
 * Since there is no real specification for how this should work
 * with CSV, it will be based on rules used with JSON (which themselves
 * are similar to those used with XML); main points are to check for
 * BOM first, then look for multi-byted fixed-length encodings
 * (UTF-16, UTF-32). And finally, if neither found, must decide
 * between most likely alternatives, UTF-8 and Latin-1.
 */
public final class CsvParserBootstrapper
{
    final static byte UTF8_BOM_1 = (byte) 0xEF;
    final static byte UTF8_BOM_2 = (byte) 0xBB;
    final static byte UTF8_BOM_3 = (byte) 0xBF;

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected final IOContext _context;

    protected final ObjectCodec _codec;

    /*
    /**********************************************************
    /* Input buffering
    /**********************************************************
     */
    
    protected final InputStream _in;

    protected final byte[] _inputBuffer;

    private int _inputPtr;

    private int _inputEnd;

    /*
    /**********************************************************
    /* Input location
    /**********************************************************
     */

    /**
     * Current number of input units (bytes or chars) that were processed in
     * previous blocks,
     * before contents of current input buffer.
     *<p>
     * Note: includes possible BOMs, if those were part of the input.
     */
    protected int _inputProcessed;

    /*
    /**********************************************************
    /* Data gathered
    /**********************************************************
     */

    protected boolean _bigEndian = true;

    protected int _bytesPerChar = 0; // 0 means "dunno yet"

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public CsvParserBootstrapper(IOContext ctxt, ObjectCodec codec, InputStream in)
    {
        _context = ctxt;
        _codec = codec;
        _in = in;
        _inputBuffer = ctxt.allocReadIOBuffer();
        _inputEnd = _inputPtr = 0;
        _inputProcessed = 0;
    }

    public CsvParserBootstrapper(IOContext ctxt, ObjectCodec codec,
            byte[] inputBuffer, int inputStart, int inputLen)
    {
        _context = ctxt;
        _codec = codec;
        _in = null;
        _inputBuffer = inputBuffer;
        _inputPtr = inputStart;
        _inputEnd = (inputStart + inputLen);
        // Need to offset this for correct location info
        _inputProcessed = -inputStart;
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public CsvParser constructParser(int baseFeatures, int csvFeatures) throws IOException
    {
        boolean foundEncoding = false;

        // First things first: BOM handling
        if (ensureLoaded(4)) {
            int quad =  (_inputBuffer[_inputPtr] << 24)
                | ((_inputBuffer[_inputPtr+1] & 0xFF) << 16)
                | ((_inputBuffer[_inputPtr+2] & 0xFF) << 8)
                | (_inputBuffer[_inputPtr+3] & 0xFF);
            
            if (handleBOM(quad)) {
                foundEncoding = true;
            } else {
                /* If no BOM, let's see if it's a fixed-width multi-byte
                 * (since we can be fairly certain no CSV document should
                 * start with null bytes otherwise...)
                 */
                // UTF-32?
                if (checkUTF32(quad)) {
                    foundEncoding = true;
                } else if (checkUTF16(quad >>> 16)) {
                    foundEncoding = true;
                }
            }
        } else if (ensureLoaded(2)) {
            int i16 = ((_inputBuffer[_inputPtr] & 0xFF) << 8)
                | (_inputBuffer[_inputPtr+1] & 0xFF);
            if (checkUTF16(i16)) {
                foundEncoding = true;
            }
        }

        JsonEncoding enc;

        /* Not found yet? As per specs, this means it must be UTF-8. */
        if (!foundEncoding || _bytesPerChar == 1) {
            enc = JsonEncoding.UTF8;
        } else if (_bytesPerChar == 2) {
            enc = _bigEndian ? JsonEncoding.UTF16_BE : JsonEncoding.UTF16_LE;
        } else if (_bytesPerChar == 4) {
            enc = _bigEndian ? JsonEncoding.UTF32_BE : JsonEncoding.UTF32_LE;
        } else {
            throw new RuntimeException("Internal error"); // should never get here
        }
        _context.setEncoding(enc);
        return new CsvParser((CsvIOContext) _context, baseFeatures, csvFeatures, _codec,
                _createReader(enc));
    }
    
    @SuppressWarnings("resource")
    private Reader _createReader(JsonEncoding enc) throws IOException
    {
        switch (enc) { 
        case UTF32_BE:
        case UTF32_LE:
            return new UTF32Reader(_context, _in, _inputBuffer, _inputPtr, _inputEnd,
                                   enc.isBigEndian());

        case UTF16_BE:
        case UTF16_LE:
            {
                // First: do we have a Stream? If not, need to create one:
                InputStream in = _in;
                
                if (in == null) {
                    in = new ByteArrayInputStream(_inputBuffer, _inputPtr, _inputEnd);
                } else {
                    /* Also, if we have any read but unused input (usually true),
                     * need to merge that input in:
                     */
                    if (_inputPtr < _inputEnd) {
                        in = new MergedStream(_context, in, _inputBuffer, _inputPtr, _inputEnd);
                    }
                }
                return new InputStreamReader(in, enc.getJavaName());
            }
        case UTF8:
            // Important: do not pass context, if we got byte[], nothing to release
            return new UTF8Reader((_in == null) ? null : _context, _in, _context.isResourceManaged(),
                    _inputBuffer, _inputPtr, _inputEnd - _inputPtr);
        default:
            throw new RuntimeException();
        }
    }
    
    /*
    /**********************************************************
    /*  Encoding detection for data format auto-detection
    /**********************************************************
     */

    /**
     * Current implementation is not as thorough as one used by
     * other data formats like JSON.
     * But it should work, for now, and can
     * be improved as necessary.
     */
    public static MatchStrength hasCSVFormat(InputAccessor acc,
            int quoteChar, char separatorChar) throws IOException
    {
        // No really good heuristics for CSV, since value starts with either
        // double-quote, or alpha-num, but can also be preceded by white space...
        
        if (!acc.hasMoreBytes()) {
            return MatchStrength.INCONCLUSIVE;
        }
        byte b = acc.nextByte();
        // Very first thing, a UTF-8 BOM?
        if (b == UTF8_BOM_1) { // yes, looks like UTF-8 BOM
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_2) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_3) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = acc.nextByte();
        }
        // Then possible leading space
        int ch = skipSpace(acc, b);
        if (ch < 0) { // end of input? Unlikely but...
            return MatchStrength.INCONCLUSIVE;
        }
        // Control character? Not very good either
        if (ch < 32) {
            return MatchStrength.NO_MATCH;
        }
        
        // But seeing a quote char is actually reasonable match
        if (ch == quoteChar) {
            return MatchStrength.SOLID_MATCH;
        }
        // and separator at least weak
        if (ch == separatorChar) {
            return MatchStrength.WEAK_MATCH;
        }
        /* otherwise, well, almost anything could in theory do it; 
         * let's trust other format detectors to find positive cases
         */
        // Let's consider letters, numbers to suggest a good match
        if (Character.isDigit(ch) || Character.isAlphabetic(ch)) {
            return MatchStrength.SOLID_MATCH;
        }
        return MatchStrength.INCONCLUSIVE;
    }

    private final static int skipSpace(InputAccessor acc, byte b) throws IOException
    {
        while (true) {
            int ch = b & 0xFF;
            if (!(ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t')) {
                return ch;
            }
            if (!acc.hasMoreBytes()) {
                return -1;
            }
            b = acc.nextByte();
            ch = b & 0xFF;
        }
    }
    
    /*
    /**********************************************************
    /* Internal methods, parsing
    /**********************************************************
     */

    /**
     * @return True if a BOM was succesfully found, and encoding
     *   thereby recognized.
     */
    private boolean handleBOM(int quad) throws IOException
    {
        /* Handling of (usually) optional BOM (required for
         * multi-byte formats); first 32-bit charsets:
         */
        switch (quad) {
        case 0x0000FEFF:
            _bigEndian = true;
            _inputPtr += 4;
            _bytesPerChar = 4;
            return true;
        case 0xFFFE0000: // UCS-4, LE?
            _inputPtr += 4;
            _bytesPerChar = 4;
            _bigEndian = false;
            return true;
        case 0x0000FFFE: // UCS-4, in-order...
            reportWeirdUCS4("2143"); // throws exception
        case 0xFEFF0000: // UCS-4, in-order...
            reportWeirdUCS4("3412"); // throws exception
        }
        // Ok, if not, how about 16-bit encoding BOMs?
        int msw = quad >>> 16;
        if (msw == 0xFEFF) { // UTF-16, BE
            _inputPtr += 2;
            _bytesPerChar = 2;
            _bigEndian = true;
            return true;
        }
        if (msw == 0xFFFE) { // UTF-16, LE
            _inputPtr += 2;
            _bytesPerChar = 2;
            _bigEndian = false;
            return true;
        }
        // And if not, then UTF-8 BOM?
        if ((quad >>> 8) == 0xEFBBBF) { // UTF-8
            _inputPtr += 3;
            _bytesPerChar = 1;
            _bigEndian = true; // doesn't really matter
            return true;
        }
        return false;
    }

    private boolean checkUTF32(int quad) throws IOException
    {
        /* Handling of (usually) optional BOM (required for
         * multi-byte formats); first 32-bit charsets:
         */
        if ((quad >> 8) == 0) { // 0x000000?? -> UTF32-BE
            _bigEndian = true;
        } else if ((quad & 0x00FFFFFF) == 0) { // 0x??000000 -> UTF32-LE
            _bigEndian = false;
        } else if ((quad & ~0x00FF0000) == 0) { // 0x00??0000 -> UTF32-in-order
            reportWeirdUCS4("3412");
        } else if ((quad & ~0x0000FF00) == 0) { // 0x0000??00 -> UTF32-in-order
            reportWeirdUCS4("2143");
        } else {
            // Can not be valid UTF-32 encoded JSON...
            return false;
        }
        // Not BOM (just regular content), nothing to skip past:
        //_inputPtr += 4;
        _bytesPerChar = 4;
        return true;
    }

    private boolean checkUTF16(int i16)
    {
        if ((i16 & 0xFF00) == 0) { // UTF-16BE
            _bigEndian = true;
        } else if ((i16 & 0x00FF) == 0) { // UTF-16LE
            _bigEndian = false;
        } else { // nope, not  UTF-16
            return false;
        }
        // Not BOM (just regular content), nothing to skip past:
        //_inputPtr += 2;
        _bytesPerChar = 2;
        return true;
    }

    /*
    /**********************************************************
    /* Internal methods, problem reporting
    /**********************************************************
     */

    private void reportWeirdUCS4(String type) throws IOException {
        throw new CharConversionException("Unsupported UCS-4 endianness ("+type+") detected");
    }

    /*
    /**********************************************************
    /* Internal methods, raw input access
    /**********************************************************
     */

    protected boolean ensureLoaded(int minimum) throws IOException
    {
        /* Let's assume here buffer has enough room -- this will always
         * be true for the limited used this method gets
         */
        int gotten = (_inputEnd - _inputPtr);
        while (gotten < minimum) {
            int count;

            if (_in == null) { // block source
                count = -1;
            } else {
                count = _in.read(_inputBuffer, _inputEnd, _inputBuffer.length - _inputEnd);
            }
            if (count < 1) {
                return false;
            }
            _inputEnd += count;
            gotten += count;
        }
        return true;
    }
}

