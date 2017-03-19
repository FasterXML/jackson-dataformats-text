package com.fasterxml.jackson.dataformat.javaprop;

import java.io.IOException;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JavaPropsMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

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
    }

    protected JavaPropsMapper(JavaPropsMapper src) {
        super(src);
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
     *
     * @since 2.9
     */
    @SuppressWarnings("resource")
    public <T> T readPropertiesAs(Properties props, JavaPropsSchema schema,
            Class<T> valueType) throws IOException {
        JsonParser p = getFactory().createParser(props);
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
        JsonParser p = getFactory().createParser(props);
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
     * in given {@link Properties} object.
     *
     * @since 2.9
     */
    public void writeValue(Properties targetProps, Object value) throws IOException
    {
        if (targetProps == null) {
            throw new IllegalArgumentException("Can not pass null Properties as target");
        }
        JavaPropsGenerator g = ((JavaPropsFactory) getFactory())
                .createGenerator(targetProps);
        writeValue(g, value);
        g.close();
    }

    /**
     * Convenience method that "writes" given `value` as properties
     * in given {@link Properties} object.
     *
     * @since 2.9
     */
    public void writeValue(Properties targetProps, Object value, JavaPropsSchema schema)
            throws IOException
    {
        if (targetProps == null) {
            throw new IllegalArgumentException("Can not pass null Properties as target");
        }
        JavaPropsGenerator g = ((JavaPropsFactory) getFactory())
                .createGenerator(targetProps);
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
     *
     * @since 2.9
     */
    public Properties writeValueAsProperties(Object value)
        throws IOException
    {
        Properties props = new Properties();
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
    
    /*
    /**********************************************************
    /* Schema support methods?
    /**********************************************************
     */

    // do we have any actually?
}
