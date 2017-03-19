package com.fasterxml.jackson.dataformat.javaprop;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomSeparatorsTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = mapperForProps();

    public void testCustomPathSeparator() throws Exception
    {
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withPathSeparator("->")
                ;
        String props = MAPPER.writer(schema)
                .writeValueAsString(
                new Rectangle(new Point(1, -2), new Point(5, 10)));
        assertEquals("topLeft->x=1\n"
                +"topLeft->y=-2\n"
                +"bottomRight->x=5\n"
                +"bottomRight->y=10\n"
                ,props);

        // and should come back as well, even with some white space sprinkled
        final String INPUT = "topLeft->x  =  1\n"
                +"topLeft->y=   -2\n"
                +"bottomRight->x   =5\n"
                +"bottomRight->y = 10\n";
        
        Rectangle result = MAPPER.readerFor(Rectangle.class)
                .with(schema)
                .readValue(INPUT);
        assertEquals(10, result.bottomRight.y);
    }
}
