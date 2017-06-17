
package com.fasterxml.jackson.dataformat.csv;

import java.util.*;

import com.fasterxml.jackson.core.FormatSchema;

/**
 * Simple {@link FormatSchema} sub-type that defines properties of
 * a CSV document to read or write.
 * Properties supported currently are:
 *<ul>
 * <li>columns (List of ColumnDef) [default: empty List]: Ordered list of columns (which may be empty, see below).
 *   Each column has name (mandatory)  as well as type (optional; if not
 *   defined, defaults to "String").
 *   Note that
 *  </li>
 * <li>useHeader (boolean) [default: false]: whether the first line of physical document defines
 *    column names (true) or not (false): if enabled, parser will take
 *    first-line values to define column names; and generator will output
 *    column names as the first line
 *  </li>
 * <li>quoteChar (char) [default: double-quote ('")]: character used for quoting values
 *   that contain quote characters or linefeeds.
 *  </li>
 * <li>columnSeparator (char) [default: comma (',')]: character used to separate values.
 *     Other commonly used values include tab ('\t') and pipe ('|')
 *  </li>
 * <li>arrayElementSeparator (String) [default: semicolon (";")]: string used to separate array elements.
 *  </li>
 * <li>lineSeparator (String) [default: "\n"]: character used to separate data rows.
 *    Only used by generator; parser accepts three standard linefeeds ("\r", "\r\n", "\n").
 *  </li>
 * <li>escapeChar (int) [default: -1 meaning "none"]: character, if any, used to
 *   escape values. Most commonly defined as backslash ('\'). Only used by parser;
 *   generator only uses quoting, including doubling up of quotes to indicate quote char
 *   itself.
 *  </li>
 * <li>skipFirstDataRow (boolean) [default: false]: whether the first data line (either
 *    first line of the document, if useHeader=false, or second, if useHeader=true)
 *    should be completely ignored by parser. Needed to support CSV-like file formats
 *    that include additional non-data content before real data begins (specifically
 *    some database dumps do this)
 *  </li>
 * <li>nullValue (String) [default: "" (empty String)]: When asked to write Java `null`,
 *    this String value will be used instead.<br />
 *   With 2.6, value will also be recognized during value reads.
 *  </li>
 * <li>strictHeaders (boolean) [default: false] (added in Jackson 2.7): whether names of
 *   columns defined in the schema MUST match with actual declaration from
 *   the header row (if header row handling enabled): if true, they must be and
 *   an exception if thrown if order differs: if false, no verification is performed.
 *  </li>
 * </ul>
 *<p>
 * Note that schemas without any columns are legal, but if no columns
 * are added, behavior of parser/generator is usually different and
 * content will be exposed as logical Arrays instead of Objects.
 *<p>
 *
 * There are 4 ways to create <code>CsvSchema</code> instances:
 *<ul>
 * <li>Manually build one, using {@link Builder}
 *  </li>
 * <li>Modify existing schema (using <code>withXxx</code> methods
 *    or {@link #rebuild} for creating {@link Builder})
 *  </li>
 * <li>Create schema based on a POJO definition (Class), using
 *    {@link CsvMapper} methods like {@link CsvMapper#schemaFor(java.lang.Class)}.
 *  </li>
 * <li>Request that {@link CsvParser} reads schema from the first line:
 *    enable "useHeader" property for the initial schema, and let parser
 *    read column names from the document itself.
 *  </li>
 *</ul>
 */
public class CsvSchema 
    implements FormatSchema,
        Iterable<CsvSchema.Column>,
        java.io.Serializable // since 2.5
{
    private static final long serialVersionUID = 1L; // 2.5

    /*
    /**********************************************************************
    /* Constants, feature flags
    /**********************************************************************
     */

    protected final static int ENCODING_FEATURE_USE_HEADER = 0x0001;
    protected final static int ENCODING_FEATURE_SKIP_FIRST_DATA_ROW = 0x0002;
    protected final static int ENCODING_FEATURE_ALLOW_COMMENTS = 0x0004;
    protected final static int ENCODING_FEATURE_REORDER_COLUMNS = 0x0008;
    protected final static int ENCODING_FEATURE_STRICT_HEADERS = 0x0010;

    protected final static int DEFAULT_ENCODING_FEATURES = 0;

    protected final static char[] NO_CHARS = new char[0];

    /*
    /**********************************************************************
    /* Constants, default settings
    /**********************************************************************
     */

    /**
     * Default separator for column values is comma (hence "Comma-Separated Values")
     */
    public final static char DEFAULT_COLUMN_SEPARATOR = ',';

    /**
     * Default separator for array elements within a column value is
     * semicolon.
     */
    public final static String DEFAULT_ARRAY_ELEMENT_SEPARATOR = ";";

    /**
     * Marker for the case where no array element separator is used
     */
    public final static String NO_ARRAY_ELEMENT_SEPARATOR = "";

    /**
     * By default no "any properties" (properties for 'extra' columns; ones
     * not specified in schema) are used, so <code>null</code> is used as marker.
     *
     * @since 2.7
     */
    public final static String DEFAULT_ANY_PROPERTY_NAME = null;
    
    public final static char DEFAULT_QUOTE_CHAR = '"';

    /**
     * By default, nulls are written as empty Strings (""); and no coercion
     * is performed from any String (higher level databind may, however,
     * coerce Strings into Java nulls).
     * To use automatic coercion on reading, null value must be set explicitly
     * to empty String ("").
     *<p>
     * NOTE: before 2.6, this value default to empty <code>char[]</code>; changed
     * to Java null in 2.6.
     */
    public final static char[] DEFAULT_NULL_VALUE = null;
    
    /**
     * By default, no escape character is used -- this is denoted by
     * int value that does not map to a valid character
     */
    public final static int DEFAULT_ESCAPE_CHAR = -1;

    public final static char[] DEFAULT_LINEFEED = "\n".toCharArray();

    /*
    /**********************************************************************
    /* Constants, other
    /**********************************************************************
     */
    
    protected final static Column[] NO_COLUMNS = new Column[0];
    
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Enumeration that defines optional type indicators that can be passed
     * with schema. If used type is used to determine type of
     * {@link com.fasterxml.jackson.core.JsonToken}
     * that column values are exposed as.
     */
    public enum ColumnType
    {
        /**
         * Default type if not explicitly defined; value will
         * be presented as <code>VALUE_STRING</code> by parser,
         * that is, no type-inference is performed, and value is
         * not trimmed.
         *<p>
         * Note that this type allows coercion into array, if higher
         * level application calls
         * {@link com.fasterxml.jackson.core.JsonParser#isExpectedStartArrayToken},
         * unlike more explicit types.
         */
        STRING,

        /**
         * Value is considered to be a String, except that tokens
         * "null", "true" and "false" are recognized as matching
         * tokens and reported as such;
         * and values are trimmed (leading/trailing white space)
         */
        STRING_OR_LITERAL,
        
        /**
         * Value should be a number, but literals "null", "true" and "false"
         * are also understood, and an empty String is considered null.
         * Values are also trimmed (leading/trailing white space)
         * Other non-numeric Strings may cause parsing exception.
         */
        NUMBER,

        /**
         * Value is taken to be a number (if it matches valid JSON number
         * formatting rules), literal (null, true or false) or String,
         * depending on best match.
         * Values are also trimmed (leading/trailing white space)
         */
        NUMBER_OR_STRING,

        /**
         * Value is expected to be a boolean ("true", "false") String,
         * or "null", or empty String (equivalent to null).
         * Values are trimmed (leading/trailing white space).
         * Values other than indicated above may result in an exception.
         * 
         * @since 2.5
         */
        BOOLEAN,
        
        /**
         * Value will be a multi-value sequence, separated by array element
         * separator. Element type itself may be any scalar type (that is, number
         * or String) and will not be optimized.
         * Separator may be overridden on per-column basis.
         *<p>
         * Note that this type is used for generic concept of multiple values, and
         * not specifically to match Java arrays: data-binding may match such columns
         * to {@link java.util.Collection}s as well, or even other types as necessary.
         * 
         * @since 2.5
         */
        ARRAY,
        
        ;
    }

    /**
     * Representation of info for a single column
     */
    public static class Column implements java.io.Serializable // since 2.4.3
    {
        private static final long serialVersionUID = 1L;

        public final static Column PLACEHOLDER = new Column(0, "");
        
        private final String _name;
        private final int _index;
        private final ColumnType _type;

        /**
         * NOTE: type changed from `char` to `java.lang.String` in 2.7
         *
         * @since 2.5
         */
        private final String _arrayElementSeparator;

        /**
         * Link to the next column within schema, if one exists;
         * null for the last column.
         * 
         * @since 2.6
         */
        private final Column _next;
        
        public Column(int index, String name) {
            this(index, name, ColumnType.STRING, "");
        }

        public Column(int index, String name, ColumnType type) {
            this(index, name, type, "");
        }

        /**
         * @deprecated use variant where `arrayElementSep` is <code>String</code>
         */
        @Deprecated // in 2.7; remove from 2.8
        public Column(int index, String name, ColumnType type, int arrayElementSep) {
            this(index, name, type, (arrayElementSep < 0) ? NO_ARRAY_ELEMENT_SEPARATOR : Character.toString((char) arrayElementSep));
        }

        public Column(int index, String name, ColumnType type, String arrayElementSep)
        {
            _index = index;
            _name = name;
            _type = type;
            _arrayElementSeparator = _validArrayElementSeparator(arrayElementSep);
            _next = null;
        }

        public Column(Column src, Column next) {
            this(src, src._index, next);
        }

        protected Column(Column src, int index, Column next)
        {
            _index = index;
            _name = src._name;
            _type = src._type;
            _arrayElementSeparator = src._arrayElementSeparator;
            _next = next;
        }
        
        public Column withName(String newName) {
            if (_name == newName) {
                return this;
            }
            return new Column(_index, newName, _type, _arrayElementSeparator);
        }

        public Column withType(ColumnType newType) {
            if (newType == _type) {
                return this;
            }
            return new Column(_index, _name, newType, _arrayElementSeparator);
        }

        /**
         * @deprecated use {@link #withArrayElementSeparator(String)} instead
         */
        @Deprecated // in 2.7; remove from 2.8
        public Column withElementSeparator(int separator) {
            return withArrayElementSeparator((separator < 0) ? NO_ARRAY_ELEMENT_SEPARATOR : Character.toString((char) separator));
        }

        public Column withArrayElementSeparator(String separator) {
            String sep = _validArrayElementSeparator(separator);
            if (_arrayElementSeparator.equals(sep)) {
                return this;
            }
            return new Column(_index, _name, _type, sep);
        }

        public Column withNext(Column next) {
            if (_next == next) {
                return this;
            }
            return new Column(this, next);
        }

        /**
         * @since 2.7
         */
        public Column withNext(int index, Column next) {
            if ((_index == index) && (_next == next)) {
                return this;
            }
            return new Column(this, index, next);
        }
        
        public int getIndex() { return _index; }
        public String getName() { return _name; }
        public ColumnType getType() { return _type; }

        public Column getNext() { return _next; }

        /**
         * Access that returns same as {@link #getNext} iff name of that
         * column is same as given name
         */
        public Column getNextWithName(String name) {
            if (_next != null && name.equals(_next._name)) {
                return _next;
            }
            return null;
        }

        public boolean hasName(String n) {
            return (_name == n) || _name.equals(n);
        }
        
        /**
         * @since 2.5
         */
        public String getArrayElementSeparator() { return _arrayElementSeparator; }

        public boolean isArray() {
            return (_type == ColumnType.ARRAY);
        }
    }
    
    /**
     * Class used for building {@link CsvSchema} instances.
     */
    public static class Builder
    {
        protected final ArrayList<Column> _columns = new ArrayList<Column>();

        /**
         * Bit-flag for general-purpose on/off features.
         * 
         * @since 2.5
         */
        protected int _encodingFeatures = DEFAULT_ENCODING_FEATURES;
        
        protected char _columnSeparator = DEFAULT_COLUMN_SEPARATOR;

        protected String _arrayElementSeparator = DEFAULT_ARRAY_ELEMENT_SEPARATOR;

        /**
         * If "any properties" (properties for 'extra' columns; ones
         * not specified in schema) are enabled, they are mapped to
         * this name: leaving it as <code>null</code> disables use of
         * "any properties" (and they are either ignored, or an exception
         * is thrown, depending on other settings); setting it to a non-null
         * String value will expose all extra properties under one specified
         * name. 
         * 
         * @since 2.7
         */
        protected String _anyPropertyName = DEFAULT_ANY_PROPERTY_NAME;
        
        // note: need to use int to allow -1 for 'none'
        protected int _quoteChar = DEFAULT_QUOTE_CHAR;

        // note: need to use int to allow -1 for 'none'
        protected int _escapeChar = DEFAULT_ESCAPE_CHAR;
        
        protected char[] _lineSeparator = DEFAULT_LINEFEED;

        /**
         * @since 2.5
         */
        protected char[] _nullValue = DEFAULT_NULL_VALUE;

        public Builder() { }

        /**
         * "Copy" constructor which creates builder that has settings of
         * given source schema
         */
        public Builder(CsvSchema src)
        {
            for (Column col : src._columns) {
                _columns.add(col);
            }
            _encodingFeatures = src._features;
            _columnSeparator = src._columnSeparator;
            _arrayElementSeparator = src._arrayElementSeparator;
            _quoteChar = src._quoteChar;
            _escapeChar = src._escapeChar;
            _lineSeparator = src._lineSeparator;
            _nullValue = src._nullValue;
            _anyPropertyName = src._anyPropertyName;
        }

        /**
         * NOTE: does NOT check for duplicate column names so it is possibly to
         * accidentally add duplicates.
         */
        public Builder addColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name));
        }

        /**
         * NOTE: does NOT check for duplicate column names so it is possibly to
         * accidentally add duplicates.
         */
        public Builder addColumn(String name, ColumnType type) {
            int index = _columns.size();
            return addColumn(new Column(index, name, type));
        }

        /**
         * NOTE: does NOT check for duplicate column names so it is possibly to
         * accidentally add duplicates.
         */
        public Builder addColumn(Column c) {
            _columns.add(c);
            return this;
        }

        /**
         * NOTE: does NOT check for duplicate column names so it is possibly to
         * accidentally add duplicates.
         *
         * @since 2.9
         */
        public Builder addColumns(Iterable<Column> cs) {
            for (Column c : cs) {
                _columns.add(c);
            }
            return this;
        }

        /**
         * NOTE: does NOT check for duplicate column names so it is possibly to
         * accidentally add duplicates.
         *
         * @since 2.9
         */
        public Builder addColumns(Iterable<String> names, ColumnType type) {
            Builder result = this;
            for (String name : names) {
                result = addColumn(name, type);
            }
            return result;
        }

        /**
         * NOTE: unlike many other add methods, this method DOES check for, and
         * discard, possible duplicate columns: that is, if this builder already
         * has a column with same name as column to be added, existing column
         * is retained and new column ignored.
         *
         * @since 2.9
         */
        public Builder addColumnsFrom(CsvSchema schema) {
            Builder result = this;
            for (Column col : schema) {
                if (!hasColumn(col.getName())) {
                    result = result.addColumn(col);
                }
            }
            return result;
        }

        public Builder addArrayColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.ARRAY, ""));
        }

        /**
         * @deprecated use {@link #addArrayColumn(String, String)} instead
         */
        @Deprecated // in 2.7; remove from 2.8
        public Builder addArrayColumn(String name, int elementSeparator) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.ARRAY, elementSeparator));
        }

        /**
         * @since 2.7
         */
        public Builder addArrayColumn(String name, String elementSeparator) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.ARRAY, elementSeparator));
        }
        public Builder addNumberColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.NUMBER));
        }
        public Builder addBooleanColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.BOOLEAN));
        }

        public Builder replaceColumn(int index, Column c) {
            _checkIndex(index);
            _columns.set(index, c);
            return this;
        }

        public Builder renameColumn(int index, String newName) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withName(newName));
            return this;
        }

        public Builder setColumnType(int index, ColumnType type) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withType(type));
            return this;
        }

        public Builder removeArrayElementSeparator(int index) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withArrayElementSeparator(""));
            return this;
        }

        /**
         * @deprecated use {@link #setArrayElementSeparator(String)} instead
         */
        @Deprecated // in 2.7; remove from 2.8
        public void setArrayElementSeparator(int index, char sep) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withElementSeparator(sep));
        }

        /**
         * @since 2.7
         */
        public Builder setArrayElementSeparator(int index, String sep) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withArrayElementSeparator(sep));
            return this;
        }

        public Builder setAnyPropertyName(String name) {
            _anyPropertyName = name;
            return this;
        }

        public Builder clearColumns() {
            _columns.clear();
            return this;
        }

        public int size() {
            return _columns.size();
        }

        public Iterator<Column> getColumns() {
            return _columns.iterator();
        }

        /**
         *<p>
         * NOTE: this method requires linear scan over existing columns
         * so it may be more efficient to use other types of lookups if
         * available (for example, {@link CsvSchema#column(String)} has a
         * hash lookup to use).
         *
         * @since 2.9
         */
        public boolean hasColumn(String name) {
            for (int i = 0, end = _columns.size(); i < end; ++i) {
                if (_columns.get(i).getName().equals(name)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Method for specifying whether Schema should indicate that
         * a header line (first row that contains column names) is to be
         * used for reading and writing or not.
         */
        public Builder setUseHeader(boolean b) {
            _feature(ENCODING_FEATURE_USE_HEADER, b);
            return this;
        }

        /**
         * Use in combination with setUseHeader.  When use header flag is
         * is set, this setting will reorder the columns defined in this
         * schema to match the order set by the header.
         *
         * @param b         Enable / Disable this setting
         * @return          This Builder instance
         *
         * @since 2.7
         */
        public Builder setReorderColumns(boolean b) {
            _feature(ENCODING_FEATURE_REORDER_COLUMNS, b);
            return this;
        }

        /**
         * Use in combination with {@link #setUseHeader}. When `strict-headers`
         * is set, encoder will ensure the headers are in the order
         * of the schema; if order differs, an exception is thrown.
         *
         * @param b Enable / Disable this setting
         * @return This Builder instance
         *
         * @since 2.7
         */
        public Builder setStrictHeaders(boolean b) {
            _feature(ENCODING_FEATURE_STRICT_HEADERS, b);
            return this;
        }

        /**
         * Method for specifying whether Schema should indicate that
         * the first line that is not a header (if header handling enabled)
         * should be skipped in its entirety.
         */
        public Builder setSkipFirstDataRow(boolean b) {
            _feature(ENCODING_FEATURE_SKIP_FIRST_DATA_ROW, b);
            return this;
        }

        /**
         * Method for specifying whether Schema should indicate that
         * "hash comments" (lines where the first non-whitespace character
         * is '#') are allowed; if so, they will be skipped without processing.
         * 
         * @since 2.5
         */
        public Builder setAllowComments(boolean b) {
            _feature(ENCODING_FEATURE_ALLOW_COMMENTS, b);
            return this;
        }
        
        protected final void _feature(int feature, boolean state) {
            _encodingFeatures = state ? (_encodingFeatures | feature) : (_encodingFeatures & ~feature);
        }

        /**
         * Method for specifying character used to separate column
         * values.
         * Default is comma (',').
         */
        public Builder setColumnSeparator(char c) {
            _columnSeparator = c;
            return this;
        }

        /**
         * @since 2.5
         * @deprecated use {@link #setArrayElementSeparator(String)} instead
         */
        @Deprecated // in 2.7; remove from 2.8
        public Builder setArrayElementSeparator(char c) {
            _arrayElementSeparator = Character.toString(c);
            return this;
        }

        /**
         * Method for specifying character used to separate array element
         * values.
         * Default value is semicolon (";")
         *
         * @since 2.7
         */
        public Builder setArrayElementSeparator(String separator) {
            _arrayElementSeparator = _validArrayElementSeparator(separator);
            return this;
        }

        /**
         * @since 2.5
         * @deprecated use {@link #disableArrayElementSeparator()} instead
         */
        @Deprecated // in 2.7; remove from 2.8
        public Builder disableElementSeparator(char c) {
            return disableArrayElementSeparator();
        }

        /**
         * @since 2.7
         */
        public Builder disableArrayElementSeparator() {
            _arrayElementSeparator = NO_ARRAY_ELEMENT_SEPARATOR;
            return this;
        }

        
        /**
         * Method for specifying character used for optional quoting
         * of values.
         * Default is double-quote ('"').
         */
        public Builder setQuoteChar(char c) {
            _quoteChar = c;
            return this;
        }

        /**
         * @since 2.4
         */
        public Builder disableQuoteChar() {
            _quoteChar = -1;
            return this;
        }
        
        /**
         * Method for specifying character used for optional escaping
         * of characters in quoted String values.
         * Default is "not used", meaning that no escaping used.
         */
        public Builder setEscapeChar(char c) {
            _escapeChar = c;
            return this;
        }

        /**
         * Method for specifying that no escape character is to be used
         * with CSV documents this schema defines.
         */
        public Builder disableEscapeChar() {
            _escapeChar = -1;
            return this;
        }
        
        public Builder setLineSeparator(String lf) {
            _lineSeparator = lf.toCharArray();
            return this;
        }

        public Builder setLineSeparator(char lf) {
            _lineSeparator = new char[] { lf };
            return this;
        }

        public Builder setNullValue(String nvl) {
            return setNullValue((nvl == null) ? null : nvl.toCharArray());
        }

        public Builder setNullValue(char[] nvl) {
            _nullValue = nvl;
            return this;
        }
        
        public CsvSchema build()
        {
            Column[] cols = _columns.toArray(new Column[_columns.size()]);
            return new CsvSchema(cols, _encodingFeatures,
                    _columnSeparator, _quoteChar, _escapeChar,
                    _lineSeparator, _arrayElementSeparator,
                    _nullValue, _anyPropertyName);
        }

        protected void _checkIndex(int index) {
            if (index < 0 || index >= _columns.size()) {
                throw new IllegalArgumentException("Illegal index "+index+"; only got "+_columns.size()+" columns");
            }
        }
    }

    /*
    /**********************************************************************
    /* Configuration, construction
    /**********************************************************************
     */
    
    /**
     * Column definitions, needed for optional header and/or mapping
     * of field names to column positions.
     */
    protected final Column[] _columns;
    
    protected final Map<String,Column> _columnsByName;

    /**
     * Bitflag for general-purpose on/off features.
     * 
     * @since 2.5
     */
    protected int _features = DEFAULT_ENCODING_FEATURES;

    protected final char _columnSeparator;

    protected final String _arrayElementSeparator;
    
    protected final int _quoteChar;
    
    protected final int _escapeChar;
    
    protected final char[] _lineSeparator;

    /**
     * @since 2.5
     */
    protected final char[] _nullValue;

    protected transient String _nullValueAsString;

    /**
     * If "any properties" (properties for 'extra' columns; ones
     * not specified in schema) are enabled, they are mapped to
     * this name: leaving it as <code>null</code> disables use of
     * "any properties" (and they are either ignored, or an exception
     * is thrown, depending on other settings); setting it to a non-null
     * String value will expose all extra properties under one specified
     * name. 
     *
     * @since 2.7
     */
    protected final String _anyPropertyName;

    /**
     * @deprecated use {@link #CsvSchema(Column[], int, char, int, int, char[], String, char[], String)} instead
     */
    @Deprecated // in 2.7; remove from 2.8
    public CsvSchema(Column[] columns, int features,
        char columnSeparator, int quoteChar, int escapeChar,
        char[] lineSeparator, int arrayElementSeparator,
        char[] nullValue) {
        this(columns, features, columnSeparator, quoteChar, escapeChar, lineSeparator,
            arrayElementSeparator == -1 ? "" : Character.toString((char) arrayElementSeparator), nullValue,
                    DEFAULT_ANY_PROPERTY_NAME);
    }

    /**
     * @since 2.7
     */
    public CsvSchema(Column[] columns, int features,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator, String arrayElementSeparator,
            char[] nullValue, String anyPropertyName)
    {
        if (columns == null) {
            columns = NO_COLUMNS;
        } else {
            columns = _link(columns);
        }
        _columns = columns;
        _features = features;
        _columnSeparator = columnSeparator;
        _arrayElementSeparator = arrayElementSeparator;
        _quoteChar = quoteChar;
        _escapeChar = escapeChar;
        _lineSeparator = lineSeparator;
        _nullValue = nullValue;
        _anyPropertyName = anyPropertyName;

        // and then we may need to create a mapping
        if (_columns.length == 0) {
            _columnsByName = Collections.emptyMap();
        } else {
            _columnsByName = new HashMap<String,Column>(4 + _columns.length);
            for (Column c : _columns) {
                _columnsByName.put(c.getName(), c);
            }
        }
    }

    /**
     * Copy constructor used for creating variants using
     * <code>withXxx()</code> methods.
     */
    protected CsvSchema(Column[] columns, int features,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator, String arrayElementSeparator,
            char[] nullValue,
            Map<String,Column> columnsByName, String anyPropertyName)
    {
        _columns = columns;
        _features = features;
        _columnSeparator = columnSeparator;
        _quoteChar = quoteChar;
        _escapeChar = escapeChar;
        _lineSeparator = lineSeparator;
        _arrayElementSeparator = arrayElementSeparator;
        _nullValue = nullValue;
        _columnsByName = columnsByName;
        _anyPropertyName = anyPropertyName;
    }    

    /**
     * Copy constructor used for creating variants using
     * <code>sortedBy()</code> methods.
     */
    protected CsvSchema(CsvSchema base, Column[] columns)
    {
        _columns = _link(columns);
        _features = base._features;
        _columnSeparator = base._columnSeparator;
        _quoteChar = base._quoteChar;
        _escapeChar = base._escapeChar;
        _lineSeparator = base._lineSeparator;
        _arrayElementSeparator = base._arrayElementSeparator;
        _nullValue = base._nullValue;
        _anyPropertyName = base._anyPropertyName;

        // and then we may need to create a mapping
        if (_columns.length == 0) {
            _columnsByName = Collections.emptyMap();
        } else {
            _columnsByName = new HashMap<String,Column>(4 + _columns.length);
            for (Column c : _columns) {
                _columnsByName.put(c.getName(), c);
            }
        }
    }
    
    /**
     * Copy constructor used for creating variants for on/off features
     * 
     * @since 2.5
     */
    protected CsvSchema(CsvSchema base, int features) {
        _columns = base._columns;
        _features = features;
        _columnSeparator = base._columnSeparator;
        _quoteChar = base._quoteChar;
        _escapeChar = base._escapeChar;
        _lineSeparator = base._lineSeparator;
        _arrayElementSeparator = base._arrayElementSeparator;
        _nullValue = base._nullValue;
        _anyPropertyName = base._anyPropertyName;
        _columnsByName = base._columnsByName;
    }

    /**
     * Helper method used for chaining columns together using next-linkage,
     * as well as ensuring that indexes are correct.
     */
    private static Column[] _link(Column[] orig)
    {
        int i = orig.length;
        Column[] result = new Column[i];
        Column prev = null;
        for (; --i >= 0; ) {
            Column curr = orig[i].withNext(i, prev);
            result[i] = curr;
            prev = curr;
        }
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Accessor for creating a "default" CSV schema instance, with following
     * settings:
     *<ul>
     * <li>Does NOT use header line
     *  </li>
     * <li>Uses double quotes ('"') for quoting of field values (if necessary)
     *  </li>
     * <li>Uses comma (',') as the field separator
     *  </li>
     * <li>Uses Unix linefeed ('\n') as row separator
     *  </li>
     * <li>Does NOT use any escape characters
     *  </li>
     * <li>Does NOT have any columns defined
     *  </li>
     * </ul>
     */
    public static CsvSchema emptySchema() {
        return builder().build();
    }
    
    /**
     * Helper method for constructing Builder that can be used to create modified
     * schema.
     */
    public Builder rebuild() {
        return new Builder(this);
    }

    /*
    /**********************************************************************
    /* Mutant factories
    /**********************************************************************
     */
    
    public CsvSchema withUseHeader(boolean state) {
        return _withFeature(ENCODING_FEATURE_USE_HEADER, state);
    }

    /**
     * Returns a clone of this instance by changing or setting the
     * column reordering flag
     *
     * @param state     New value for setting
     * @return          A copy of itself, ensuring the setting for
     *                  the column reordering feature.
     * @since 2.7
     */
    public CsvSchema withColumnReordering(boolean state) {
        return _withFeature(ENCODING_FEATURE_REORDER_COLUMNS, state);
    }

    /**
     * Returns a clone of this instance by changing or setting the
     * strict headers flag
     *
     * @param state     New value for setting
     * @return          A copy of itself, ensuring the setting for
     *                  the strict headers feature.
     * @since 2.7
     */
    public CsvSchema withStrictHeaders(boolean state) {
        return _withFeature(ENCODING_FEATURE_STRICT_HEADERS, state);
    }

    /**
     * Helper method for constructing and returning schema instance that
     * is similar to this one, except that it will be using header line.
     */
    public CsvSchema withHeader() {
        return _withFeature(ENCODING_FEATURE_USE_HEADER, true);
    }

    /**
     * Helper method for construcing and returning schema instance that
     * is similar to this one, except that it will not be using header line.
     */
    public CsvSchema withoutHeader() {
        return _withFeature(ENCODING_FEATURE_USE_HEADER, false);
    }

    public CsvSchema withSkipFirstDataRow(boolean state) {
        return _withFeature(ENCODING_FEATURE_SKIP_FIRST_DATA_ROW, state);
    }

    /**
     * Method to indicate whether "hash comments" are allowed
     * for document described by this schema.
     * 
     * @since 2.5
     */
    public CsvSchema withAllowComments(boolean state) {
        return _withFeature(ENCODING_FEATURE_ALLOW_COMMENTS, state);
    }

    /**
     * Method to indicate that "hash comments" ARE allowed
     * for document described by this schema.
     * 
     * @since 2.5
     */
    public CsvSchema withComments() {
        return _withFeature(ENCODING_FEATURE_ALLOW_COMMENTS, true);
    }

    /**
     * Method to indicate that "hash comments" are NOT allowed for document
     * described by this schema.
     * 
     * @since 2.5
     */
    public CsvSchema withoutComments() {
        return _withFeature(ENCODING_FEATURE_ALLOW_COMMENTS, false);
    }

    protected CsvSchema _withFeature(int feature, boolean state) {
        int newFeatures = state ? (_features | feature) : (_features & ~feature);
        return (newFeatures == _features) ? this : new CsvSchema(this, newFeatures);
    }

    public CsvSchema withColumnSeparator(char sep) {
        return (_columnSeparator == sep) ? this :
            new CsvSchema(_columns, _features,
                    sep, _quoteChar, _escapeChar, _lineSeparator, _arrayElementSeparator,
                    _nullValue, _columnsByName, _anyPropertyName);
    }

    public CsvSchema withQuoteChar(char c) {
        return (_quoteChar == c) ? this :
            new CsvSchema(_columns, _features,
                    _columnSeparator, c, _escapeChar, _lineSeparator,_arrayElementSeparator,
                    _nullValue, _columnsByName, _anyPropertyName);
    }

    public CsvSchema withoutQuoteChar() {
        return (_quoteChar == -1) ? this :
            new CsvSchema(_columns, _features,
                    _columnSeparator, -1, _escapeChar, _lineSeparator, _arrayElementSeparator,
                    _nullValue, _columnsByName, _anyPropertyName);
    }

    public CsvSchema withEscapeChar(char c) {
        return (_escapeChar == c) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, c, _lineSeparator, _arrayElementSeparator,
                        _nullValue, _columnsByName, _anyPropertyName);
    }

    public CsvSchema withoutEscapeChar() {
        return (_escapeChar == -1) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, -1, _lineSeparator, _arrayElementSeparator,
                        _nullValue, _columnsByName, _anyPropertyName);
    }

    /**
     * @since 2.5
     * @deprecated use {@link #withArrayElementSeparator(String)} instead
     */
    @Deprecated // in 2.7; remove in 2.8
    public CsvSchema withArrayElementSeparator(char c) {
        return withArrayElementSeparator( Character.toString(c));
    }

    /**
     * @since 2.7
     */
    public CsvSchema withArrayElementSeparator(String separator) {
        String sep = separator == null ? "" : separator;
        return (_arrayElementSeparator.equals(sep)) ? this : new CsvSchema(_columns, _features,
            _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, separator,
            _nullValue, _columnsByName, _anyPropertyName);
    }


    /**
     * @since 2.5
     */
    public CsvSchema withoutArrayElementSeparator() {
        return (_arrayElementSeparator.isEmpty()) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, "",
                        _nullValue, _columnsByName, _anyPropertyName);
    }
    
    public CsvSchema withLineSeparator(String sep) {
        return new CsvSchema(_columns, _features,
                _columnSeparator, _quoteChar, _escapeChar, sep.toCharArray(),
                _arrayElementSeparator, _nullValue, _columnsByName, _anyPropertyName);
    }

    /**
     * @since 2.5
     */
    public CsvSchema withNullValue(String nvl) {
        return new CsvSchema(_columns, _features,
                _columnSeparator, _quoteChar, _escapeChar, _lineSeparator,
                _arrayElementSeparator,
                (nvl == null) ? null : nvl.toCharArray(),
                _columnsByName, _anyPropertyName);
    }

    public CsvSchema withoutColumns() {
        return new CsvSchema(NO_COLUMNS, _features,
                _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, _arrayElementSeparator,
                _nullValue, _columnsByName, _anyPropertyName);
    }

    /**
     * Mutant factory method that will try to combine columns of this schema with those
     * from `toAppend`, starting with columns of this instance, and ignoring
     * duplicates (if any) from argument `toAppend`.
     * All settings aside from column sets are copied from `this` instance.
     *<p>
     * As with all `withXxx()` methods this method never modifies `this` but either
     * returns it unmodified (if no new columns found from `toAppend`), or constructs
     * a new instance and returns that.
     *
     * @since 2.9
     */
    public CsvSchema withColumnsFrom(CsvSchema toAppend) {
        int addCount = toAppend.size();
        if (addCount == 0) {
            return this;
        }
        Builder b = rebuild();
        for (int i = 0; i < addCount; ++i) {
            Column col = toAppend.column(i);
            if (column(col.getName()) == null) {
                b.addColumn(col);
            }
        }
        return b.build();
    }

    /**
     * @since 2.7
     */
    public CsvSchema withAnyPropertyName(String name) {
        return new CsvSchema(_columns, _features,
                _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, _arrayElementSeparator,
                _nullValue, _columnsByName, name);
    }
    
    /**
     * Mutant factory method that will construct a new instance in which columns
     * are sorted based on names given as argument. Columns not listed in argument
     * will be sorted after those within list, using existing ordering.
     *<p>
     * For example, schema that has columns:
     *<pre>"a", "d", "c", "b"
     *</pre>
     * ordered with <code>schema.sortedBy("a", "b");</code>
     * would result instance that columns in order:
     *<pre>"a", "b", "d", "c"
     *</pre>
     * 
     * @since 2.4
     */
    public CsvSchema sortedBy(String... columnNames)
    {
        LinkedHashMap<String,Column> map = new LinkedHashMap<String,Column>();
        for (String colName : columnNames) {
            Column col = _columnsByName.get(colName);
            if (col != null) {
                map.put(col.getName(), col);
            }
        }
        for (Column col : _columns) {
            map.put(col.getName(), col);
        }
        return new CsvSchema(this, map.values().toArray(new Column[map.size()]));
    }

    /**
     * Mutant factory method that will construct a new instance in which columns
     * are sorted using given {@link Comparator} over column names.
     * 
     * @since 2.4
     */
    public CsvSchema sortedBy(Comparator<String> cmp) {
        TreeMap<String,Column> map = new TreeMap<String,Column>(cmp);
        for (Column col : _columns) {
            map.put(col.getName(), col);
        }
        return new CsvSchema(this, map.values().toArray(new Column[map.size()]));
    }
    
    /*
    /**********************************************************************
    /* Public API, FormatSchema
    /**********************************************************************
     */

    @Override
    public String getSchemaType() {
        return "CSV";
    }

    /*
    /**********************************************************************
    /* Public API, extended, properties
    /**********************************************************************
     */

    public boolean usesHeader() { return (_features & ENCODING_FEATURE_USE_HEADER) != 0; }
    public boolean reordersColumns() { return (_features & ENCODING_FEATURE_REORDER_COLUMNS) != 0; }
    public boolean skipsFirstDataRow() { return (_features & ENCODING_FEATURE_SKIP_FIRST_DATA_ROW) != 0; }
    public boolean allowsComments() { return (_features & ENCODING_FEATURE_ALLOW_COMMENTS) != 0; }
    public boolean strictHeaders() { return (_features & ENCODING_FEATURE_STRICT_HEADERS) != 0; }

    /**
     * @deprecated Use {@link #usesHeader()} instead
     */
    @Deprecated // since 2.5
    public boolean useHeader() { return (_features & ENCODING_FEATURE_USE_HEADER) != 0; }

    /**
     * @deprecated Use {@link #skipsFirstDataRow()} instead
     */
    @Deprecated // since 2.5
    public boolean skipFirstDataRow() { return (_features & ENCODING_FEATURE_SKIP_FIRST_DATA_ROW) != 0; }
    
    public char getColumnSeparator() { return _columnSeparator; }
    public String getArrayElementSeparator() { return _arrayElementSeparator; }
    public int getQuoteChar() { return _quoteChar; }
    public int getEscapeChar() { return _escapeChar; }

    public char[] getLineSeparator() { return _lineSeparator; }

    /**
     * @return Null value defined, as char array, if one is defined to be recognized; Java null
     *    if not.
     * 
     * @since 2.5
     */
    public char[] getNullValue() { return _nullValue; }

    /**
     * Same as {@link #getNullValue()} except that undefined null value (one that remains as <code>null</code>,
     * or explicitly set as such) will be returned as empty <code>char[]</code>
     *
     * @since 2.6
     */
    public char[] getNullValueOrEmpty() {
        if (_nullValue == null) {
            return NO_CHARS;
        }
        return _nullValue;
    }

    /**
     * @since 2.6
     */
    public String getNullValueString() {
        String str = _nullValueAsString;
        if (str == null) {
            if (_nullValue == null) {
                return null;
            }
            str = (_nullValue.length == 0) ? "" : new String(_nullValue);
            _nullValueAsString = str;
        }
        return str;
    }

    public boolean usesQuoteChar() { return _quoteChar >= 0; }
    public boolean usesEscapeChar() { return _escapeChar >= 0; }

    /**
     * @since 2.5
     */
    public boolean hasArrayElementSeparator() { return !_arrayElementSeparator.isEmpty(); }

    /**
     * @since 2.7
     */
    public String getAnyPropertyName() { return _anyPropertyName; }

    /*
    /**********************************************************************
    /* Public API, extended; column access
    /**********************************************************************
     */
    
    @Override
    public Iterator<Column> iterator() {
        return Arrays.asList(_columns).iterator();
    }

    /**
     * Accessor for finding out how many columns this schema defines.
     *
     * @return Number of columns this schema defines
     */
    public int size() { return _columns.length; }

    /**
     * Accessor for column at specified index (0-based); index having to be within
     *<pre>
     *    0 &lt;= index &lt; size()
     *</pre>
     */
    public Column column(int index) {
        return _columns[index];
    }

    /**
     * @since 2.6
     */
    public String columnName(int index) {
        return _columns[index].getName();
    }
    
    public Column column(String name) {
        return _columnsByName.get(name);
    }

    /**
     * Optimized variant where a hint is given as to likely index of the column
     * name.
     *
     * @since 2.6
     */
    public Column column(String name, int probableIndex) {
        if (probableIndex < _columns.length) {
            Column col = _columns[probableIndex];
            if (col.hasName(name)) {
                return col;
            }
        }
        return _columnsByName.get(name);
    }
    
    /**
     * Method for getting description of column definitions in
     * developer-readable form
     */
    public String getColumnDesc()
    {
        StringBuilder sb = new StringBuilder(100);
        for (Column col : _columns) {
            if (sb.length() == 0) {
                sb.append('[');
            } else {
                sb.append(',');
            }
            sb.append('"');
            sb.append(col.getName());
            sb.append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* Other overrides
    /**********************************************************************
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(150);
        sb.append("[CsvSchema: ")
            .append("columns=[");
        boolean first = true;
        for (Column col : _columns) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append('"');
            sb.append(col.getName());
            sb.append("\"/");
            sb.append(col.getType());
        }
        sb.append(']');
        sb.append(", header? ").append(usesHeader());
        sb.append(", skipFirst? ").append(skipsFirstDataRow());
        sb.append(", comments? ").append(allowsComments());
        sb.append(", any-properties? ");
        String anyProp = getAnyPropertyName();
        if (anyProp == null) {
            sb.append("N/A");
        } else {
            sb.append("as '").append(anyProp).append("'");
        }
        
        sb.append(']');
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected static String _validArrayElementSeparator(String sep) {
        if (sep == null || sep.isEmpty()) {
            return NO_ARRAY_ELEMENT_SEPARATOR;
        }
        return sep;
    }
}
