package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;

/**
 * @author Petar Tahchiev
 * @since 2.0.1
 */
public class CsvWriteContext extends JsonStreamContext {

    protected final CsvWriteContext _parent;

    protected CsvWriteContext _child = null;

    protected String _currentName;

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

    protected int _basePathLength;

    protected CsvWriteContext(int type, CsvWriteContext parent,
                                int basePathLength)
    {
        super();
        _type = type;
        _parent = parent;
        _basePathLength = basePathLength;
        _index = -1;
    }


    /*
    /**********************************************************
    /* State changes
    /**********************************************************
     */

    public boolean writeFieldName(String name) throws JsonProcessingException {
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

    public static CsvWriteContext createRootContext() {
        return new CsvWriteContext(TYPE_ROOT, null, 0);
    }


    public CsvWriteContext createChildArrayContext(int basePathLength) {
        CsvWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new CsvWriteContext(TYPE_ARRAY, this, basePathLength);
            return ctxt;
        }
        ctxt.reset(TYPE_ARRAY, basePathLength);
        return ctxt;
    }

    public CsvWriteContext createChildObjectContext(int basePathLength) {
        CsvWriteContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new CsvWriteContext(TYPE_OBJECT, this, basePathLength);
            return ctxt;
        }
        ctxt.reset(TYPE_OBJECT, basePathLength);
        return ctxt;
    }

    private void reset(int type, int basePathLength) {
        _type = type;
        _basePathLength = basePathLength;
        _currentValue = null;
        _index = -1;
    }

    @Override
    public CsvWriteContext getParent() {
        return _parent;
    }

    @Override
    public String getCurrentName() {
        return _currentName;
    }
}
