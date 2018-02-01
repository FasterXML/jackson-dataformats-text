package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convenience version of {@link ObjectMapper} which is configured
 * with {@link com.fasterxml.jackson.dataformat.yaml.YAMLFactory}.
 */
public class YAMLMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

    public YAMLMapper() { this(new YAMLFactory()); }

    public YAMLMapper(YAMLFactory f) {
        super(f);
    }

    public YAMLMapper(YAMLMapper base) {
        super(base);
    }

    @Override
    public YAMLMapper copy()
    {
        _checkInvalidCopy(YAMLMapper.class);
        return new YAMLMapper(this);
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Additional typed accessors
    /**********************************************************************
     */

    /**
     * Overridden with more specific type, since factory we have
     * is always of type {@link YAMLFactory}
     */
    @Override
    public final YAMLFactory tokenStreamFactory() {
        return (YAMLFactory) _streamFactory;
    }
}
