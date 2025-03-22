package com.fasterxml.jackson.dataformat.csv;

import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;

/**
 * Abstract base class for text-based format parsers (e.g., CSV, YAML) that provides
 * common functionality for configuration, state management, and token handling.
 */
public abstract class TextFormatParser extends ParserBase {

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Codec used for data binding when requested.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Bitmask of format-specific features enabled/disabled.
     */
    protected int _formatFeatures;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected TextFormatParser(IOContext ctxt, int parserFeatures) {
        super(ctxt, parserFeatures);
    }

    /*
    /**********************************************************************
    /* JsonParser Overrides: Configuration and Codec
    /**********************************************************************
     */

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    @Override
    public int getFormatFeatures() {
        return _formatFeatures;
    }

    @Override
    public JsonParser overrideFormatFeatures(int values, int mask) {
        _formatFeatures = (_formatFeatures & ~mask) | (values & mask);
        updateFeatureDependentState();
        return this;
    }

    /*
    /**********************************************************************
    /* Abstract Methods for Format-Specific Behavior
    /**********************************************************************
     */

    /**
     * Updates internal state dependent on format feature changes (e.g., flags like EMPTY_STRING_AS_NULL).
     * Subclasses should override if they maintain feature-dependent fields.
     */
    protected abstract void updateFeatureDependentState();

    /**
     * Advances the parser to the next token.
     */
    @Override
    public abstract JsonToken nextToken() throws IOException;

    /**
     * Closes the underlying input source if applicable.
     */
    @Override
    protected abstract void _closeInput() throws IOException;

    /*
    /**********************************************************************
    /* Common Token Accessors
    /**********************************************************************
     */

    @Override
    public String currentName() throws IOException {
        return (_currToken == JsonToken.FIELD_NAME) ? getCurrentFieldName() : super.currentName();
    }

    @Override
    public String getText() throws IOException {
        if (_currToken == JsonToken.FIELD_NAME) {
            return getCurrentFieldName();
        }
        if (_currToken != null && _currToken.isScalarValue()) {
            return getCurrentScalarValue();
        }
        return (_currToken != null) ? _currToken.asString() : null;
    }

    @Override
    public int getText(Writer writer) throws IOException {
        String str = getText();
        if (str == null) {
            return 0;
        }
        writer.write(str);
        return str.length();
    }

    /*
    /**********************************************************************
    /* Abstract Helpers for Subclass-Specific State
    /**********************************************************************
     */

    /**
     * Returns the current field name, if applicable.
     */
    protected abstract String getCurrentFieldName();

    /**
     * Returns the current scalar value (e.g., string, number), if applicable.
     */
    protected abstract String getCurrentScalarValue();

    /*
    /**********************************************************************
    /* Default Capability Introspection
    /**********************************************************************
     */

    @Override
    public boolean requiresCustomCodec() {
        return false;
    }

    @Override
    public JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
        return DEFAULT_READ_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Version
    /**********************************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION; // Assume a common package version constant
    }
}