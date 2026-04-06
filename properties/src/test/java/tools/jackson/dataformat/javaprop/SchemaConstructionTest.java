package tools.jackson.dataformat.javaprop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SchemaConstructionTest extends ModuleTestBase
{
    // Tests to verify stickiness of settings
    @Test
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

    // [dataformats-text#xxx]: withPathSeparatorEscapeChar() was comparing
    // against wrong field (_pathSeparator instead of _pathSeparatorEscapeChar)
    @Test
    public void testWithPathSeparatorEscapeChar()
    {
        JavaPropsSchema empty = JavaPropsSchema.emptySchema();
        assertEquals('\0', empty.pathSeparatorEscapeChar());

        // Setting a new escape char should produce a new schema
        JavaPropsSchema schema2 = empty.withPathSeparatorEscapeChar('\\');
        assertNotSame(empty, schema2);
        assertEquals('\\', schema2.pathSeparatorEscapeChar());

        // Setting the same escape char should return the same instance
        assertSame(schema2, schema2.withPathSeparatorEscapeChar('\\'));

        // Setting a different escape char should produce a new schema
        JavaPropsSchema schema3 = schema2.withPathSeparatorEscapeChar('/');
        assertNotSame(schema2, schema3);
        assertEquals('/', schema3.pathSeparatorEscapeChar());
    }
}
