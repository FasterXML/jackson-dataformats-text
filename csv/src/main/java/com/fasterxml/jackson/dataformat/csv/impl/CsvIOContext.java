package com.fasterxml.jackson.dataformat.csv.impl;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.util.BufferRecycler;

public class CsvIOContext extends IOContext
{
    // @since 2.15
    public CsvIOContext(StreamReadConstraints streamReadConstraints,
                        BufferRecycler br, ContentReference sourceRef, boolean managedResource) {
        super(streamReadConstraints, br, sourceRef, managedResource);
    }

    @Deprecated // @since v2.15
    public CsvIOContext(BufferRecycler br, ContentReference sourceRef,
            boolean managedResource) {
        super(br, sourceRef, managedResource);
    }

    public CsvTextBuffer csvTextBuffer() {
        return new CsvTextBuffer(streamReadConstraints(), _bufferRecycler);
    }
}
