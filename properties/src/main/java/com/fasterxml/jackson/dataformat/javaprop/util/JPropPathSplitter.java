package com.fasterxml.jackson.dataformat.javaprop.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;

/**
 * Helper class used for splitting a flattened property key into
 * nested/structured path that can be used to traverse and/or define
 * hierarchic structure.
 */
public abstract class JPropPathSplitter
{
    protected final boolean _useSimpleIndex;

    protected JPropPathSplitter(boolean useSimpleIndex) {
        _useSimpleIndex = useSimpleIndex;
    }

    public static JPropPathSplitter create(JavaPropsSchema schema)
    {
        // will never be null
        String sep = schema.pathSeparator();
        final Markers indexMarker = schema.indexMarker();

        // First: index marker in use?
        if (indexMarker == null) { // nope, only path separator, if anything
            // and if no separator, can use bogus "splitter":
            return pathOnlySplitter(schema);
        }
        // Yes, got index marker to use. But separator?
        if (sep.isEmpty()) {
            return new IndexOnlySplitter(schema.parseSimpleIndexes(), indexMarker);
        }
        return new FullSplitter(sep, schema.parseSimpleIndexes(),
                indexMarker,
                pathOnlySplitter(schema));
    }

    private static JPropPathSplitter pathOnlySplitter(JavaPropsSchema schema)
    {
        String sep = schema.pathSeparator();
        if (sep.isEmpty()) {
            return NonSplitting.instance;
        }
        // otherwise it's still quite simple
        if (sep.length() == 1) {
            return new CharPathOnlySplitter(sep.charAt(0), schema.parseSimpleIndexes());
        }
        return new StringPathOnlySplitter(sep, schema.parseSimpleIndexes());
    }

    /**
     * Main access method for splitting key into one or more segments and using
     * segmentation to add the String value as a node in its proper location.
     * 
     * @return Newly added node
     */
    public abstract JPropNode splitAndAdd(JPropNode parent,
            String key, String value);

    /*
    /**********************************************************************
    /* Helper methods for implementations
    /**********************************************************************
     */

    protected JPropNode _addSegment(JPropNode parent, String segment)
    {
        if (_useSimpleIndex) {
            int ix = _asInt(segment);
            if (ix >= 0) {
                return parent.addByIndex(ix);
            }
        }
        return parent.addByName(segment);
    }

    protected JPropNode _lastSegment(JPropNode parent, String path, int start, int end)
    {
        if (start < end) {
            if (start > 0) {
                path = path.substring(start);
            }
            parent = _addSegment(parent, path);
        }
        return parent;
    }

    protected int _asInt(String segment) {
        final int len = segment.length();
        // do not allow ridiculously long numbers as indexes
        if ((len == 0) || (len > 9)) {
            return -1;
        }
        char c = segment.charAt(0);
        if ((c > '9') || (c < '0')) {
            return -1;
        }
        for (int i = 0; i < len; ++i) {
            c = segment.charAt(i);
            if ((c > '9') || (c < '0')) {
                return -1;
            }
        }
        return Integer.parseInt(segment);
    }

    /*
    /**********************************************************************
    /* Implementations
    /**********************************************************************
     */

    /**
     * "No-op" implementation that does no splitting and simply adds entries
     * as is.
     */
    public static class NonSplitting extends JPropPathSplitter
    {
        public final static NonSplitting instance = new NonSplitting();

        private NonSplitting() { super(false); }
        
        @Override
        public JPropNode splitAndAdd(JPropNode parent,
                String key, String value)
        {
            return parent.addByName(key).setValue(value);
        }
    }

    /**
     * Simple variant where we only have path separator, and optional "segment
     * is index iff value is integer number"
     */
    public static class CharPathOnlySplitter extends JPropPathSplitter
    {
        protected final char _pathSeparatorChar;

        public CharPathOnlySplitter(char sepChar, boolean useIndex)
        {
            super(useIndex);
            _pathSeparatorChar = sepChar;
        }

        @Override
        public JPropNode splitAndAdd(JPropNode parent,
                String key, String value)
        {
            JPropNode curr = parent;
            int start = 0;
            final int keyLen = key.length();
            int ix;

            while ((ix = key.indexOf(_pathSeparatorChar, start)) >= start) {
                if (ix > start) { // segment before separator
                    String segment = key.substring(start, ix);
                    curr = _addSegment(curr, segment);
                }
                start = ix + 1;
                if (start == key.length()) {
                    break;
                }
            }
            return _lastSegment(curr, key, start, keyLen).setValue(value);
        }
    }

    /**
     * Simple variant where we only have path separator, and optional "segment
     * is index iff value is integer number"
     */
    public static class StringPathOnlySplitter extends JPropPathSplitter
    {
        protected final String _pathSeparator;
        protected final int _pathSeparatorLength;

        public StringPathOnlySplitter(String pathSeparator, boolean useIndex)
        {
            super(useIndex);
            _pathSeparator = pathSeparator;
            _pathSeparatorLength = pathSeparator.length();
        }

        @Override
        public JPropNode splitAndAdd(JPropNode parent,
                String key, String value)
        {
            JPropNode curr = parent;
            int start = 0;
            final int keyLen = key.length();
            int ix;

            while ((ix = key.indexOf(_pathSeparator, start)) >= start) {
                if (ix > start) { // segment before separator
                    String segment = key.substring(start, ix);
                    curr = _addSegment(curr, segment);
                }
                start = ix + _pathSeparatorLength;
                if (start == key.length()) {
                    break;
                }
            }
            return _lastSegment(curr, key, start, keyLen).setValue(value);
        }
    }
    
    /**
     * Special variant that does not use path separator, but does allow
     * index indicator, at the end of path.
     */
    public static class IndexOnlySplitter extends JPropPathSplitter
    {
        protected final Pattern _indexMatch;
        
        public IndexOnlySplitter(boolean useSimpleIndex,
                Markers indexMarker)
        {
            super(useSimpleIndex);
            _indexMatch = Pattern.compile(String.format("(.*)%s(\\d{1,9})%s$",
                    Pattern.quote(indexMarker.getStart()),
                    Pattern.quote(indexMarker.getEnd())));
        }

        @Override
        public JPropNode splitAndAdd(JPropNode parent,
                String key, String value)
        {
            Matcher m = _indexMatch.matcher(key);
            // short-cut for common case of no index:
            if (!m.matches()) {
                return _addSegment(parent, key).setValue(value);
            }
            // otherwise we need recursion as we "peel" away layers
            return _splitMore(parent, m.group(1), m.group(2))
                    .setValue(value);
        }

        protected JPropNode _splitMore(JPropNode parent, String prefix, String indexStr)
        {
            int ix = Integer.parseInt(indexStr);
            Matcher m = _indexMatch.matcher(prefix);
            if (!m.matches()) {
                parent = _addSegment(parent, prefix);
            } else {
                parent = _splitMore(parent, m.group(1), m.group(2));
            }
            return parent.addByIndex(ix);
        }
    }

    /**
     * Instance that supports both path separator and index markers
     * (and possibly also "simple" indexes)
     */
    public static class FullSplitter extends JPropPathSplitter
    {
        protected final Pattern _indexMatch;

        // small but important optimization for cases where index markers are absent
        protected final int _indexFirstChar;
        protected final JPropPathSplitter _simpleSplitter;
        
        public FullSplitter(String pathSeparator, boolean useSimpleIndex,
                Markers indexMarker, JPropPathSplitter fallbackSplitter)
        {
            super(useSimpleIndex);
            String startMarker = indexMarker.getStart();
            _indexFirstChar = startMarker.charAt(0);
            _simpleSplitter = fallbackSplitter;
            _indexMatch = Pattern.compile(String.format
                    ("(%s)|(%s(\\d{1,9})%s)",
                            Pattern.quote(pathSeparator),
                            Pattern.quote(startMarker),
                            Pattern.quote(indexMarker.getEnd())));
        }

        @Override
        public JPropNode splitAndAdd(JPropNode parent,
                String key, String value)
        {
            if (key.indexOf(_indexFirstChar) < 0) { // no index start marker
                return _simpleSplitter.splitAndAdd(parent, key, value);
            }
            Matcher m = _indexMatch.matcher(key);
            int start = 0;

            while (m.find()) {
                // which match did we get? Either path separator (1), or index (2)
                int ix = m.start(1);

                if (ix >= 0) { // path separator...
                    if (ix > start) {
                        String segment = key.substring(start, ix);
                        parent = _addSegment(parent, segment);
                    }
                    start = m.end(1);
                    continue;
                }
                // no, index marker, with contents
                ix = m.start(2);
                if (ix > start) {
                    String segment = key.substring(start, ix);
                    parent = _addSegment(parent, segment);
                }
                start = m.end(2);
                ix = Integer.parseInt(m.group(3));
                parent = parent.addByIndex(ix);
            }
            return _lastSegment(parent, key, start, key.length()).setValue(value);
        }
    }
}
