package tools.jackson.dataformat.csv.impl;

import tools.jackson.core.io.IOContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.util.BufferRecycler;

public class CsvIOContext extends IOContext
{
    public CsvIOContext(StreamReadConstraints src,
            BufferRecycler br, ContentReference sourceRef,
            boolean managedResource) {
        super(src, br, sourceRef, managedResource, null);
    }

    public CsvTextBuffer csvTextBuffer() {
        return new CsvTextBuffer(_bufferRecycler);
    }
}
