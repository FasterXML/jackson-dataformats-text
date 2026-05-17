package tools.jackson.dataformat.toml;

import java.math.BigDecimal;

import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.cfg.MapperBuilderState;
import tools.jackson.databind.deser.DeserializationContextExt;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.util.NumberUtil;

public class TomlMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

    public static class Builder extends MapperBuilder<TomlMapper, Builder>
    {
        public Builder(TomlFactory f) {
            super(f);

            enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            _coercionConfigs.defaultCoercions().setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
            nodeFactory(TomlNodeFactory.INSTANCE);
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

        public Builder enable(TomlWriteFeature... features) {
            for (TomlWriteFeature feature : features) {
                _formatWriteFeatures |= feature.getMask();
            }
            return this;
        }

        public Builder disable(TomlWriteFeature... features) {
            for (TomlWriteFeature feature : features) {
                _formatWriteFeatures &= ~feature.getMask();
            }
            return this;
        }

        public Builder configure(TomlWriteFeature feature, boolean state)
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

    public TomlMapper() {
        this(new TomlFactory());
    }

    public TomlMapper(TomlFactory f) {
        this(new Builder(f));
    }

    TomlMapper(Builder b) {
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
    /**********************************************************************
    /* Tree reading: short-circuit to preserve TOML-specific node metadata
    /**********************************************************************
     */

    /**
     * Override to bypass the default deserializer-driven tree reconstruction
     * when the parser is one we created ourselves. {@link TomlFactory} wraps
     * the parsed tree in a {@link TomlTreeTraversingParser}; re-walking those
     * tokens through {@code JsonNodeDeserializer} would build a fresh tree
     * via the node factory, dropping the {@code fromTableHeader} /
     * {@code fromArrayOfTables} flags. Externally supplied
     * {@code TreeTraversingParser} instances are *not* short-circuited: the
     * caller may have positioned them at a sub-token, and we must not mutate
     * their source tree. A user-overridden {@link JsonNodeFactory} also
     * disables the short-circuit, since the parser-built tree uses
     * {@link TomlNodeFactory#INSTANCE} and would otherwise silently override
     * the caller's factory choice.
     */
    @Override
    protected JsonNode _readTreeAndClose(DeserializationContextExt ctxt,
            JsonParser p0) throws tools.jackson.core.JacksonException {
        if (p0 instanceof TomlTreeTraversingParser
                && ctxt.getNodeFactory() == TomlNodeFactory.INSTANCE) {
            try (JsonParser p = ctxt.assignAndReturnParser(p0)) {
                Object src = p.streamReadInputSource();
                if (src instanceof JsonNode) {
                    JsonNode node = (JsonNode) src;
                    // Bypassing JsonNodeDeserializer means we also bypass the strip
                    // it would have applied; do it here when the feature is on.
                    if (ctxt.isEnabled(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)) {
                        node = _stripTrailingBigDecimalZeroes(node, ctxt.getNodeFactory());
                    }
                    return node;
                }
            }
        }
        return super._readTreeAndClose(ctxt, p0);
    }

    private static JsonNode _stripTrailingBigDecimalZeroes(JsonNode node, JsonNodeFactory nf) {
        if (node.isObject()) {
            ObjectNode on = (ObjectNode) node;
            for (java.util.Map.Entry<String, JsonNode> en : on.properties()) {
                JsonNode replaced = _stripTrailingBigDecimalZeroes(en.getValue(), nf);
                if (replaced != en.getValue()) {
                    on.set(en.getKey(), replaced);
                }
            }
            return on;
        }
        if (node.isArray()) {
            ArrayNode an = (ArrayNode) node;
            for (int i = 0, n = an.size(); i < n; ++i) {
                JsonNode orig = an.get(i);
                JsonNode replaced = _stripTrailingBigDecimalZeroes(orig, nf);
                if (replaced != orig) {
                    an.set(i, replaced);
                }
            }
            return an;
        }
        if (node.isBigDecimal()) {
            BigDecimal v = node.decimalValue();
            // 0.0.stripTrailingZeros() is 0E-1, not BigDecimal.ZERO; normalize to avoid scale surprise.
            BigDecimal stripped = v.signum() == 0 ? BigDecimal.ZERO : NumberUtil.stripTrailingZeros(v);
            if (stripped.equals(v) && stripped.scale() == v.scale()) {
                return node;
            }
            return nf.numberNode(stripped);
        }
        return node;
    }

    /*
    /**********************************************************************
    /* Format-specific
    /**********************************************************************
     */

    public boolean isEnabled(TomlReadFeature f) {
        return _deserializationConfig.hasFormatFeature(f);
    }

    public boolean isEnabled(TomlWriteFeature f) {
        return _serializationConfig.hasFormatFeature(f);
    }
    
    /*
    /**********************************************************************
    /* Helper class(es)
    /**********************************************************************
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
