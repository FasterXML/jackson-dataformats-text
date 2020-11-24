package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MapperBuilderState;

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
        }

        public Builder(StateImpl state) {
            super(state);
        }

        @Override
        public YAMLMapper build() {
            return new YAMLMapper(this);
        }

        @Override
        protected MapperBuilderState _saveState() {
            // nothing extra, just format features
            return new StateImpl(this);
        }

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        public Builder enable(YAMLParser.Feature... features) {
            for (YAMLParser.Feature f : features) {
                _formatReadFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(YAMLParser.Feature... features) {
            for (YAMLParser.Feature f : features) {
                _formatReadFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(YAMLParser.Feature feature, boolean state)
        {
            if (state) {
                _formatReadFeatures |= feature.getMask();
            } else {
                _formatReadFeatures &= ~feature.getMask();
            }
            return this;
        }

        public Builder enable(YAMLGenerator.Feature... features) {
            for (YAMLGenerator.Feature f : features) {
                _formatWriteFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(YAMLGenerator.Feature... features) {
            for (YAMLGenerator.Feature f : features) {
                _formatWriteFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(YAMLGenerator.Feature feature, boolean state)
        {
            if (state) {
                _formatWriteFeatures |= feature.getMask();
            } else {
                _formatWriteFeatures &= ~feature.getMask();
            }
            return this;
        }

        protected static class StateImpl extends MapperBuilderState
            implements java.io.Serializable // important!
        {
            private static final long serialVersionUID = 3L;
    
            public StateImpl(Builder src) {
                super(src);
            }
    
            // We also need actual instance of state as base class can not implement logic
             // for reinstating mapper (via mapper builder) from state.
            @Override
            protected Object readResolve() {
                return new Builder(this).build();
            }
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

    public static Builder builder() {
        return new Builder(new YAMLFactory());
    }

    public static Builder builder(YAMLFactory streamFactory) {
        return new Builder(streamFactory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder rebuild() {
        return new Builder((Builder.StateImpl) _savedBuilderState);
    }

    /*
    /**********************************************************************
    /* Life-cycle, shared "vanilla" (default configuration) instance
    /**********************************************************************
     */

    /**
     * Accessor method for getting globally shared "default" {@link YAMLMapper}
     * instance: one that has default configuration, no modules registered, no
     * config overrides. Usable mostly when dealing "untyped" or Tree-style
     * content reading and writing.
     */
    public static YAMLMapper shared() {
        return SharedWrapper.wrapped();
    }

    /*
    /**********************************************************************
    /* Life-cycle: JDK serialization support
    /**********************************************************************
     */

    // 27-Feb-2018, tatu: Not sure why but it seems base class definitions
    //   are not sufficient alone; sub-classes must re-define.
    @Override
    protected Object writeReplace() {
        return _savedBuilderState;
    }

    @Override
    protected Object readResolve() {
        throw new IllegalStateException("Should never deserialize `"+getClass().getName()+"` directly");
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

    /*
    /**********************************************************
    /* Helper class(es)
    /**********************************************************
     */

    /**
     * Helper class to contain dynamically constructed "shared" instance of
     * mapper, should one be needed via {@link #shared}.
     */
    private final static class SharedWrapper {
        private final static YAMLMapper MAPPER = YAMLMapper.builder().build();

        public static YAMLMapper wrapped() { return MAPPER; }
    }
}
