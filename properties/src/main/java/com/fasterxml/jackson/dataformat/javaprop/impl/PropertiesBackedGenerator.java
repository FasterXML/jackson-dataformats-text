package com.fasterxml.jackson.dataformat.javaprop.impl;

import java.io.*;
import java.util.Properties;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsGenerator;

public class PropertiesBackedGenerator extends JavaPropsGenerator
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Underlying {@link Properties} that we will update with logical
     * properties written out.
     */
    final protected Properties _props;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public PropertiesBackedGenerator(IOContext ctxt, Properties props,
            int stdFeatures, ObjectCodec codec)
    {
        super(ctxt, stdFeatures, codec);
        _props = props;
    }

    /*
    /**********************************************************
    /* Overridden methods, configuration
    /**********************************************************
     */

    @Override
    public Object getOutputTarget() {
        return _props;
    }

    /*
    /**********************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************
     */

    @Override
    public void close() throws IOException { }

    @Override
    public void flush() throws IOException { }

    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */

    @Override
    protected void _releaseBuffers() { }

    /*
    /**********************************************************
    /* Internal methods; escaping writes
    /**********************************************************
     */

    @Override
    protected void _writeEscapedEntry(char[] text, int offset, int len) throws IOException {
        _writeEscapedEntry(new String(text, offset, len));
    }
    
    @Override
    protected void _writeEscapedEntry(String value) throws IOException
    {
        _props.put(_basePath.toString(), value);
    }

    @Override
    protected void _writeUnescapedEntry(String value) throws IOException
    {
        _props.put(_basePath.toString(), value);
    }

    /*
    /**********************************************************
    /* Internal methods; raw writes
    /**********************************************************
     */

    /* 02-Jun-2016, tatu: no way to support raw writes, so two things we
     *   could do instead: throw exception, or just quietly ignore. Typically
     *   I favor throwing exception, but here it's probably better to simply
     *   ignore.
     */
    
    @Override
    protected void _writeRaw(char c) throws IOException
    {
    }

    @Override
    protected void _writeRaw(String text) throws IOException
    {
    }

    @Override
    protected void _writeRaw(StringBuilder text) throws IOException
    {
    }

    @Override
    protected void _writeRaw(char[] text, int offset, int len) throws IOException
    {
    }

    protected void _writeRawLong(String text) throws IOException
    {
    }

    protected void _writeRawLong(StringBuilder text) throws IOException
    {
    }
}
