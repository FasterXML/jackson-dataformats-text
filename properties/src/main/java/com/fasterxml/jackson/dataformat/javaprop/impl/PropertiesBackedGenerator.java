package com.fasterxml.jackson.dataformat.javaprop.impl;

import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsGenerator;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;

public class PropertiesBackedGenerator extends JavaPropsGenerator
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Underlying {@link Properties} that we will update with logical
     * properties written out.
     */
    protected final Map<String, Object> _content;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public PropertiesBackedGenerator(ObjectWriteContext writeCtxt, IOContext ctxt,
            int stdFeatures, JavaPropsSchema schema,
            Map<?,?> content)
    {
        super(writeCtxt, ctxt, stdFeatures, schema);
        _content = (Map<String, Object>) content;
        // Since this is not physically encoding properties, should NOT try
        // to attempt writing headers. Easy way is to just fake we already did it
        _headerChecked = true;
    }

    /*
    /**********************************************************************
    /* Overridden methods, configuration
    /**********************************************************************
     */

    @Override
    public Object streamWriteTarget() {
        return _content;
    }

    @Override
    public int streamWriteOutputBuffered() { return -1; }

    /*
    /**********************************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************************
     */

    @Override
    public void close() { }

    @Override
    public void flush() { }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */

    @Override
    protected void _releaseBuffers() { }

    @Override
    protected void _appendPropertyName(StringBuilder path, String name) {
        // No escaping should be applied
        path.append(name);
    }
    
    /*
    /**********************************************************************
    /* Internal methods; escaping writes
    /**********************************************************************
     */

    @Override
    protected void _writeEscapedEntry(char[] text, int offset, int len) throws JacksonException {
        _writeEscapedEntry(new String(text, offset, len));
    }
    
    @Override
    protected void _writeEscapedEntry(String value) throws JacksonException
    {
        _content.put(_basePath.toString(), value);
    }

    @Override
    protected void _writeUnescapedEntry(String value) throws JacksonException
    {
        _content.put(_basePath.toString(), value);
    }

    /*
    /**********************************************************************
    /* Internal methods; raw writes
    /**********************************************************************
     */

    /* 02-Jun-2016, tatu: no way to support raw writes, so two things we
     *   could do instead: throw exception, or just quietly ignore. Typically
     *   I favor throwing exception, but here it's probably better to simply
     *   ignore.
     */
    
    @Override
    protected void _writeRaw(char c) throws JacksonException
    {
    }

    @Override
    protected void _writeRaw(String text) throws JacksonException
    {
    }

    @Override
    protected void _writeRaw(StringBuilder text) throws JacksonException
    {
    }

    @Override
    protected void _writeRaw(char[] text, int offset, int len) throws JacksonException
    {
    }

    protected void _writeRawLong(String text) throws JacksonException
    {
    }

    protected void _writeRawLong(StringBuilder text) throws JacksonException
    {
    }
}
