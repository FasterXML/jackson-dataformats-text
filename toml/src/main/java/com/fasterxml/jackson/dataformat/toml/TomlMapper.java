package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MapperBuilderState;

public class TomlMapper extends ObjectMapper {
    public static class Builder extends MapperBuilder<TomlMapper, Builder>
    {
        public Builder(TomlFactory f) {
            super(f);
        }

        public Builder(StateImpl state) {
            super(state);
        }

        @Override
        public TomlMapper build() {
            return new TomlMapper(this);
        }

        @Override
        protected MapperBuilderState _saveState() {
            return new StateImpl(this);
        }

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        public Builder enable(TomlReadFeature... features) {
            for (TomlReadFeature f : features) {
                _formatReadFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(TomlReadFeature... features) {
            for (TomlReadFeature f : features) {
                _formatReadFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(TomlReadFeature feature, boolean state)
        {
            if (state) {
                _formatReadFeatures |= feature.getMask();
            } else {
                _formatReadFeatures &= ~feature.getMask();
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

    public TomlMapper() {
        this(new TomlFactory());
    }

    public TomlMapper(TomlFactory f) {
        this(new Builder(f));
    }

    private TomlMapper(Builder b) {
        super(b);
    }

    public static Builder builder() {
        return new Builder(new TomlFactory());
    }

    public static Builder builder(TomlFactory streamFactory) {
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
     * Accessor method for getting globally shared "default" {@link TomlMapper}
     * instance: one that has default configuration, no modules registered, no
     * config overrides. Usable mostly when dealing "untyped" or Tree-style
     * content reading and writing.
     */
    public static TomlMapper shared() {
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
    /**********************************************************************
    /* Basic accessor overrides
    /**********************************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public TomlFactory tokenStreamFactory() {
        return (TomlFactory) _streamFactory;
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
        private final static TomlMapper MAPPER = TomlMapper.builder().build();

        public static TomlMapper wrapped() { return MAPPER; }
    }
}
