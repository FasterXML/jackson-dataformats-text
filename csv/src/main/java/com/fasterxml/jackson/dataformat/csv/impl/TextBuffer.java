package com.fasterxml.jackson.dataformat.csv.impl;

import java.math.BigDecimal;
import java.util.LinkedList;

import com.fasterxml.jackson.core.util.BufferRecycler;

// NOTE: copied and slightly modified from Jackson code version
/**
 * Helper class for efficiently aggregating parsed and decoded
 * textual content
 *
 */
public final class TextBuffer
{
    final static char[] NO_CHARS = new char[0];

    final static int MIN_SEGMENT_LEN = 1000;
    
    final static int MAX_SEGMENT_LEN = 0x40000; // 256k
    
    /*
    /**********************************************************
    /* Configuration:
    /**********************************************************
     */

    // thing we can borrow char array from, return...
    private final BufferRecycler _allocator;

    /*
    /**********************************************************
    /* Shared input buffers
    /**********************************************************
     */

    /**
     * Shared input buffer; stored here in case some input can be returned
     * as is, without being copied to collector's own buffers.
     */
    private char[] _inputBuffer;

    /**
     * Character offset of first char in input buffer; -1 to indicate
     * that input buffer currently does not contain any useful char data
     */
    private int _inputStart;

    private int _inputLen;

    /*
    /**********************************************************
    /* Aggregation segments (when not using input buf)
    /**********************************************************
     */

    /**
     * List of segments prior to currently active segment.
     */
    private LinkedList<char[]> _segments;

    /**
     * Flag that indicates whether _seqments is non-empty
     */
    private boolean _hasSegments = false;

    // // // Currently used segment; not (yet) contained in _seqments

    /**
     * Amount of characters in segments in {@link _segments}
     */
    private int _segmentSize;

    private char[] _currentSegment;

    /**
     * Number of characters in currently active (last) segment
     */
    private int _currentSize;

    /*
    /**********************************************************
    /* Caching of results
    /**********************************************************
     */

    /**
     * String that will be constructed when the whole contents are
     * needed; will be temporarily stored in case asked for again.
     */
    private String _resultString;

    private char[] _resultArray;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public TextBuffer(BufferRecycler allocator)
    {
        _allocator = allocator;
    }

    public void releaseBuffers()
    {
        if (_allocator == null) {
            reset();
        } else {
            if (_currentSegment != null) {
                // First, let's get rid of all but the largest char array
                reset();
                // And then return that array
                char[] buf = _currentSegment;
                _currentSegment = null;
                _allocator.releaseCharBuffer(BufferRecycler.CHAR_TEXT_BUFFER, buf);
            }
        }
    }

    public void reset()
    {
        _inputStart = -1; // indicates shared buffer not used
        _currentSize = 0;
        _inputLen = 0;

        _inputBuffer = null;
        _resultString = null;
        _resultArray = null;

        // And then reset internal input buffers, if necessary:
        if (_hasSegments) {
            clearSegments();
        }
    }

    public void resetWithString(String value)
    {
        _inputBuffer = null;
        _inputStart = -1;
        _inputLen = 0;

        _resultString = value;
        _resultArray = null;

        if (_hasSegments) {
            clearSegments();
        }
        _currentSize = 0;
    }
    
    /**
     * Helper method used to find a buffer to use, ideally one
     * recycled earlier.
     */
    private final char[] findBuffer(int needed)
    {
        if (_allocator != null) {
            return _allocator.allocCharBuffer(BufferRecycler.CHAR_TEXT_BUFFER, needed);
        }
        return new char[Math.max(needed, MIN_SEGMENT_LEN)];
    }

    private final void clearSegments()
    {
        _hasSegments = false;
        _segments.clear();
        _currentSize = _segmentSize = 0;
    }

    /*
    /**********************************************************
    /* Accessors for implementing public interface
    /**********************************************************
     */

    /**
     * @return Number of characters currently stored by this collector
     */
    public int size() {
        if (_inputStart >= 0) { // shared copy from input buf
            return _inputLen;
        }
        if (_resultArray != null) {
            return _resultArray.length;
        }
        if (_resultString != null) {
            return _resultString.length();
        }
        // local segmented buffers
        return _segmentSize + _currentSize;
    }

    public int getTextOffset()
    {
        // Only shared input buffer can have non-zero offset; buffer
        // segments start at 0, and if we have to create a combo buffer,
        // that too will start from beginning of the buffer
        return (_inputStart >= 0) ? _inputStart : 0;
    }

    public boolean hasTextAsCharacters()
    {
        // if we have array in some form, sure
        if (_inputStart >= 0 || _resultArray != null) {
            return true;
        }
        // not if we have String as value
        if (_resultString != null) {
            return false;
        }
        return true;
    }
    
    public char[] getTextBuffer()
    {
        // Are we just using shared input buffer?
        if (_inputStart >= 0) {
            return _inputBuffer;
        }
        if (_resultArray != null) {
            return _resultArray;
        }
        if (_resultString != null) {
            return (_resultArray = _resultString.toCharArray());
        }
        // Nope; but does it fit in just one segment?
        if (!_hasSegments) {
            return _currentSegment;
        }
        // Nope, need to have/create a non-segmented array and return it
        return contentsAsArray();
    }

    /*
    /**********************************************************
    /* Other accessors:
    /**********************************************************
     */

    public String contentsAsString()
    {
        if (_resultString == null) {
            // Has array been requested? Can make a shortcut, if so:
            if (_resultArray != null) {
                _resultString = new String(_resultArray);
            } else {
                // Do we use shared array?
                if (_inputStart >= 0) {
                    if (_inputLen < 1) {
                        return (_resultString = "");
                    }
                    _resultString = new String(_inputBuffer, _inputStart, _inputLen);
                } else { // nope... need to copy
                    // But first, let's see if we have just one buffer
                    int segLen = _segmentSize;
                    int currLen = _currentSize;
                    
                    if (segLen == 0) { // yup
                        _resultString = (currLen == 0) ? "" : new String(_currentSegment, 0, currLen);
                    } else { // no, need to combine
                        StringBuilder sb = new StringBuilder(segLen + currLen);
                        // First stored segments
                        if (_segments != null) {
                            for (char[] curr : _segments) {
                                sb.append(curr, 0, curr.length);
                            }
                        }
                        // And finally, current segment:
                        sb.append(_currentSegment, 0, _currentSize);
                        _resultString = sb.toString();
                    }
                }
            }
        }
        return _resultString;
    }
 
    public char[] contentsAsArray()
    {
        char[] result = _resultArray;
        if (result == null) {
            _resultArray = result = buildResultArray();
        }
        return result;
    }

    /**
     * Convenience method for converting contents of the buffer
     * into a {@link BigDecimal}.
     */
    public BigDecimal contentsAsDecimal()
        throws NumberFormatException
    {
        // Already got a pre-cut array?
        if (_resultArray != null) {
            return new BigDecimal(_resultArray);
        }
        // Or a shared buffer?
        if (_inputStart >= 0) {
            return new BigDecimal(_inputBuffer, _inputStart, _inputLen);
        }
        // Or if not, just a single buffer (the usual case)
        if (_segmentSize == 0) {
            return new BigDecimal(_currentSegment, 0, _currentSize);
        }
        // If not, let's just get it aggregated...
        return new BigDecimal(contentsAsArray());
    }

    /**
     * Convenience method for converting contents of the buffer
     * into a Double value.
     */
    public double contentsAsDouble()
        throws NumberFormatException
    {
        return NumberInput.parseDouble(contentsAsString());
    }

    public boolean looksLikeInt() {
        final char[] ch = contentsAsArray();
        final int len = ch.length;

        if (len == 0) {
            return false;
        }
        
        int i = 0;
        char c = ch[0];
        if (c == '-' || c == '+') {
            if (len == 1) {
                return false;
            }
            ++i;
        }
        for (; i < len; ++i) {
            c = ch[i];
            if (c > '9' || c < '0') {
                return false;
            }
        }
        return true;
    }

    /*
    /**********************************************************
    /* Public mutators:
    /**********************************************************
     */

    /**
     * Method called to make sure that buffer is not using shared input
     * buffer; if it is, it will copy such contents to private buffer.
     */
    public void ensureNotShared() {
        if (_inputStart >= 0) {
            unshare(16);
        }
    }

    /*
    /**********************************************************
    /* Raw access, for high-performance use:
    /**********************************************************
     */

    public char[] getCurrentSegment()
    {
        // Since the intention of the caller is to directly add stuff into
        // buffers, we should NOT have anything in shared buffer... ie. may
        if (_inputStart >= 0) {
            unshare(1);
        } else {
            char[] curr = _currentSegment;
            if (curr == null) {
                _currentSegment = findBuffer(0);
            } else if (_currentSize >= curr.length) {
                // Plus, we better have room for at least one more char
                expand(1);
            }
        }
        return _currentSegment;
    }

    public final char[] emptyAndGetCurrentSegment()
    {
        // inlined 'resetWithEmpty()'
        _inputStart = -1; // indicates shared buffer not used
        _currentSize = 0;
        _inputLen = 0;

        _inputBuffer = null;
        _resultString = null;
        _resultArray = null;

        // And then reset internal input buffers, if necessary:
        if (_hasSegments) {
            clearSegments();
        }
        char[] curr = _currentSegment;
        if (curr == null) {
            _currentSegment = curr = findBuffer(0);
        }
        return curr;
    }

    public int getCurrentSegmentSize() {
        return _currentSize;
    }

    /**
     * @param lastSegmentEnd End offset in the currently active segment,
     *    could be 0 in the case of first character is
     *    delimiter or end-of-line
     * @param trimTrailingSpaces Whether trailing spaces should be trimmed or not
     */
    public String finishAndReturn(int lastSegmentEnd, boolean trimTrailingSpaces)
    {
        if (trimTrailingSpaces) {
            // First, see if it's enough to trim end of current segment:
            int ptr = lastSegmentEnd - 1;
            if (ptr < 0 || _currentSegment[ptr] <= 0x0020) {
                return _doTrim(ptr);
            }
        }
        _currentSize = lastSegmentEnd;
        return contentsAsString();
    }

    private String _doTrim(int ptr)
    {
        while (true) {
            final char[] curr = _currentSegment;
            while (--ptr >= 0) {
                if (curr[ptr] > 0x0020) { // found the ending non-space char, all done:
                    _currentSize = ptr+1;
                    return contentsAsString();
                }
            }
            // nope: need to handle previous segment; if there is one:
            if (_segments == null || _segments.isEmpty()) {
                break;
            }
            _currentSegment = _segments.removeLast();
            ptr = _currentSegment.length;
        }
        // we get here if everything was trimmed, so:
        _currentSize = 0;
        _hasSegments = false;
        return contentsAsString();
    }
    
    /*
    public void finish(int lastSegmentEnd, boolean trimTrailingSpaces)
    {
        _currentSize = lastSegmentEnd;
        if (trimTrailingSpaces) {
            // !!! TODO
        }
    }
    */
    
    public char[] finishCurrentSegment()
    {
        if (_segments == null) {
            _segments = new LinkedList<char[]>();
        }
        _hasSegments = true;
        _segments.add(_currentSegment);
        int oldLen = _currentSegment.length;
        _segmentSize += oldLen;
        // Let's grow segments by 50%
        int newLen = Math.min(oldLen + (oldLen >> 1), MAX_SEGMENT_LEN);
        char[] curr = _charArray(newLen);
        _currentSize = 0;
        _currentSegment = curr;
        return curr;
    }

    /*
    /**********************************************************
    /* Internal methods:
    /**********************************************************
     */

    /**
     * Method called if/when we need to append content when we have been
     * initialized to use shared buffer.
     */
    private void unshare(int needExtra)
    {
        int sharedLen = _inputLen;
        _inputLen = 0;
        char[] inputBuf = _inputBuffer;
        _inputBuffer = null;
        int start = _inputStart;
        _inputStart = -1;

        // Is buffer big enough, or do we need to reallocate?
        int needed = sharedLen+needExtra;
        if (_currentSegment == null || needed > _currentSegment.length) {
            _currentSegment = findBuffer(needed);
        }
        if (sharedLen > 0) {
            System.arraycopy(inputBuf, start, _currentSegment, 0, sharedLen);
        }
        _segmentSize = 0;
        _currentSize = sharedLen;
    }

    private void expand(int minNewSegmentSize)
    {
        // First, let's move current segment to segment list:
        if (_segments == null) {
            _segments = new LinkedList<char[]>();
        }
        char[] curr = _currentSegment;
        _hasSegments = true;
        _segments.add(curr);
        _segmentSize += curr.length;
        int oldLen = curr.length;
        // Let's grow segments by 50% minimum
        int sizeAddition = oldLen >> 1;
        if (sizeAddition < minNewSegmentSize) {
            sizeAddition = minNewSegmentSize;
        }
        curr = _charArray(Math.min(MAX_SEGMENT_LEN, oldLen + sizeAddition));
        _currentSize = 0;
        _currentSegment = curr;
    }

    private char[] buildResultArray()
    {
        if (_resultString != null) { // Can take a shortcut...
            return _resultString.toCharArray();
        }
        char[] result;
        
        // Do we use shared array?
        if (_inputStart >= 0) {
            if (_inputLen < 1) {
                return NO_CHARS;
            }
            result = _charArray(_inputLen);
            System.arraycopy(_inputBuffer, _inputStart, result, 0,
                             _inputLen);
        } else { // nope 
            int size = size();
            if (size < 1) {
                return NO_CHARS;
            }
            int offset = 0;
            result = _charArray(size);
            if (_segments != null) {
                for (char[] curr : _segments) {
                    int currLen = curr.length;
                    System.arraycopy(curr, 0, result, offset, currLen);
                    offset += currLen;
                }
            }
            System.arraycopy(_currentSegment, 0, result, offset, _currentSize);
        }
        return result;
    }

    private final char[] _charArray(int len) {
        return new char[len];
    }
}
