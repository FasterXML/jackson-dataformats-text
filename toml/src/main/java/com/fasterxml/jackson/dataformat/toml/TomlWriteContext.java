package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonStreamContext;

final class TomlWriteContext extends JsonStreamContext {
    /**
     * Parent context for this context; null for root context.
     */
    protected final TomlWriteContext _parent;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speed up things
    /* a bit (10-15%) for docs with lots of small arrays/objects
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

    boolean _inline;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    TomlWriteContext(int type, TomlWriteContext parent,
                                Object currValue, int basePathLength)
    {
        super();
        _type = type;
        _parent = parent;
        _basePathLength = basePathLength;
        _index = -1;
        _currentValue = currValue;
        _inline = (parent != null && parent._inline) || type == TYPE_ARRAY;
    }

    private void reset(int type, Object currValue, int basePathLength) {
        _type = type;
        _basePathLength = basePathLength;
        _currentValue = null;
        _index = -1;
        _currentValue = currValue;
    }

    // // // Factory methods

    static TomlWriteContext createRootContext() {
        return new TomlWriteContext(TYPE_ROOT, null, null, 0);
    }

    static TomlWriteContext createRootContext(int basePathLength) {
        return new TomlWriteContext(TYPE_ROOT, null, null, basePathLength);
    }

    public TomlWriteContext createChildArrayContext(Object currValue, int basePathLength) {
        TomlWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TomlWriteContext(TYPE_ARRAY, this, currValue, basePathLength);
            return ctxt;
        }
        ctxt.reset(TYPE_ARRAY, currValue, basePathLength);
        return ctxt;
    }

    public TomlWriteContext createChildObjectContext(Object currValue, int basePathLength) {
        TomlWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TomlWriteContext(TYPE_OBJECT, this, currValue, basePathLength);
            return ctxt;
        }
        ctxt.reset(TYPE_OBJECT, currValue, basePathLength);
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
    public String getCurrentName() {
        return _currentName;
    }

    @Override
    public Object getCurrentValue() {
        return _currentValue;
    }

    @Override
    public void setCurrentValue(Object v) {
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
