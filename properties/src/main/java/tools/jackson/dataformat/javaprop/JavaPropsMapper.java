package tools.jackson.dataformat.javaprop;

import java.io.IOException;
import java.util.*;

import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.ser.SerializationContextExt;

public class JavaPropsMapper extends ObjectMapper
{
    private static final long serialVersionUID = 3L;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * Java Properties backend.
     *
     * @since 3.0
     */
    public static class Builder extends MapperBuilder<JavaPropsMapper, Builder>
    {
        public Builder(JavaPropsFactory f) {
            super(f);
            // 09-Apr-2021, tatu: [dataformats-text#255]: take empty String to
            //    mean `null` where applicable; also accept Blank same way
            enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

            _coercionConfigs.defaultCoercions()
                .setAcceptBlankAsEmpty(Boolean.TRUE)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty)
            ;
        }

        public Builder(StateImpl state) {
            super(state);
        }

        @Override
        public JavaPropsMapper build() {
            return new JavaPropsMapper(this);
        }

        @Override
        protected MapperBuilderState _saveState() {
            return new StateImpl(this);
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
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public JavaPropsMapper() {
        this(new JavaPropsFactory());
    }

    public JavaPropsMapper(JavaPropsFactory f) {
        this(new Builder(f));
    }

    public JavaPropsMapper(Builder b) {
        super(b);
    }

    public static Builder builder() {
        return new Builder(new JavaPropsFactory());
    }

    public static Builder builder(JavaPropsFactory streamFactory) {
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
     * Accessor method for getting globally shared "default" {@link JavaPropsMapper}
     * instance: one that has default configuration, no modules registered, no
     * config overrides. Usable mostly when dealing "untyped" or Tree-style
     * content reading and writing.
     */
    public static JavaPropsMapper shared() {
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
    public JavaPropsFactory tokenStreamFactory() {
        return (JavaPropsFactory) _streamFactory;
    }

    /*
    /**********************************************************************
    /* Extended read methods, from Properties objects
    /**********************************************************************
     */

    /**
     * Convenience method which uses given `Properties` as the source
     * as if they had been read from an external source, processes
     * them (splits paths etc), and then binds as given result
     * value.
     *<p>
     * Note that this is NOT identical to calling {@link #convertValue(Object, Class)};
     * rather, it would be similar to writing `Properties` out into a File,
     * then calling `readValue()` on this mapper to bind contents.
     */
    @SuppressWarnings("resource")
    public <T> T readPropertiesAs(Properties props, JavaPropsSchema schema,
            Class<T> valueType) throws IOException
    {
        DeserializationContext ctxt = _deserializationContext();
        JsonParser p = tokenStreamFactory().createParser(ctxt, schema, props);
        return (T) readValue(p, valueType);
    }

    /**
     * Convenience method which uses given `Properties` as the source
     * as if they had been read from an external source, processes
     * them (splits paths etc), and then binds as given result
     * value.
     *<p>
     * Note that this is NOT identical to calling {@link #convertValue(Object, Class)};
     * rather, it would be similar to writing `Properties` out into a File,
     * then calling `readValue()` on this mapper to bind contents.
     */
    @SuppressWarnings({ "resource", "unchecked" })
    public <T> T readPropertiesAs(Properties props, JavaPropsSchema schema,
            JavaType valueType) throws IOException
    {
        DeserializationContext ctxt = _deserializationContext();
        JsonParser p = tokenStreamFactory().createParser(ctxt, schema, props);
        return (T) readValue(p, valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     */
    public <T> T readPropertiesAs(Properties props, Class<T> valueType) throws IOException {
        return readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     */
    public <T> T readPropertiesAs(Properties props, JavaType valueType) throws IOException {
        return readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
    }

    /*
    /**********************************************************************
    /* Extended read methods, from Map objects
    /**********************************************************************
     */

    /**
     * Convenience method which uses given `Properties` as the source
     * as if they had been read from an external source, processes
     * them (splits paths etc), and then binds as given result
     * value.
     *<p>
     * Note that this is NOT identical to calling {@link #convertValue(Object, Class)};
     * rather, it would be similar to writing `Properties` out into a File,
     * then calling `readValue()` on this mapper to bind contents.
     */
    @SuppressWarnings("resource")
    public <T> T readMapAs(Map<String, String> map, JavaPropsSchema schema,
            Class<T> valueType) throws IOException {
        DeserializationContext ctxt = _deserializationContext();
        JsonParser p = tokenStreamFactory().createParser(ctxt, schema, map);
        return (T) readValue(p, valueType);
    }

    /**
     * Convenience method which uses given `Properties` as the source
     * as if they had been read from an external source, processes
     * them (splits paths etc), and then binds as given result
     * value.
     *<p>
     * Note that this is NOT identical to calling {@link #convertValue(Object, Class)};
     * rather, it would be similar to writing `Properties` out into a File,
     * then calling `readValue()` on this mapper to bind contents.
     */
    @SuppressWarnings({ "resource", "unchecked" })
    public <T> T readMapAs(Map<String, String> map, JavaPropsSchema schema,
            JavaType valueType) throws IOException {
        DeserializationContext ctxt = _deserializationContext();
        JsonParser p = tokenStreamFactory().createParser(ctxt, schema, map);
        return (T) readValue(p, valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     */
    public <T> T readMapAs(Map<String, String> map, Class<T> valueType) throws IOException {
        return readMapAs(map, JavaPropsSchema.emptySchema(), valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     */
    public <T> T readMapAs(Map<String, String> map, JavaType valueType) throws IOException {
        return readMapAs(map, JavaPropsSchema.emptySchema(), valueType);
    }
    
    /*
    /**********************************************************************
    /* Extended read methods, from System Properties
    /**********************************************************************
     */

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(System.getProperties(), schema, valueType);
     *</pre>
     */
    public <T> T readSystemPropertiesAs(JavaPropsSchema schema, 
            Class<T> valueType) throws IOException {
        return readPropertiesAs(System.getProperties(), schema, valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(System.getProperties(), schema, valueType);
     *</pre>
     */
    public <T> T readSystemPropertiesAs(JavaPropsSchema schema,
            JavaType valueType) throws IOException {
        return readPropertiesAs(System.getProperties(), schema, valueType);
    }

    /*
    /**********************************************************************
    /* Extended read methods, from Env variables
    /**********************************************************************
     */
    
    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(convertMapToProperties(System.getenv()), schema, valueType);
     *</pre>
     */
    public <T> T readEnvVariablesAs(JavaPropsSchema schema, 
            Class<T> valueType) throws IOException {
        return readPropertiesAs(_env(), schema, valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(convertMapToProperties(System.getenv()), schema, valueType);
     *</pre>
     */
    public <T> T readEnvVariablesAs(JavaPropsSchema schema,
            JavaType valueType) throws IOException {
        return readPropertiesAs(_env(), schema, valueType);
    }

    protected Properties _env() {
        Properties props = new Properties();
        props.putAll(System.getenv());
        return props;
    }

    /*
    /**********************************************************************
    /* Extended write methods
    /**********************************************************************
     */

    /**
     * Convenience method that "writes" given `value` as properties
     * in given {@link Map} object.
     */
    public void writeValue(Map<?,?> target, Object value) throws IOException
    {
        if (target == null) {
            throw new IllegalArgumentException("Can not pass `null` target");
        }
        SerializationContextExt prov = _serializationContext();
        try (JavaPropsGenerator g = tokenStreamFactory().createGenerator(prov, null, target)) {
            writeValue(g, value);
        }
    }

    /**
     * Convenience method that "writes" given `value` as properties
     * in given {@link Map} object.
     */
    public void writeValue(Map<?,?> target, Object value, JavaPropsSchema schema)
            throws IOException
    {
        if (target == null) {
            throw new IllegalArgumentException("Can not pass `null` target");
        }
        SerializationContextExt prov = _serializationContext();
        try (JavaPropsGenerator g = tokenStreamFactory().createGenerator(prov, schema, target)) {
            writeValue(g, value);
        }
    }

    /**
     * Convenience method that serializes given value but so that results are
     * stored in a newly constructed {@link Properties}. Functionally equivalent
     * to serializing in a File and reading contents into {@link Properties}.
     */
    public Properties writeValueAsProperties(Object value)
        throws IOException
    {
        final Properties props = new Properties();
        writeValue(props, value);
        return props;
    }

    /**
     * Convenience method that serializes given value but so that results are
     * stored in given {@link Properties} instance.
     */
    public Properties writeValueAsProperties(Object value, JavaPropsSchema schema)
        throws IOException
    {
        final Properties props = new Properties();
        writeValue(props, value, schema);
        return props;
    }

    /**
     * Convenience method that serializes given value but so that results are
     * stored in a newly constructed {@link Properties}. Functionally equivalent
     * to serializing in a File and reading contents into {@link Properties}.
     */
    public Map<String, String> writeValueAsMap(Object value) throws IOException
    {
        final Map<String, String> map = new LinkedHashMap<>();
        writeValue(map, value);
        return map;
    }

    /**
     * Convenience method that serializes given value but so that results are
     * stored in given {@link Properties} instance.
     */
    public Map<String, String> writeValueAsMap(Object value, JavaPropsSchema schema)
        throws IOException
    {
        final Map<String, String> map = new LinkedHashMap<>();
        writeValue(map, value, schema);
        return map;
    }

    /*
    /**********************************************************************
    /* Schema support methods?
    /**********************************************************************
     */

    // do we have any actually?

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
        private final static JavaPropsMapper MAPPER = JavaPropsMapper.builder().build();

        public static JavaPropsMapper wrapped() { return MAPPER; }
    }
}
