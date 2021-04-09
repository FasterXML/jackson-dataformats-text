package com.fasterxml.jackson.dataformat.javaprop.deser.convert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.type.LogicalType;

import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

// 2020-12-18, tatu: Modified from "jackson-databind" version: Properties
//   backend MUST NOT prevent coercion from String since Properties
//   values are fundamentally textual and only have String values
public class CoerceToBooleanTest
    extends ModuleTestBase
{
    static class BooleanPrimitivePOJO {
        public boolean value;

        public void setValue(boolean v) { value = v; }
    }

    static class BooleanWrapperPOJO {
        public Boolean value;

        public void setValue(Boolean v) { value = v; }
    }

    static class AtomicBooleanWrapper {
        public AtomicBoolean value;

        public void setValue(AtomicBoolean v) { value = v; }
    }

    private final ObjectMapper DEFAULT_MAPPER = newPropertiesMapper();

    private final ObjectMapper MAPPER_STRING_TO_BOOLEAN_FAIL
        = propertiesMapperBuilder()
            .withCoercionConfig(LogicalType.Boolean, cfg ->
                cfg.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
            .build();

    private final ObjectMapper MAPPER_EMPTY_TO_BOOLEAN_FAIL
        = propertiesMapperBuilder()
                .withCoercionConfig(LogicalType.Boolean, cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
            .build();

    /*
    /**********************************************************
    /* Test methods: default, legacy configuration, from String
    /**********************************************************
     */

    // for [databind#403]
    public void testEmptyStringFailForBooleanPrimitive() throws IOException
    {
        final ObjectReader reader = MAPPER_EMPTY_TO_BOOLEAN_FAIL
                .readerFor(BooleanPrimitivePOJO.class);
        try {
            reader.readValue("value:\n");
            fail("Expected failure for boolean + empty String");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot coerce empty String");
            verifyException(e, "to `boolean` value");
        }
    }

    public void testDefaultStringToBooleanCoercionOk() throws Exception {
        _verifyStringToBooleanOk(DEFAULT_MAPPER);
    }

    /*
    /**********************************************************
    /* Test methods: CoercionConfig, from String
    /**********************************************************
     */

    public void testStringToBooleanOkDespiteCoercionConfig() throws Exception {
        _verifyStringToBooleanOk(MAPPER_STRING_TO_BOOLEAN_FAIL);
    }

    /*
    /**********************************************************
    /* Verification
    /**********************************************************
     */

    public void _verifyStringToBooleanOk(ObjectMapper mapper) throws Exception
    {
        // first successful coercions, basic types, some variants

        assertEquals(true,
                _verifyCoerceSuccess(mapper, "value: true", BooleanPrimitivePOJO.class).value);
        assertEquals(false,
                _verifyCoerceSuccess(mapper, "value: false", BooleanPrimitivePOJO.class).value);
        assertEquals(true,
                _verifyCoerceSuccess(mapper, "value: True", BooleanPrimitivePOJO.class).value);
        assertEquals(false,
                _verifyCoerceSuccess(mapper, "value: False", BooleanPrimitivePOJO.class).value);

        assertEquals(Boolean.TRUE,
                _verifyCoerceSuccess(mapper, "value: true", BooleanWrapperPOJO.class).value);
        assertEquals(Boolean.FALSE,
                _verifyCoerceSuccess(mapper, "value: false", BooleanWrapperPOJO.class).value);
        assertEquals(Boolean.TRUE,
                _verifyCoerceSuccess(mapper, "value: True", BooleanWrapperPOJO.class).value);
        assertEquals(Boolean.FALSE,
                _verifyCoerceSuccess(mapper, "value: False", BooleanWrapperPOJO.class).value);

        // and then Special boolean derivatives:

        AtomicBooleanWrapper abw = _verifyCoerceSuccess(mapper, "value: true", AtomicBooleanWrapper.class);
        assertTrue(abw.value.get());

        abw = _verifyCoerceSuccess(mapper, "value: false", AtomicBooleanWrapper.class);
        assertFalse(abw.value.get());
    }

    /*
    /**********************************************************
    /* Other helper methods
    /**********************************************************
     */

    private <T> T _verifyCoerceSuccess(ObjectMapper mapper,
            String input, Class<T> type) throws IOException
    {
        return mapper.readerFor(type)
                .readValue(input);
    }
}
