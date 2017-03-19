package com.fasterxml.jackson.dataformat.javaprop;

import java.util.Properties;

import com.fasterxml.jackson.dataformat.javaprop.util.Markers;

public class ArrayGenerationTest extends ModuleTestBase
{
    private final JavaPropsMapper MAPPER = mapperForProps();

    public void testPointListSimple() throws Exception
    {
        Points input = new Points
                (new Point(1, 2), new Point(3, 4), new Point(5, 6));
        String output = MAPPER.writeValueAsString(input);
        assertEquals("p.1.x=1\n"
                +"p.1.y=2\n"
                +"p.2.x=3\n"
                +"p.2.y=4\n"
                +"p.3.x=5\n"
                +"p.3.y=6\n"
                ,output);
        Properties props = MAPPER.writeValueAsProperties(input);
        assertEquals(6, props.size());
        assertEquals("6", props.get("p.3.y"));
        assertEquals("1", props.get("p.1.x"));
    }

    public void testPointListWithIndex() throws Exception
    {
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withWriteIndexUsingMarkers(true)
                .withFirstArrayOffset(3);
        Points input = new Points
                (new Point(1, 2), new Point(3, 4), new Point(5, 6));
        String output = MAPPER.writer(schema)
                .writeValueAsString(input);
        assertEquals("p[3].x=1\n"
                +"p[3].y=2\n"
                +"p[4].x=3\n"
                +"p[4].y=4\n"
                +"p[5].x=5\n"
                +"p[5].y=6\n"
                ,output);
        Properties props = MAPPER.writeValueAsProperties(input, schema);
        assertEquals(6, props.size());
        assertEquals("2", props.get("p[3].y"));
        assertEquals("3", props.get("p[4].x"));
    }

    public void testPointListWithCustomMarkers() throws Exception
    {
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withWriteIndexUsingMarkers(true)
                .withIndexMarker(Markers.create("<<", ">>"))
                ;
        Points input = new Points(new Point(1, 2), new Point(3, 4));
        String output = MAPPER.writer(schema)
                .writeValueAsString(input);
        assertEquals("p<<1>>.x=1\n"
                +"p<<1>>.y=2\n"
                +"p<<2>>.x=3\n"
                +"p<<2>>.y=4\n"
                ,output);
        Properties props = MAPPER.writeValueAsProperties(input, schema);
        assertEquals(4, props.size());
        assertEquals("1", props.get("p<<1>>.x"));
        assertEquals("4", props.get("p<<2>>.y"));
    }
}
