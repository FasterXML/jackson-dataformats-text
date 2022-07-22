package tools.jackson.dataformat.csv.impl;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.MergedStream;
import tools.jackson.core.io.UTF32Reader;
import tools.jackson.dataformat.csv.CsvParser;
import tools.jackson.dataformat.csv.CsvSchema;

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
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final IOContext _context;

    /*
    /**********************************************************************
    /* Input buffering
    /**********************************************************************
     */
    
    protected final InputStream _in;

    protected final byte[] _inputBuffer;

    private int _inputPtr;

    private int _inputEnd;

    /*
    /**********************************************************************
    /* Input location
    /**********************************************************************
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
    /**********************************************************************
    /* Data gathered
    /**********************************************************************
     */

    protected boolean _bigEndian = true;

    protected int _bytesPerChar = 0; // 0 means "dunno yet"

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CsvParserBootstrapper(IOContext ctxt, InputStream in)
    {
        _context = ctxt;
        _in = in;
        _inputBuffer = ctxt.allocReadIOBuffer();
        _inputEnd = _inputPtr = 0;
        _inputProcessed = 0;
    }

    public CsvParserBootstrapper(IOContext ctxt,
            byte[] inputBuffer, int inputStart, int inputLen)
    {
        _context = ctxt;
        _in = null;
        _inputBuffer = inputBuffer;
        _inputPtr = inputStart;
        _inputEnd = (inputStart + inputLen);
        // Need to offset this for correct location info
        _inputProcessed = -inputStart;
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public CsvParser constructParser(ObjectReadContext readCtxt,
            int parserFeatures, int csvFeatures,
            CsvSchema schema)
        throws JacksonException
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
        final boolean autoClose = _context.isResourceManaged()
            || StreamReadFeature.AUTO_CLOSE_SOURCE.enabledIn(parserFeatures);
        return new CsvParser(readCtxt, (CsvIOContext) _context,
                parserFeatures, csvFeatures, schema,
                _createReader(enc, autoClose));
    }

    @SuppressWarnings("resource")
    private Reader _createReader(JsonEncoding enc, boolean autoClose)
        throws JacksonException
    {
        switch (enc) { 
        case UTF32_BE:
        case UTF32_LE:
            return new UTF32Reader(_context, _in, autoClose,
                    _inputBuffer, _inputPtr, _inputEnd, enc.isBigEndian());

        case UTF16_BE:
        case UTF16_LE:
            {
                // First: do we have a Stream? If not, need to create one:
                InputStream in = _in;
                
                if (in == null) {
                    in = new ByteArrayInputStream(_inputBuffer, _inputPtr, _inputEnd);
                } else {
                    // Also, if we have any read but unused input (usually true),
                    // need to merge that input in:
                    if (_inputPtr < _inputEnd) {
                        in = new MergedStream(_context, in, _inputBuffer, _inputPtr, _inputEnd);
                    }
                }
                try {
                    return new InputStreamReader(in, enc.getJavaName());
                } catch (IOException e) {
                    throw _wrapIOFailure(e);
                }
            }
        case UTF8:
            // Important: do not pass context, if we got byte[], nothing to release
            return new UTF8Reader((_in == null) ? null : _context, _in, autoClose,
                    _inputBuffer, _inputPtr, _inputEnd - _inputPtr);
        default:
            throw new RuntimeException();
        }
    }

    /*
    /**********************************************************************
    /* Internal methods, parsing
    /**********************************************************************
     */

    /**
     * @return True if a BOM was successfully found, and encoding
     *   thereby recognized.
     */
    private boolean handleBOM(int quad) throws JacksonException
    {
        // Handling of (usually) optional BOM (required for
        // multi-byte formats); first 32-bit charsets:
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

    private boolean checkUTF32(int quad) throws JacksonException
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
    /**********************************************************************
    /* Internal methods, problem reporting
    /**********************************************************************
     */

    private void reportWeirdUCS4(String type) throws JacksonException {
        throw _createIOFailure("Unsupported UCS-4 endianness ("+type+") detected");
    }

    /*
    /**********************************************************************
    /* Internal methods, raw input access
    /**********************************************************************
     */

    protected boolean ensureLoaded(int minimum) throws JacksonException
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
                try {
                    count = _in.read(_inputBuffer, _inputEnd, _inputBuffer.length - _inputEnd);
                } catch (IOException e) {
                    throw _wrapIOFailure(e);
                }
            }
            if (count < 1) {
                return false;
            }
            _inputEnd += count;
            gotten += count;
        }
        return true;
    }

    /*
    /**********************************************************************
    /* Internal methods, exception handling
    /**********************************************************************
     */

    private static JacksonException _createIOFailure(String msg) throws JacksonException {
        // 12-Jan-2021, tatu: Couple of alternatives, but since this is before
        //    actual parser created, seems best to simply fake this was "true"
        //    IOException
        return _wrapIOFailure(new IOException(msg));
    }

    private static JacksonException _wrapIOFailure(IOException e) throws JacksonException {
        return WrappedIOException.construct(e);
    }
}

