package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.MapperBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convenience version of {@link ObjectMapper} which is configured
 * with {@link com.fasterxml.jackson.dataformat.yaml.YAMLFactory}.
 */
public class YAMLMapper extends ObjectMapper
{
    private static final long serialVersionUID = 3L;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * YAML backend.
     *
     * @since 3.0
     */
    public static class Builder extends MapperBuilder<YAMLMapper, Builder>
    {
        public Builder(YAMLFactory f) {
            super(f);
            _formatGeneratorFeatures = f._formatGeneratorFeatures;
        }

        @Override
        public YAMLMapper build() {
            return new YAMLMapper(this);
        }

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        // No Parser-features yet
        
        public Builder enable(YAMLGenerator.Feature... features) {
            for (YAMLGenerator.Feature f : features) {
                _formatGeneratorFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(YAMLGenerator.Feature... features) {
            for (YAMLGenerator.Feature f : features) {
                _formatGeneratorFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(YAMLGenerator.Feature feature, boolean state)
        {
            if (state) {
                _formatGeneratorFeatures |= feature.getMask();
            } else {
                _formatGeneratorFeatures &= ~feature.getMask();
            }
            return this;
        }
    }

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public YAMLMapper() { this(new Builder(new YAMLFactory())); }

    public YAMLMapper(YAMLFactory f) {
        this(new Builder(f));
    }

    public YAMLMapper(Builder b) {
        super(b);
    }

    @SuppressWarnings("unchecked")
    public static Builder builder() {
        return new Builder(new YAMLFactory());
    }

    public static Builder builder(YAMLFactory streamFactory) {
        return new Builder(streamFactory);
    }

    /*
    /**********************************************************
    /* Basic accessor overrides
    /**********************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /**
     * Overridden with more specific type, since factory we have
     * is always of type {@link YAMLFactory}
     */
    @Override
    public final YAMLFactory tokenStreamFactory() {
        return (YAMLFactory) _streamFactory;
    }
}
