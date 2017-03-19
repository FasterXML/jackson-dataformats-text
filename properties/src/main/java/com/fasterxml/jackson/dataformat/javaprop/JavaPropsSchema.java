package com.fasterxml.jackson.dataformat.javaprop;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropPathSplitter;
import com.fasterxml.jackson.dataformat.javaprop.util.Markers;

/**
 * Simple {@link FormatSchema} sub-type that defines details of things like:
 *<ul>
 *  <li>How are "flat" property names mapped to hierarchic POJO types, using
 * separator-based naming convention.
 *    </li>
 *  <li>What indentation (if any) is used before key values, and between key/value
 *    </li>
 *  <li>If and how are Array values inferred from property names
 *    </li>
 *</ul>
 */
public class JavaPropsSchema
    implements FormatSchema,
    java.io.Serializable
{
    private static final long serialVersionUID = 1L; // 2.5

    protected final static Markers DEFAULT_INDEX_MARKER = Markers.create("[", "]");

    protected final static JavaPropsSchema EMPTY = new JavaPropsSchema();

    /**
     * Since splitter instances are slightly costly to build in some cases,
     * we will lazily instantiate and cache them.
     */
    protected transient JPropPathSplitter _splitter;

    /*
    /**********************************************************************
    /* Simple numeric properties
    /**********************************************************************
     */

    /**
     * Specifies index number used when writing the first array entry (which
     * in Java has index of 0). After this initial value, additional elements
     * will have consecutive values, incremented by 1.
     * Note that this setting has no effect on reading: input indexes are only
     * used for sorting values, and their exact values have no meaning.
     *<p>
     * Default value is 1.
     */
    protected int _firstArrayOffset = 1;

    /*
    /**********************************************************************
    /* Formatting constants for input and output
    /**********************************************************************
     */

    /**
     * Default path separator to use for hierarchic paths, if any; empty
     * String may be used to indicate that no hierarchy should be inferred
     * using a simple separator (although index markers may still be used,
     * if defined).
     */
    protected String _pathSeparator = ".";

    /**
     * Default start marker for index access, if any; empty String may be used
     * to indicate no marker-based index detection should be made.
     *<p>
     * Default value of "[" is usually combined with end marker of "]" to allow
     * C/Java-style bracket notation, like "settings.path[1]".
     */
    protected Markers _indexMarker = DEFAULT_INDEX_MARKER;

    /*
    /**********************************************************************
    /* Formatting constants for input(-only)
    /**********************************************************************
     */

    /**
     * Whether 'simple' index-notation is supported for path segments or not:
     * simple meaning that if a path segment is a textual representation of
     * a non-negative integer value with length of 9 or less (that is, up to
     * but no including one billion), it will be considered index, not property
     * name.
     *<p>
     * Note that this settings does NOT control whether "start/end marker" indicated
     * indexes are enabled or not; those depend on {@link #_indexMarker}.
     *<p>
     * Default value is <code>true</code>, "plain" index segments are
     * supported.
     */
    protected boolean _parseSimpleIndexes = true;

    /*
    /**********************************************************************
    /* Formatting constants for output(-only)
    /**********************************************************************
     */

    /**
     * Whether array-element paths are written using start/end markers
     * (see {@link #_indexMarker} or
     * "simple" index number: if set to <code>true</code> AND markers
     * are specified as non-empty Strings, will use sequence of
     *<pre>
     *   startMarker index endMarker
     *</pre>
     * to include index in path; otherwise will simply use textual representation
     * of the index number as path segment, prefixed by path separator as necessary.
     */
    protected boolean _writeIndexUsingMarkers;

    /**
     * String prepended before key value, as possible indentation
     */
    protected String _lineIndentation = "";

    /**
     * String added between key and value; needs to include the "equals character"
     * (either '=' or ':', both allowed by Java Properties specification), may
     * also include white before and/or after "equals character".
     * Default value is a single '=' character with no white spaces around
     */
    protected String _keyValueSeparator = "=";
    
    /**
     * String added after value, including at least one linefeed.
     * Default value is the 'Unix linefeed'.
     */
    protected String _lineEnding = "\n";

    /**
     * Optional header to prepend before any other output: typically a
     * comment section or so. Note that contents here are
     * <b>NOT modified in any way</b>, meaning that any comment indicators
     * (leading '#' or '!') and linefeeds MUST be specified by caller.
     */
    protected String _header = "";

    /*
    /**********************************************************************
    /* Construction, factories, mutant factories
    /**********************************************************************
     */

    public JavaPropsSchema() { }

    public JavaPropsSchema(JavaPropsSchema base) {
        _firstArrayOffset = base._firstArrayOffset;
        _pathSeparator = base._pathSeparator;
        _indexMarker = base._indexMarker;
        _parseSimpleIndexes = base._parseSimpleIndexes;
        _writeIndexUsingMarkers = base._writeIndexUsingMarkers;
        _lineIndentation = base._lineIndentation;
        _keyValueSeparator = base._keyValueSeparator;
        _lineEnding = base._lineEnding;
        _header = base._header;
    }

    /**
     * Accessor for getting a {@link JPropPathSplitter} instance that does
     * splitting according to the settings of this instance.
     *<p>
     * Note that instance is constructed lazily as needed, but reused afterwards
     * for this instance (and for these specific settings).
     */
    public JPropPathSplitter pathSplitter() {
        JPropPathSplitter splitter = _splitter;
        if (splitter == null) {
            _splitter = splitter = JPropPathSplitter.create(this);
        }
        return splitter;
    }
    
    public JavaPropsSchema withFirstArrayOffset(int v) {
        if (v == _firstArrayOffset) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._firstArrayOffset = v;
        return s;
    }

    /**
     * Mutant factory method for constructing a new instance with
     * specified path separator; default being comma (".").
     * Note that setting separator to `null` or empty String will
     * basically disable handling of nesting, similar to
     * calling {@link #withoutPathSeparator}.
     */
    public JavaPropsSchema withPathSeparator(String v) {
        if (v == null) {
            v = "";
        }
        if (_equals(v, _pathSeparator)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._pathSeparator = v;
        return s;
    }

    /**
     * Mutant factory method for constructing a new instance that
     * specifies that no "path splitting" is to be done: this is
     * similar to default behavior of {@link java.util.Properties}
     * in which keys are full Strings and there is no nesting of values.
     */
    public JavaPropsSchema withoutPathSeparator() {
        if ("".equals(_pathSeparator)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._pathSeparator = "";
        return s;
    }

    public JavaPropsSchema withIndexMarker(Markers v) {
        if (_equals(v, _indexMarker)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._indexMarker = v;
        return s;
    }

    public JavaPropsSchema withoutIndexMarker() {
        if (_indexMarker == null) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._indexMarker = null;
        return s;
    }
    
    public JavaPropsSchema withParseSimpleIndexes(boolean v) {
        if (v == _parseSimpleIndexes) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._parseSimpleIndexes = v;
        return s;
    }

    public JavaPropsSchema withWriteIndexUsingMarkers(boolean v) {
        if (v == _writeIndexUsingMarkers) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._writeIndexUsingMarkers = v;
        return s;
    }

    public JavaPropsSchema withLineIndentation(String v) {
        if (_equals(v, _lineIndentation)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._lineIndentation = v;
        return s;
    }

    /**
     * @since 2.8
     */
    public JavaPropsSchema withoutLineIndentation() {
        return withLineIndentation("");
    }

    public JavaPropsSchema withKeyValueSeparator(String v) {
        if (_equals(v, _keyValueSeparator)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._keyValueSeparator = v;
        return s;
    }

    public JavaPropsSchema withLineEnding(String v) {
        if (_equals(v, _lineEnding)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._lineEnding = v;
        return s;
    }

    /**
     * Mutant factory for constructing schema instance where specified
     * header section (piece of text written out right before actual
     * properties entries) will be used.
     * Note that caller must specify any and all linefeeds to use: generator
     * will NOT modify header String contents in any way, and will not append
     * a linefeed after contents (if any).
     */
    public JavaPropsSchema withHeader(String v) {
        if (_equals(v, _header)) {
            return this;
        }
        JavaPropsSchema s = new JavaPropsSchema(this);
        s._header = v;
        return s;
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   withHeader("")
     *</pre>
     * used to ensure that no header is prepended before actual property values
     * are output.
     *
     * @since 2.8
     */
    public JavaPropsSchema withoutHeader() {
        return withHeader("");
    }

    /*
    /**********************************************************************
    /* Public API, FormatSchema
    /**********************************************************************
     */

    @Override
    public String getSchemaType() {
        return "JavaProps";
    }

    public static JavaPropsSchema emptySchema() {
        return EMPTY;
    }

    /*
    /**********************************************************************
    /* Public API, extended, properties
    /**********************************************************************
     */

    public int firstArrayOffset() {
        return _firstArrayOffset;
    }

    public String header() {
        return _header;
    }

    public Markers indexMarker() {
        return _indexMarker;
    }

    public String lineEnding() {
        return _lineEnding;
    }

    public String lineIndentation() {
        return _lineIndentation;
    }

    public String keyValueSeparator() {
        return _keyValueSeparator;
    }

    public boolean parseSimpleIndexes() {
        return _parseSimpleIndexes;
    }

    public String pathSeparator() {
        return _pathSeparator;
    }

    public boolean writeIndexUsingMarkers() {
        return _writeIndexUsingMarkers && (_indexMarker != null);
    }

    private <V> boolean _equals(V a, V b) {
        if (a == null) {
            return (b == null);
        }
        return (b != null) && a.equals(b);
    }
}
