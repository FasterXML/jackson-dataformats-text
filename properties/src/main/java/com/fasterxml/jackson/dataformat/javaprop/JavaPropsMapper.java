package com.fasterxml.jackson.dataformat.javaprop;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

public class JavaPropsMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * Java Properties backend.
     *
     * @since 2.10
     */
    public static class Builder extends MapperBuilder<JavaPropsMapper, Builder>
    {
        public Builder(JavaPropsMapper m) {
            super(m);
        }
    }

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public JavaPropsMapper() {
        this(new JavaPropsFactory());
    }

    public JavaPropsMapper(JavaPropsFactory f) {
        super(f);

        // 09-Apr-2021, tatu: [dataformats-text#255]: take empty String to
        //    mean `null` where applicable; also accept Blank same way
        enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        coercionConfigDefaults()
            .setAcceptBlankAsEmpty(Boolean.TRUE)
            .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty)
        ;
    }

    protected JavaPropsMapper(JavaPropsMapper src) {
        super(src);
    }

    public static Builder builder() {
        return new JavaPropsMapper.Builder(new JavaPropsMapper());
    }

    public static Builder builder(JavaPropsFactory streamFactory) {
        return new JavaPropsMapper.Builder(new JavaPropsMapper(streamFactory));
    }

    @Override
    public JavaPropsMapper copy()
    {
        _checkInvalidCopy(JavaPropsMapper.class);
        return new JavaPropsMapper(this);
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public JavaPropsFactory getFactory() {
        return (JavaPropsFactory) _jsonFactory;
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
     *
     * @since 2.9
     */
    @SuppressWarnings("resource")
    public <T> T readPropertiesAs(Properties props, JavaPropsSchema schema,
            Class<T> valueType) throws IOException {
        JsonParser p = getFactory().createParser((Map<?,?>)props);
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
     *
     * @since 2.9
     */
    @SuppressWarnings({ "resource", "unchecked" })
    public <T> T readPropertiesAs(Properties props, JavaPropsSchema schema,
            JavaType valueType) throws IOException {
        JsonParser p = getFactory().createParser((Map<?,?>)props);
        p.setSchema(schema);
        return (T) readValue(p, valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     *
     * @since 2.9
     */
    public <T> T readPropertiesAs(Properties props, Class<T> valueType) throws IOException {
        return readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     *
     * @since 2.9
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
     *
     * @since 2.10
     */
    @SuppressWarnings("resource")
    public <T> T readMapAs(Map<String, String> map, JavaPropsSchema schema,
            Class<T> valueType) throws IOException {
        JsonParser p = getFactory().createParser(map);
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
     *
     * @since 2.10
     */
    @SuppressWarnings({ "resource", "unchecked" })
    public <T> T readMapAs(Map<String, String> map, JavaPropsSchema schema,
            JavaType valueType) throws IOException {
        JsonParser p = getFactory().createParser(map);
        p.setSchema(schema);
        return (T) readValue(p, valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     *
     * @since 2.10
     */
    public <T> T readMapAs(Map<String, String> map, Class<T> valueType) throws IOException {
        return readMapAs(map, JavaPropsSchema.emptySchema(), valueType);
    }

    /**
     * Convenience method, functionally equivalent to:
     *<pre>
     *   readPropertiesAs(props, JavaPropsSchema.emptySchema(), valueType);
     *</pre>
     *
     * @since 2.10
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
     *
     * @since 2.9
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
     *
     * @since 2.9
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
     *
     * @since 2.9
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
     *
     * @since 2.9
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
    /**********************************************************
    /* Extended write methods
    /**********************************************************
     */

    /**
     * Convenience method that "writes" given `value` as properties
     * in given {@link Map} object.
     *
     * @since 2.10
     */
    public void writeValue(Map<?,?> target, Object value) throws IOException
    {
        if (target == null) {
            throw new IllegalArgumentException("Can not pass `null` target");
        }
        try (JavaPropsGenerator g = getFactory().createGenerator(target, null)) {
            writeValue(g, value);
        }
    }

    /**
     * Convenience method that "writes" given `value` as properties
     * in given {@link Map} object.
     *
     * @since 2.10
     */
    public void writeValue(Map<?,?> target, Object value, JavaPropsSchema schema)
            throws IOException
    {
        if (target == null) {
            throw new IllegalArgumentException("Can not pass `null` target");
        }
        try (JavaPropsGenerator g = getFactory().createGenerator(target, schema)) {
            if (schema != null) {
                g.setSchema(schema);
            }
            writeValue(g, value);
        }
    }

    @Deprecated // since 2.10
    public void writeValue(Properties targetProps, Object value) throws IOException {
        writeValue((Map<?,?>) targetProps, value);
    }

    @Deprecated // since 2.10
    public void writeValue(Properties targetProps, Object value, JavaPropsSchema schema)
            throws IOException {
        writeValue((Map<?,?>) targetProps, value, schema);
    }
    
    /**
     * Convenience method that serializes given value but so that results are
     * stored in a newly constructed {@link Properties}. Functionally equivalent
     * to serializing in a File and reading contents into {@link Properties}.
     *
     * @since 2.9
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
     *
     * @since 2.9
     */
    public Properties writeValueAsProperties(Object value, JavaPropsSchema schema)
        throws IOException
    {
        Properties props = new Properties();
        writeValue(props, value, schema);
        return props;
    }

    /**
     * Convenience method that serializes given value but so that results are
     * stored in a newly constructed {@link Properties}. Functionally equivalent
     * to serializing in a File and reading contents into {@link Properties}.
     *
     * @since 2.10
     */
    public Map<String, String> writeValueAsMap(Object value)
        throws IOException
    {
        final Map<String, String> map = new LinkedHashMap<>();
        writeValue(map, value);
        return map;
    }

    /**
     * Convenience method that serializes given value but so that results are
     * stored in given {@link Properties} instance.
     *
     * @since 2.10
     */
    public Map<String, String> writeValueAsMap(Object value, JavaPropsSchema schema)
        throws IOException
    {
        final Map<String, String> map = new LinkedHashMap<>();
        writeValue(map, value, schema);
        return map;
    }

    /*
    /**********************************************************
    /* Schema support methods?
    /**********************************************************
     */

    // do we have any actually?
}
