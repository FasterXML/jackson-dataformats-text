package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

//import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convenience version of {@link ObjectMapper} which is configured
 * with {@link com.fasterxml.jackson.dataformat.yaml.YAMLFactory}.
 */
public class YAMLMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * YAML backend.
     *
     * @since 3.0
     */
    public static class Builder extends MapperBuilder<YAMLMapper, Builder>
    {
        public Builder(YAMLMapper m) {
            super(m);
        }

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        // No Parser-features yet
        
        public Builder enable(YAMLGenerator.Feature... features) {
            for (YAMLGenerator.Feature f : features) {
                _mapper.enable(f);
            }
            return this;
        }

        public Builder disable(YAMLGenerator.Feature... features) {
            for (YAMLGenerator.Feature f : features) {
                _mapper.disable(f);
            }
            return this;
        }

        public Builder configure(YAMLGenerator.Feature f, boolean state)
        {
            if (state) {
                _mapper.enable(f);
            } else {
                _mapper.disable(f);
            }
            return this;
        }
    }
    
    public YAMLMapper() { this(new YAMLFactory()); }

    public YAMLMapper(YAMLFactory f) {
        super(f);
    }

    /**
     * @since 2.5
     */
    public YAMLMapper(YAMLMapper base) {
        super(base);
    }

    @SuppressWarnings("unchecked")
    public static YAMLMapper.Builder builder() {
        return new YAMLMapper.Builder(new YAMLMapper());
    }

    public static YAMLMapper.Builder builder(YAMLFactory streamFactory) {
        return new YAMLMapper.Builder(new YAMLMapper(streamFactory));
    }

    /**
     * @since 2.5
     */
    @Override
    public YAMLMapper copy()
    {
        _checkInvalidCopy(YAMLMapper.class);
        return new YAMLMapper(this);
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    public YAMLMapper configure(YAMLGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    public YAMLMapper configure(YAMLParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    public YAMLMapper enable(YAMLGenerator.Feature f) {
        ((YAMLFactory)_jsonFactory).enable(f);
        return this;
    }

    public YAMLMapper enable(YAMLParser.Feature f) {
        ((YAMLFactory)_jsonFactory).enable(f);
        return this;
    }

    public YAMLMapper disable(YAMLGenerator.Feature f) {
        ((YAMLFactory)_jsonFactory).disable(f);
        return this;
    }

    public YAMLMapper disable(YAMLParser.Feature f) {
        ((YAMLFactory)_jsonFactory).disable(f);
        return this;
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
    public final YAMLFactory getFactory() {
        return (YAMLFactory) _jsonFactory;
    }
}
