package com.fasterxml.jackson.dataformat.javaprop.io;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropNode;

/**
 * Helper class used to keep track of traversal over contents of
 * content tree expressed as {@link JPropNode}s.
 */
public abstract class JPropReadContext
    extends JsonStreamContext
{
    /**
     * Parent cursor of this cursor, if any; null for root
     * cursors.
     */
    protected final JPropReadContext _parent;

    /**
     * Current field name
     */
    protected String _currentName;

    protected String _currentText;
    
    /**
     * Java-level Object that corresponds to this level of input hierarchy,
     * if any; used by databinding functionality, opaque for parser.
     */
    protected java.lang.Object _currentValue;

    /**
     * We need to keep track of value nodes to construct further contexts.
     */
    protected JPropNode _nextNode;

    /**
     * Optional "this" value for cases where path branches have
     * direct values; these are exposed before child values
     * with bogus 'name' of empty String.
     */
    protected String _branchText;

    protected int _state;
    
    public JPropReadContext(int contextType, JPropReadContext p, JPropNode node)
    {
        super();
        _type = contextType;
        _index = -1;
        _parent = p;
        _branchText = node.getValue();
    }

    public static JPropReadContext create(JPropNode root) {
        if (root.isArray()) { // can this ever occur?
            return new ArrayContext(null, root);
        }
        return new ObjectContext(null, root);
    }

    /*
    /**********************************************************
    /* JsonStreamContext impl
    /**********************************************************
     */

    // note: co-variant return type
    @Override
    public final JPropReadContext getParent() { return _parent; }

    @Override
    public final String getCurrentName() {
        return _currentName;
    }

    public void overrideCurrentName(String name) {
        _currentName = name;
    }

    @Override
    public java.lang.Object getCurrentValue() {
        return _currentValue;
    }

    @Override
    public void setCurrentValue(java.lang.Object v) {
        _currentValue = v;
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public abstract JsonToken nextToken();

    /**
     * Method called to figure out child or parent context when change
     * is needed, as indicated by this context returning `null`.
     */
    public JPropReadContext nextContext()
    {
        JPropNode n = _nextNode;
        if (n == null) {
            return _parent;
        }
        _nextNode = null;
        if (n.isArray()) {
            return new ArrayContext(this, n);
        }
        return new ObjectContext(this, n);
    }

    public String getCurrentText() {
        return _currentText;
    }

    /*
    /**********************************************************
    /* Concrete implementations
    /**********************************************************
     */

    /**
     * Cursor used for traversing non-empty JSON Array nodes
     */
    protected final static class ArrayContext
        extends JPropReadContext
    {
        final static int STATE_START = 0; // before START_ARRAY
        final static int STATE_BRANCH_VALUE = 1;
        final static int STATE_CONTENT_VALUE = 2;
        final static int STATE_END = 3; // after END_ARRAY
      
        protected Iterator<JPropNode> _contents;

        public ArrayContext(JPropReadContext p, JPropNode arrayNode) {
            super(JsonStreamContext.TYPE_ARRAY, p, arrayNode);
            _contents = arrayNode.arrayContents();
            _state = STATE_START;
        }

        @Override
        public JsonToken nextToken()
        {
            switch (_state) {
            case STATE_START: // START_ARRAY
                _state = (_branchText == null) ? STATE_CONTENT_VALUE : STATE_BRANCH_VALUE;
                return JsonToken.START_ARRAY;
            case STATE_BRANCH_VALUE:
                _state = STATE_CONTENT_VALUE;
                _currentText = _branchText;
                return JsonToken.VALUE_STRING;
            case STATE_CONTENT_VALUE:
                if (!_contents.hasNext()) {
                    _state = STATE_END;
                    return JsonToken.END_ARRAY;
                }
                JPropNode n = _contents.next();
                if (n.isLeaf()) {
                    _currentText = n.getValue();
                    return JsonToken.VALUE_STRING;
                }
                _nextNode = n;
                // Structured; need to indicate indirectly
                return null;
                
            case STATE_END:
            default:
            }
            return null;
        }
    }

    /**
     * Cursor used for traversing non-empty JSON Object nodes
     */
    protected final static class ObjectContext
        extends JPropReadContext
    {
        final static int STATE_START = 0; // before START_OBJECT
        final static int STATE_BRANCH_KEY = 1; // if (and only if) we have intermediate node ("branch") value
        final static int STATE_BRANCH_VALUE = 2;
        final static int STATE_CONTENT_KEY = 3;
        final static int STATE_CONTENT_VALUE = 4;
        final static int STATE_END = 5; // after END_OBJECT
        
        /**
         * Iterator over child values.
         */
        protected Iterator<Map.Entry<String, JPropNode>> _contents;

        public ObjectContext(JPropReadContext p, JPropNode objectNode)
        {
            super(JsonStreamContext.TYPE_OBJECT, p, objectNode);
            _contents = objectNode.objectContents();
            _state = STATE_START;
        }

        @Override
        public JsonToken nextToken()
        {
            switch (_state) {
            case STATE_START: // START_OBJECT
                _state = (_branchText == null) ? STATE_CONTENT_KEY : STATE_BRANCH_KEY;
                return JsonToken.START_OBJECT;
            case STATE_BRANCH_KEY:
                _currentName = "";
                _state = STATE_BRANCH_VALUE;
                return JsonToken.FIELD_NAME;
            case STATE_BRANCH_VALUE:
                _currentText = _branchText;
                _state = STATE_CONTENT_KEY;
                return JsonToken.VALUE_STRING;
            case STATE_CONTENT_KEY:
                if (!_contents.hasNext()) {
                    _state = STATE_END;
                    _nextNode = null;
                    return JsonToken.END_OBJECT;
                }
                Map.Entry<String, JPropNode> entry = _contents.next();
                _currentName = entry.getKey();
                _nextNode = entry.getValue();
                _state = STATE_CONTENT_VALUE;
                return JsonToken.FIELD_NAME;
            case STATE_CONTENT_VALUE:
                _state = STATE_CONTENT_KEY;
                // Simple textual leaf?
                if (_nextNode.isLeaf()) {
                    _currentText = _nextNode.getValue();
                    _nextNode = null;
                    return JsonToken.VALUE_STRING;
                }
                // Structured; need to indicate indirectly
                return null;
                
            case STATE_END:
            default:
            }
            return null;
        }
    }
}
