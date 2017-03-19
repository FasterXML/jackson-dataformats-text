package com.fasterxml.jackson.dataformat.javaprop.util;

/**
 * Simple value class for encapsulating a pair of start and end markers;
 * initially needed for index markers (like "[" and "]").
 */
public class Markers
{
    protected final String _start, _end;

    protected Markers(String start, String end) {
        if (start == null || start.isEmpty()) {
            throw new IllegalArgumentException("Missing 'start' value");
        }
        if (end == null || end.isEmpty()) {
            throw new IllegalArgumentException("Missing 'end' value");
        }
        _start = start;
        _end = end;
    }

    /**
     * Factory method for creating simple marker pair with given
     * start and end markers. Note that both are needed; neither may
     * be empty or null
     */
    public static Markers create(String start, String end) {
        return new Markers(start, end);
    }
    
    public String getStart() {
        return _start;
    }

    public String getEnd() {
        return _end;
    }

//    public StringBuilder appendIn(String)
}
