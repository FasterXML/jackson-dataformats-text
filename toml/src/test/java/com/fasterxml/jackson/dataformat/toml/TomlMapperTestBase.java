package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

public abstract class TomlMapperTestBase {
    protected static TomlFactory newTomlFactory() {
        return TomlFactory.builder().build();
    }
    
    protected static TomlMapper newTomlMapper() {
        return new TomlMapper(newTomlFactory());
    }

    protected static TomlMapper newTomlMapper(TomlFactory tomlFactory) {
        return new TomlMapper(tomlFactory);
    }

    protected static IOContext testIOContext() {
        return testIOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    protected static IOContext testIOContext(StreamReadConstraints src) {
        return testIOContext(src,
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    protected static IOContext testIOContext(StreamWriteConstraints swc) {
        return testIOContext(StreamReadConstraints.defaults(),
                swc,
                ErrorReportConfiguration.defaults());
    }

    private static IOContext testIOContext(StreamReadConstraints src,
            StreamWriteConstraints swc,
            ErrorReportConfiguration erc) {
        return new IOContext(src, swc, erc,
                new BufferRecycler(), ContentReference.unknown(), false);
    }
}
