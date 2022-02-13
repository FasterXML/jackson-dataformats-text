package com.fasterxml.jackson.dataformat.csv;

import java.util.Collection;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MapperBuilderState;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.SimpleLookupCache;
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
     * @since 3.0
     */
    public static class Builder extends MapperBuilder<CsvMapper, Builder>
    {
        public Builder(CsvFactory f) {
            super(f);
        }

        public Builder(StateImpl state) {
            super(state);
        }

        @Override
        public CsvMapper build() {
            return new CsvMapper(this);
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

        public Builder enable(CsvParser.Feature... features) {
            for (CsvParser.Feature f : features) {
                _formatReadFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(CsvParser.Feature... features) {
            for (CsvParser.Feature f : features) {
                _formatReadFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(CsvParser.Feature feature, boolean state)
        {
            if (state) {
                _formatReadFeatures |= feature.getMask();
            } else {
                _formatReadFeatures &= ~feature.getMask();
            }
            return this;
        }

        public Builder enable(CsvGenerator.Feature... features) {
            for (CsvGenerator.Feature f : features) {
                _formatWriteFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(CsvGenerator.Feature... features) {
            for (CsvGenerator.Feature f : features) {
                _formatWriteFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(CsvGenerator.Feature feature, boolean state)
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
    /**********************************************************************
    /* Caching of schemas
    /**********************************************************************
     */

    /**
     * Simple caching for schema instances, given that they are relatively expensive
     * to construct; this one is for "loose" (non-typed) schemas
     */
    protected final SimpleLookupCache<ViewKey,CsvSchema> _untypedSchemas;

    /**
     * Simple caching for schema instances, given that they are relatively expensive
     * to construct; this one is for typed schemas
     */
    protected final SimpleLookupCache<ViewKey,CsvSchema> _typedSchemas;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CsvMapper() {
        this(new Builder(new CsvFactory()));
    }

    public CsvMapper(CsvFactory f) {
        this(new Builder(f));

    }

    /**
     * @since 3.0
     */
    public CsvMapper(CsvMapper.Builder b) {
        super(b);
        _untypedSchemas = new SimpleLookupCache<>(8,32);
        _typedSchemas = new SimpleLookupCache<>(8,32);
    }

    public static CsvMapper.Builder builder() {
        return new CsvMapper.Builder(new CsvFactory());
    }

    public static CsvMapper.Builder builder(CsvFactory streamFactory) {
        return new CsvMapper.Builder(streamFactory);
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
     * Accessor method for getting globally shared "default" {@link CsvMapper}
     * instance: one that has default configuration, no modules registered, no
     * config overrides. Usable mostly when dealing "untyped" or Tree-style
     * content reading and writing.
     */
    public static CsvMapper shared() {
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
    /* Additional typed accessors
    /**********************************************************************
     */

    /**
     * Overridden with more specific type, since factory we have
     * is always of type {@link CsvFactory}
     */
    @Override
    public CsvFactory tokenStreamFactory() {
        return (CsvFactory) _streamFactory;
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

    protected CsvSchema _schemaFor(JavaType pojoType, SimpleLookupCache<ViewKey,CsvSchema> schemas,
            boolean typed, Class<?> view)
    {
        final ViewKey viewKey = new ViewKey(pojoType, view);
        synchronized (schemas) {
            CsvSchema s = schemas.get(viewKey);
            if (s != null) {
                return s;
            }
        }
        // 15-Oct-2019, tatu: Since 3.0, need context for introspection
        final SerializerProvider ctxt = _serializerProvider();
        CsvSchema.Builder builder = CsvSchema.builder();
        _addSchemaProperties(ctxt, builder, typed, pojoType, null, view);
        CsvSchema result = builder.build();
        synchronized (schemas) {
            schemas.put(viewKey, result);
        }
        return result;
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

    protected void _addSchemaProperties(SerializerProvider ctxt, CsvSchema.Builder builder,
            boolean typed, JavaType pojoType, NameTransformer unwrapper, Class<?> view)
    {
        // 09-Aug-2015, tatu: From [dataformat-csv#87], realized that one can not have
        //    real schemas for primitive/wrapper
        if (_nonPojoType(pojoType)) {
            return;
        }
        BeanDescription beanDesc = ctxt.introspectBeanDescription(pojoType);
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
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
                NameTransformer nextUnwrapper = intr.findUnwrappingNameTransformer(ctxt.getConfig(),
                        prop.getPrimaryMember());
                if (nextUnwrapper != null) {
                    if (unwrapper != null) {
                        nextUnwrapper = NameTransformer.chainedTransformer(unwrapper, nextUnwrapper);
                    }
                    JavaType nextType = m.getType();
                    _addSchemaProperties(ctxt, builder, typed, nextType, nextUnwrapper, view);
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

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Simple class in order to create a map key based on {@link JavaType} and a given view.
     * Used for caching associated schemas in {@code _untypedSchemas} and {@code _typedSchemas}.
     */
    public static final class ViewKey
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;
    
        private final JavaType _pojoType;
        private final Class<?> _view;
        private final int _hashCode;
    
    
        public ViewKey(final JavaType pojoType, final Class<?> view)
        {
            _pojoType = pojoType;
            _view = view;
            _hashCode = Objects.hash(pojoType, view);
        }
    
    
      @Override
      public int hashCode() { return _hashCode; }
    
      @Override
      public boolean equals(final Object o)
      {
          if (o == this) { return true; }
          if (o == null || o.getClass() != getClass()) { return false; }
          final ViewKey other = (ViewKey) o;
          if (_hashCode != other._hashCode || _view != other._view) { return false; }
          return Objects.equals(_pojoType, other._pojoType);
      }
    
      @Override
      public String toString()
      {
          String viewName = _view != null ? _view.getName() : null;
          return "[ViewKey: pojoType=" + _pojoType + ", view=" + viewName + "]";
      }
    }

    /**
     * Helper class to contain dynamically constructed "shared" instance of
     * mapper, should one be needed via {@link #shared}.
     */
    private final static class SharedWrapper {
        private final static CsvMapper MAPPER = CsvMapper.builder().build();

        public static CsvMapper wrapped() { return MAPPER; }
    }
}
