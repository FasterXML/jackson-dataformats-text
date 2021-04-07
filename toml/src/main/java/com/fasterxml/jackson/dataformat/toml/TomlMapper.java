package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

public class TomlMapper extends ObjectMapper {
    public static class Builder extends MapperBuilder<TomlMapper, Builder> {

        Builder(TomlMapper mapper) {
            super(mapper);
        }

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        public Builder enable(TomlReadFeature... features) {
            for (TomlReadFeature feature : features) {
                _mapper.enable(feature);
            }
            return this;
        }

        public Builder disable(TomlReadFeature... features) {
            for (TomlReadFeature feature : features) {
                _mapper.disable(feature);
            }
            return this;
        }

        public Builder configure(TomlReadFeature feature, boolean state)
        {
            _mapper.configure(feature, state);
            return this;
        }
    }

    public TomlMapper() {
        this(new TomlFactory());
    }

    public TomlMapper(TomlFactory f) {
        super(f);
    }

    public static Builder builder() {
        return new Builder(new TomlMapper());
    }

    public static Builder builder(TomlFactory streamFactory) {
        return new Builder(new TomlMapper(streamFactory));
    }

    /**
     * @since 2.5
     */
    @Override
    public TomlMapper copy() {
        _checkInvalidCopy(TomlMapper.class);
        return new TomlMapper(tokenStreamFactory().copy());
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    public TomlMapper configure(TomlReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    public TomlMapper enable(TomlReadFeature f) {
        ((TomlFactory) _jsonFactory).enable(f);
        return this;
    }

    public TomlMapper disable(TomlReadFeature f) {
        ((TomlFactory) _jsonFactory).disable(f);
        return this;
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
        return (TomlFactory) _jsonFactory;
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
