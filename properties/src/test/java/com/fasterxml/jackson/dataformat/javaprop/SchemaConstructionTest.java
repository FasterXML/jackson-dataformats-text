package com.fasterxml.jackson.dataformat.javaprop;

public class SchemaConstructionTest extends ModuleTestBase
{
    // Tests to verify stickiness of settings
    public void testMutantFactories()
    {
        JavaPropsSchema empty = JavaPropsSchema.emptySchema();
        JavaPropsSchema schema2;

        assertFalse(empty.writeIndexUsingMarkers());
        
        schema2 = empty.withFirstArrayOffset(1);
        assertEquals(1, schema2.firstArrayOffset());
        assertEquals(1, schema2.withFirstArrayOffset(1).firstArrayOffset());

        schema2 = empty.withPathSeparator("//");
        assertEquals("//", schema2.pathSeparator());
        assertEquals("//", schema2.withPathSeparator("//").pathSeparator());
        assertEquals("", schema2.withoutPathSeparator().pathSeparator());
        assertEquals("", schema2.withPathSeparator(null).pathSeparator());

        schema2 = empty.withLineIndentation("  ");
        assertEquals("  ", schema2.lineIndentation());
        assertEquals("  ", schema2.withLineIndentation("  ").lineIndentation());
        
        schema2 = empty.withHeader("");
        assertEquals("", schema2.header());
        assertEquals("", schema2.withHeader("").header());

        schema2 = empty.withLineEnding("\r");
        assertEquals("\r", schema2.lineEnding());
        assertEquals("\r", schema2.withLineEnding("\r").lineEnding());

        assertEquals("JavaProps", schema2.getSchemaType());
    }
}
