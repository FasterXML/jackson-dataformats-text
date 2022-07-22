package com.fasterxml.jackson.dataformat.csv.impl;

import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.util.BufferRecycler;

public class CsvIOContext extends IOContext
{
    public CsvIOContext(BufferRecycler br, ContentReference sourceRef,
            boolean managedResource) {
        super(br, sourceRef, managedResource);
    }

    public TextBuffer csvTextBuffer() {
        return new TextBuffer(_bufferRecycler);
    }
}
