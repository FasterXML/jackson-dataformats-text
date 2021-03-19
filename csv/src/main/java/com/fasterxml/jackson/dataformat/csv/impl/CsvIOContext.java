package com.fasterxml.jackson.dataformat.csv.impl;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.InputSourceReference;
import com.fasterxml.jackson.core.util.BufferRecycler;

public class CsvIOContext extends IOContext
{
    public CsvIOContext(BufferRecycler br, InputSourceReference sourceRef,
            boolean managedResource) {
        super(br, sourceRef, managedResource);
    }

    public TextBuffer csvTextBuffer() {
        return new TextBuffer(_bufferRecycler);
    }
}
