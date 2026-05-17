package tools.jackson.dataformat.toml;

import tools.jackson.core.TokenStreamContext;

final class TomlWriteContext extends TokenStreamContext {
    /**
     * TOML-specific output mode for a context. The base class {@code _type}
     * still tracks ROOT/OBJECT/ARRAY for the {@link TokenStreamContext}
     * contract; this enum is layered on top to control how the generator
     * renders the value.
     */
    enum Kind {
        /** Top-level document. */
        ROOT,
        /** Object rendered as dotted keys ({@code foo.bar = 1}). */
        DOTTED_OBJECT,
        /** Object rendered as inline table ({@code foo = {bar = 1}}). */
        INLINE_OBJECT,
        /** Array rendered inline ({@code foo = [1, 2]}). */
        INLINE_ARRAY,
        /** Object rendered as standard table ({@code [foo]} header). */
        TABLE,
        /** Array rendered as array of tables ({@code [[foo]]} headers). */
        ARRAY_OF_TABLES
    }

    /**
     * Parent context for this context; null for root context.
     */
    protected final TomlWriteContext _parent;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speed up things a bit
    /**********************************************************************
     */

    protected TomlWriteContext _child = null;

    /*
    /**********************************************************************
    /* Location/state information (minus source reference)
    /**********************************************************************
     */

    /**
     * Value that is being serialized and caused this context to be created;
     * typically a POJO or container type.
     */
    protected Object _currentValue;

    /**
     * Marker used to indicate that we just received a name, and
     * now expect a value
     */
    protected boolean _gotName;

    /**
     * Name of the field of which value is to be parsed; only
     * used for OBJECT contexts
     */
    protected String _currentName;

    protected int _basePathLength;

    Kind _kind;

    /**
     * Set true once the first sub-table (or array-of-tables) has been opened
     * within this context, locking out further scalar / dotted-key writes at
     * this level. Enforces the strict-ordering rule required for table syntax.
     */
    boolean _scalarsClosed;

    /**
     * For TABLE and ARRAY_OF_TABLES contexts: snapshot of the full base path
     * (e.g. {@code "server.database"}) at the moment the context was opened,
     * so it can be restored when the context closes. Inside the table itself
     * the generator's {@code _basePath} is reset to empty so that keys are
     * written relative to the section header.
     */
    String _savedPath;

    /**
     * For TABLE and ARRAY_OF_TABLES contexts: the contents of the
     * generator's {@code _basePath} at the moment this section opened
     * (i.e. the parent's relative-key prefix). Restored on close so that
     * sibling writes after the section see the parent's prefix again.
     * {@code null} when the parent had no buffered prefix.
     */
    String _savedOuterBasePath;

    /**
     * Absolute section path of the nearest enclosing TABLE or
     * ARRAY_OF_TABLES context, or {@code ""} for the document root. Computed
     * once at context creation by inheriting from the parent, so opening a
     * new section is O(1) instead of walking the context chain on every
     * header.
     */
    String _enclosingSectionPath = "";

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    TomlWriteContext(int type, TomlWriteContext parent,
            Object currValue, int basePathLength, Kind kind)
    {
        super();
        _type = type;
        _parent = parent;
        _nestingDepth = parent == null ? 0 : parent._nestingDepth + 1;
        _basePathLength = basePathLength;
        _index = -1;
        _currentValue = currValue;
        _kind = kind;
        _scalarsClosed = false;
        _enclosingSectionPath = _inheritEnclosingPath(parent);
    }

    private void reset(int type, Object currValue, int basePathLength, Kind kind) {
        _type = type;
        _basePathLength = basePathLength;
        _currentValue = currValue;
        _index = -1;
        _gotName = false;
        _currentName = null;
        _kind = kind;
        _scalarsClosed = false;
        _enclosingSectionPath = _inheritEnclosingPath(_parent);
        _savedPath = null;
        _savedOuterBasePath = null;
    }

    private static String _inheritEnclosingPath(TomlWriteContext parent) {
        if (parent == null) {
            return "";
        }
        if (parent._kind == Kind.TABLE || parent._kind == Kind.ARRAY_OF_TABLES) {
            return parent._savedPath == null ? "" : parent._savedPath;
        }
        return parent._enclosingSectionPath;
    }

    boolean isInline() {
        return _kind == Kind.INLINE_OBJECT || _kind == Kind.INLINE_ARRAY;
    }

    // // // Factory methods

    static TomlWriteContext createRootContext() {
        return new TomlWriteContext(TYPE_ROOT, null, null, 0, Kind.ROOT);
    }

    static TomlWriteContext createRootContext(int basePathLength) {
        return new TomlWriteContext(TYPE_ROOT, null, null, basePathLength, Kind.ROOT);
    }

    public TomlWriteContext createChildArrayContext(Object currValue, int basePathLength) {
        return createChildArrayContext(currValue, basePathLength, Kind.INLINE_ARRAY);
    }

    public TomlWriteContext createChildArrayContext(Object currValue, int basePathLength, Kind kind) {
        TomlWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TomlWriteContext(TYPE_ARRAY, this, currValue, basePathLength, kind);
            return ctxt;
        }
        ctxt.reset(TYPE_ARRAY, currValue, basePathLength, kind);
        return ctxt;
    }

    public TomlWriteContext createChildObjectContext(Object currValue, int basePathLength) {
        // Inline parents force inline children; the contract has no way to
        // emit a [section] header from inside { } scope.
        Kind kind = isInline() ? Kind.INLINE_OBJECT : Kind.DOTTED_OBJECT;
        return createChildObjectContext(currValue, basePathLength, kind);
    }

    public TomlWriteContext createChildObjectContext(Object currValue, int basePathLength, Kind kind) {
        TomlWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TomlWriteContext(TYPE_OBJECT, this, currValue, basePathLength, kind);
            return ctxt;
        }
        ctxt.reset(TYPE_OBJECT, currValue, basePathLength, kind);
        return ctxt;
    }

    /*
    /**********************************************************************
    /* State changes
    /**********************************************************************
     */

    public boolean writeName(String name)
    {
        if (_gotName) {
            return false;
        }
        _gotName = true;
        _currentName = name;
        return true;
    }

    public boolean writeValue() {
        // Most likely, object:
        if (_type == TYPE_OBJECT) {
            if (!_gotName) {
                return false;
            }
            _gotName = false;
        }
        // Array fine, and must allow root context for Object values too so...
        ++_index;
        return true;
    }

    public void truncatePath(StringBuilder sb) {
        int len = sb.length();
        if (len != _basePathLength) {
            if (len < _basePathLength) { // sanity check
                throw new IllegalStateException(String.format
                        ("Internal error: base path length %d, buffered %d, trying to truncate",
                                _basePathLength, len));
            }
            sb.setLength(_basePathLength);
        }
    }

    /*
    /**********************************************************************
    /* Simple accessors, mutators
    /**********************************************************************
     */

    @Override
    public final TomlWriteContext getParent() { return _parent; }

    @Override
    public String currentName() {
        return _currentName;
    }

    @Override
    public Object currentValue() {
        return _currentValue;
    }

    @Override
    public void assignCurrentValue(Object v) {
        _currentValue = v;
    }

    public StringBuilder appendDesc(StringBuilder sb) {
        if (_parent != null) {
            sb = _parent.appendDesc(sb);
            sb.append('/');
        }
        switch (_type) {
            case TYPE_OBJECT:
                if (_currentName != null) {
                    sb.append(_currentName);
                }
                break;
            case TYPE_ARRAY:
                sb.append(getCurrentIndex());
                break;
            case TYPE_ROOT:
        }
        return sb;
    }

    // // // Overridden standard methods

    /**
     * Overridden to provide developer JsonPointer representation
     * of the context.
     */
    @Override
    public final String toString() {
        return appendDesc(new StringBuilder(64)).toString();
    }
}
