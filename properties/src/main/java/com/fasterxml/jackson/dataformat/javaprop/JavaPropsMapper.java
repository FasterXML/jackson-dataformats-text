package com.fasterxml.jackson.dataformat.javaprop;

import java.io.IOException;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MapperBuilderState;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

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
    /**********************************************************
    /* Extended read methods
    /**********************************************************
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
        DeserializationContext ctxt = createDeserializationContext();
        JsonParser p = tokenStreamFactory().createParser(ctxt, props);
        p.setSchema(schema);
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
        DeserializationContext ctxt = createDeserializationContext();
        JsonParser p = tokenStreamFactory().createParser(ctxt, props);
        p.setSchema(schema);
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
     * in given {@link Properties} object.
     */
    public void writeValue(Properties targetProps, Object value) throws IOException
    {
        if (targetProps == null) {
            throw new IllegalArgumentException("Can not pass null Properties as target");
        }
        DefaultSerializerProvider prov = _serializerProvider();
        JavaPropsGenerator g = tokenStreamFactory()
                .createGenerator(prov, targetProps);
        writeValue(g, value);
        g.close();
    }

    /**
     * Convenience method that "writes" given `value` as properties
     * in given {@link Properties} object.
     */
    public void writeValue(Properties targetProps, Object value, JavaPropsSchema schema)
            throws IOException
    {
        if (targetProps == null) {
            throw new IllegalArgumentException("Can not pass null Properties as target");
        }
        DefaultSerializerProvider prov = _serializerProvider();
        JavaPropsGenerator g = tokenStreamFactory()
                .createGenerator(prov, targetProps);
        if (schema != null) {
            g.setSchema(schema);
        }
        writeValue(g, value);
        g.close();
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
        Properties props = new Properties();
        writeValue(props, value, schema);
        return props;
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
