package com.fasterxml.jackson.dataformat.javaprop.util;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JPropPathSplitterTest extends ModuleTestBase
{
    @Test
    public void testSplitters()
    {
        JPropPathSplitter sp = JPropPathSplitter.create(JavaPropsSchema.emptySchema());
        // by default we should be getting general-purpose one
        assertEquals(JPropPathSplitter.FullSplitter.class, sp.getClass());

        // but if disabling index:
        sp = JPropPathSplitter.create(JavaPropsSchema.emptySchema()
                .withoutIndexMarker());
        assertEquals(JPropPathSplitter.CharPathOnlySplitter.class, sp.getClass());

        // or, if disabling index and path
        sp = JPropPathSplitter.create(JavaPropsSchema.emptySchema()
                .withoutPathSeparator()
                .withoutIndexMarker());
        assertEquals(JPropPathSplitter.NonSplitting.class, sp.getClass());
    }
}
