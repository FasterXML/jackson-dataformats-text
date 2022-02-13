package com.fasterxml.jackson.dataformat.csv;

import java.util.Collection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.csv.impl.LRUMap;
import com.fasterxml.jackson.databind.util.ViewMatcher;

/**
 * Specialized {@link ObjectMapper}, with extended functionality to
 * produce {@link CsvSchema} instances out of POJOs.
 */
public class CsvMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * CSV backend.
     *
     * @since 2.10
     */
    public static class Builder extends MapperBuilder<CsvMapper, Builder>
    {
        public Builder(CsvMapper m) {
            super(m);
        }

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        public Builder enable(CsvParser.Feature... features) {
            for (CsvParser.Feature f : features) {
                _mapper.enable(f);
            }
            return this;
        }

        public Builder disable(CsvParser.Feature... features) {
            for (CsvParser.Feature f : features) {
                _mapper.disable(f);
            }
            return this;
        }

        public Builder configure(CsvParser.Feature f, boolean state)
        {
            if (state) {
                _mapper.enable(f);
            } else {
                _mapper.disable(f);
            }
            return this;
        }

        public Builder enable(CsvGenerator.Feature... features) {
            for (CsvGenerator.Feature f : features) {
                _mapper.enable(f);
            }
            return this;
        }

        public Builder disable(CsvGenerator.Feature... features) {
            for (CsvGenerator.Feature f : features) {
                _mapper.disable(f);
            }
            return this;
        }

        public Builder configure(CsvGenerator.Feature f, boolean state)
        {
            if (state) {
                _mapper.enable(f);
            } else {
                _mapper.disable(f);
            }
            return this;
        }
    }

    /**
     * Simple caching for schema instances, given that they are relatively expensive
     * to construct; this one is for "loose" (non-typed) schemas
     */
    protected final LRUMap<JavaType,CsvSchema> _untypedSchemas;

    /**
     * Simple caching for schema instances, given that they are relatively expensive
     * to construct; this one is for typed schemas
     */
    protected final LRUMap<JavaType,CsvSchema> _typedSchemas;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CsvMapper() {
        this(new CsvFactory());
    }

    @SuppressWarnings("deprecation")
    public CsvMapper(CsvFactory f)
    {
        super(f);
        // As per #11: default to alphabetic ordering
        enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        _untypedSchemas = new LRUMap<JavaType,CsvSchema>(8,32);
        _typedSchemas = new LRUMap<JavaType,CsvSchema>(8,32);
    }

    /**
     * Copy-constructor, mostly used to support {@link #copy}.
     *<p>
     * NOTE: {@link ObjectMapper} had this method since 2.1.
     * 
     * @since 2.5
     */
    protected CsvMapper(CsvMapper src)
    {
        super(src);
        _untypedSchemas = new LRUMap<JavaType,CsvSchema>(8,32);
        _typedSchemas = new LRUMap<JavaType,CsvSchema>(8,32);
    }

    /**
     * Short-cut for:
     *<pre>
     *   return builder(new CsvFactory());
     *</pre>
     *
     * @since 2.10
     */
    public static CsvMapper.Builder csvBuilder() {
        return new CsvMapper.Builder(new CsvMapper());
    }

    /**
     * @since 2.10
     */
    public static CsvMapper.Builder builder() {
        return new CsvMapper.Builder(new CsvMapper());
    }

    /**
     * @since 2.10
     */
    public static CsvMapper.Builder builder(CsvFactory streamFactory) {
        return new CsvMapper.Builder(new CsvMapper(streamFactory));
    }

    /**
     * @since 2.5
     */
    @Override
    public CsvMapper copy()
    {
        _checkInvalidCopy(CsvMapper.class);
        return new CsvMapper(this);
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    public CsvMapper configure(CsvGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    public CsvMapper configure(CsvParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    public CsvMapper enable(CsvGenerator.Feature f) {
        ((CsvFactory)_jsonFactory).enable(f);
        return this;
    }

    public CsvMapper enable(CsvParser.Feature f) {
        ((CsvFactory)_jsonFactory).enable(f);
        return this;
    }

    public CsvMapper disable(CsvGenerator.Feature f) {
        ((CsvFactory)_jsonFactory).disable(f);
        return this;
    }

    public CsvMapper disable(CsvParser.Feature f) {
        ((CsvFactory)_jsonFactory).disable(f);
        return this;
    }

    /*
    /**********************************************************************
    /* Additional typed accessors
    /**********************************************************************
     */

    /**
     * Overridden with more specific type, since factory we have
     * is always of type {@link CsvFactory}
     */
    @Override
    public CsvFactory getFactory() {
        return (CsvFactory) _jsonFactory;
    }

    /*
    /**********************************************************************
    /* Additional ObjectReader factory methods
    /**********************************************************************
     */

    /**
     * Convenience method which is functionally equivalent to:
     *<pre>
     *  reader(pojoType).withSchema(schemaFor(pojoType));
     *</pre>
     * that is, constructs a {@link ObjectReader} which both binds to
     * specified type and uses "loose" {@link CsvSchema} introspected from
     * specified type (one without strict inferred typing).
     *<p>
     * @param pojoType Type used both for data-binding (result type) and for
     *   schema introspection. NOTE: must NOT be an array or Collection type, since
     *   these only make sense for data-binding (like arrays of objects to bind),
     *   but not for schema construction (no CSV types can be mapped to arrays
     *   or Collections)
     */
    public ObjectReader readerWithSchemaFor(Class<?> pojoType)
    {
        JavaType type = constructType(pojoType);
        /* sanity check: not useful for structured types, since
         * schema type will need to differ from data-bind type
         */
        if (type.isArrayType() || type.isCollectionLikeType()) {
            throw new IllegalArgumentException("Type can NOT be a Collection or array type");
        }
        return readerFor(type).with(schemaFor(type));
    }

    /**
     * Convenience method which is functionally equivalent to:
     *<pre>
     *  reader(pojoType).withSchema(typedSchemaFor(pojoType));
     *</pre>
     * that is, constructs a {@link ObjectReader} which both binds to
     * specified type and uses "strict" {@link CsvSchema} introspected from
     * specified type (one where typing is inferred).
     */
    public ObjectReader readerWithTypedSchemaFor(Class<?> pojoType)
    {
        JavaType type = constructType(pojoType);
        // sanity check: not useful for structured types, since
        // schema type will need to differ from data-bind type
        if (type.isArrayType() || type.isCollectionLikeType()) {
            throw new IllegalArgumentException("Type can NOT be a Collection or array type");
        }
        return readerFor(type).with(typedSchemaFor(type));
    }

    /*
    /**********************************************************************
    /* Additional ObjectWriter factory methods
    /**********************************************************************
     */

    /**
     * Convenience method which is functionally equivalent to:
     *<pre>
     *  writer(pojoType).with(schemaFor(pojoType));
     *</pre>
     * that is, constructs a {@link ObjectWriter} which both binds to
     * specified type and uses "loose" {@link CsvSchema} introspected from
     * specified type (one without strict inferred typing).
     *<p>
     * @param pojoType Type used both for data-binding (result type) and for
     *   schema introspection. NOTE: must NOT be an array or Collection type, since
     *   these only make sense for data-binding (like arrays of objects to bind),
     *   but not for schema construction (no root-level CSV types can be mapped to arrays
     *   or Collections)
     */
    public ObjectWriter writerWithSchemaFor(Class<?> pojoType)
    {
        JavaType type = constructType(pojoType);
        // sanity check as per javadoc above
        if (type.isArrayType() || type.isCollectionLikeType()) {
            throw new IllegalArgumentException("Type can NOT be a Collection or array type");
        }
        return writerFor(type).with(schemaFor(type));
    }

    /**
     * Convenience method which is functionally equivalent to:
     *<pre>
     *  writer(pojoType).with(typedSchemaFor(pojoType));
     *</pre>
     * that is, constructs a {@link ObjectWriter} which both binds to
     * specified type and uses "strict" {@link CsvSchema} introspected from
     * specified type (one where typing is inferred).
     */
    public ObjectWriter writerWithTypedSchemaFor(Class<?> pojoType)
    {
        JavaType type = constructType(pojoType);
        // sanity check as per javadoc above
        if (type.isArrayType() || type.isCollectionLikeType()) {
            throw new IllegalArgumentException("Type can NOT be a Collection or array type");
        }
        return writerFor(type).with(typedSchemaFor(type));
    }

    /*
    /**********************************************************************
    /* CsvSchema construction; overrides, new methods
    /**********************************************************************
     */

    /**
     * Convenience method that is same as
     *<pre>
     *   CsvSchema.emptySchema().withHeader();
     *</pre>
     * and returns a {@link CsvSchema} instance that uses default configuration
     * with additional setting that the first content line contains intended
     * column names.
     *
     * @since 2.5
     */
    public CsvSchema schemaWithHeader() {
        return CsvSchema.emptySchema().withHeader();
    }

    /**
     * Convenience method that is same as
     *<pre>
     *   CsvSchema.emptySchema()
     *</pre>
     * that is, returns an "empty" Schema; one with default values and no
     * column definitions.
     *
     * @since 2.5
     */
    public CsvSchema schema() {
        return CsvSchema.emptySchema();
    }

    /**
     * Method that can be used to determine a CSV schema to use for given
     * POJO type, using default serialization settings including ordering.
     * Definition will not be strictly typed (that is, all columns are
     * just defined to be exposed as String tokens).
     */
    public CsvSchema schemaFor(JavaType pojoType) {
        return _schemaFor(pojoType, _untypedSchemas, false, null);
    }

    public CsvSchema schemaForWithView(JavaType pojoType, Class<?> view) {
        return _schemaFor(pojoType, _untypedSchemas, false, view);
    }

    public final CsvSchema schemaFor(Class<?> pojoType) {
        return _schemaFor(constructType(pojoType), _untypedSchemas, false, null);
    }

    public final CsvSchema schemaForWithView(Class<?> pojoType, Class<?> view) {
        return _schemaFor(constructType(pojoType), _untypedSchemas, false, view);
    }

    public final CsvSchema schemaFor(TypeReference<?> pojoTypeRef) {
        return _schemaFor(constructType(pojoTypeRef.getType()), _untypedSchemas, false, null);
    }

    public final CsvSchema schemaForWithView(TypeReference<?> pojoTypeRef, Class<?> view) {
        return _schemaFor(constructType(pojoTypeRef.getType()), _untypedSchemas, false, view);
    }

    /**
     * Method that can be used to determine a CSV schema to use for given
     * POJO type, using default serialization settings including ordering.
     * Definition WILL be strictly typed: that is, code will try to 
     * determine type limitations which may make parsing more efficient
     * (especially for numeric types like java.lang.Integer).
     */
    public CsvSchema typedSchemaFor(JavaType pojoType) {
        return _schemaFor(pojoType, _typedSchemas, true, null);
    }

    public CsvSchema typedSchemaForWithView(JavaType pojoType, Class<?> view) {
        return _schemaFor(pojoType, _typedSchemas, true, view);
    }

    public final CsvSchema typedSchemaFor(Class<?> pojoType) {
        return _schemaFor(constructType(pojoType), _typedSchemas, true, null);
    }

    public final CsvSchema typedSchemaForWithView(Class<?> pojoType, Class<?> view) {
        return _schemaFor(constructType(pojoType), _typedSchemas, true, view);
    }

    public final CsvSchema typedSchemaFor(TypeReference<?> pojoTypeRef) {
        return _schemaFor(constructType(pojoTypeRef.getType()), _typedSchemas, true, null);
    }

    public final CsvSchema typedSchemaForWithView(TypeReference<?> pojoTypeRef, Class<?> view) {
        return _schemaFor(constructType(pojoTypeRef.getType()), _typedSchemas, true, view);
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected CsvSchema _schemaFor(JavaType pojoType, LRUMap<JavaType,CsvSchema> schemas,
            boolean typed, Class<?> view)
    {
        // 15-Dec-2021, tatu: [dataformats-text#288] Only cache if we don't have
        //    a view, to avoid conflicts
        if (view == null) { 
            synchronized (schemas) {
                CsvSchema s = schemas.get(pojoType);
                if (s != null) {
                    return s;
                }
            }
        }
        final AnnotationIntrospector intr = _deserializationConfig.getAnnotationIntrospector();
        CsvSchema.Builder builder = CsvSchema.builder();
        _addSchemaProperties(builder, intr, typed, pojoType, null, view);
        CsvSchema result = builder.build();
        if (view == null) { // only cache without view (see above)
            synchronized (schemas) {
                schemas.put(pojoType, result);
            }
        }
        return result;
    }

    @Deprecated // since 2.11 (remove from 3.0 at latest)
    protected CsvSchema _schemaFor(JavaType pojoType, LRUMap<JavaType,CsvSchema> schemas, boolean typed) {
        return _schemaFor(pojoType, schemas, typed, null);
    }

    protected boolean _nonPojoType(JavaType t)
    {
        if (t.isPrimitive() || t.isEnumType()) {
            return true;
        }
        Class<?> raw = t.getRawClass();
        // Wrapper types for numbers
        if (Number.class.isAssignableFrom(raw)) {
            if ((raw == Byte.class)
                || (raw == Short.class)
                || (raw == Character.class)
                || (raw == Integer.class)
                || (raw == Long.class)
                || (raw == Float.class)
                || (raw == Double.class)
                ) {
                return true;
            }
        }
        // Some other well-known non-POJO types
        if ((raw == Boolean.class)
                || (raw == String.class)
                ) {
            return true;
        }
        return false;
    }

    protected void _addSchemaProperties(CsvSchema.Builder builder, AnnotationIntrospector intr,
            boolean typed, JavaType pojoType, NameTransformer unwrapper, Class<?> view)
    {
        // 09-Aug-2015, tatu: From [dataformat-csv#87], realized that one can not have
        //    real schemas for primitive/wrapper
        if (_nonPojoType(pojoType)) {
            return;
        }
        
        BeanDescription beanDesc = getSerializationConfig().introspect(pojoType);
        final boolean includeByDefault = isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION);
        for (BeanPropertyDefinition prop : beanDesc.findProperties()) {
            if (view != null) {
                Class<?>[] views = prop.findViews();
                if (views == null) {
                    views = beanDesc.findDefaultViews();
                }
                // If property defines no Views AND non-view-enabled included by default,
                // should include
                if ((views == null) && includeByDefault) {
                    ;
                } else if (!ViewMatcher.construct(views).isVisibleForView(view)) {
                    continue;
                }
            }
            // ignore setter-only properties:
            if (!prop.couldSerialize()) {
                continue;
            }
            // [dataformat-csv#15]: handle unwrapped props
            AnnotatedMember m = prop.getPrimaryMember();
            if (m != null) {
                NameTransformer nextUnwrapper = intr.findUnwrappingNameTransformer(prop.getPrimaryMember());
                if (nextUnwrapper != null) {
                    if (unwrapper != null) {
                        nextUnwrapper = NameTransformer.chainedTransformer(unwrapper, nextUnwrapper);
                    }
                    JavaType nextType = m.getType();
                    _addSchemaProperties(builder, intr, typed, nextType, nextUnwrapper, view);
                    continue;
                }
            }
            // Then name wrapping/unwrapping
            String name = prop.getName();
            
            if (unwrapper != null) {
                name = unwrapper.transform(name);
            }
            if (typed && m != null) {
                builder.addColumn(name, _determineType(m.getRawType()));
            } else {
                builder.addColumn(name);
            }
        }
    }
    
    // should not be null since couldSerialize() returned true, so:
    protected CsvSchema.ColumnType _determineType(Class<?> propType)
    {
        // very first thing: arrays
        if (propType.isArray()) {
            // one exception; byte[] assumed to come in as Base64 encoded
            if (propType == byte[].class) {
                return CsvSchema.ColumnType.STRING;
            }
            return CsvSchema.ColumnType.ARRAY;
        }
        
        // First let's check certain cases that ought to be just presented as Strings...
        if (propType == String.class
                || propType == Character.TYPE
                || propType == Character.class) {
            return CsvSchema.ColumnType.STRING;
        }
        if (propType == Boolean.class
                || propType == Boolean.TYPE) {
            return CsvSchema.ColumnType.BOOLEAN;
        }

        // all primitive types are good for NUMBER, since 'char', 'boolean' handled above
        if (propType.isPrimitive()) {
            return CsvSchema.ColumnType.NUMBER;
        }
        if (Number.class.isAssignableFrom(propType)) {
            return CsvSchema.ColumnType.NUMBER;
        }
        if (Collection.class.isAssignableFrom(propType)) { // since 2.5
            return CsvSchema.ColumnType.ARRAY;
        }
        // but in general we will just do what we can:
        return CsvSchema.ColumnType.NUMBER_OR_STRING;
    }
}
