package tools.jackson.dataformat.javaprop;

import java.util.Map;

import tools.jackson.databind.ObjectMapper;

public class MapParsingTest extends ModuleTestBase
{
    static class MapWrapper {
        public Map<String,Object> map;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testMapWithBranchNoEscaping() throws Exception
    {
        ObjectMapper mapper = newPropertiesMapper();
        
        // basically "extra" branch should become as first element, and
        // after that ordering by numeric value
        final String INPUT = "map=first\n"
                +"map.b=second\n"
                +"map.xyz=third\n"
                +"map.ab\\\\.c=fourth\n"
                ;
        MapWrapper w = mapper.readValue(INPUT, MapWrapper.class);
        assertNotNull(w.map);
        assertEquals(4, w.map.size());
        assertEquals("first", w.map.get(""));
        assertEquals("second", w.map.get("b"));
        assertEquals("third", w.map.get("xyz"));
        assertEquals("fourth", ((Map<?,?>) w.map.get("ab\\")).get("c"));
    }

    public void testMapWithBranchBackslashEscape() throws Exception
    {
        JavaPropsMapper mapper = newPropertiesMapper();
        
        // Lots of backslash escaped values
        final String INPUT = "map=first\n"
                +"map.b=second\n"
                +"map.xyz=third\n"
                +"map.ab\\\\.c=fourth\n"              // ab\. => ab.c
                +"map.ab\\\\cd\\\\.ef\\\\.gh\\\\\\\\ij=fifth\n" // ab\cd\.df\.gh\\ij => ab\cd.df.gh\\ij
                +"map.\\\\.=sixth\n"                  // \. => .
                +"map.ab\\\\.d=seventh\n"             // ab\.d => ab.d
                +"map.ef\\\\\\\\.d=eigth\n"           // ef\\.d => ef\->d
                +"map.ab\\\\\\\\\\\\.d=ninth\n"       // ab\\\.d => ab\.d
                +"map.xy\\\\.d.ij=tenth\n"            // xy\.d.ij => xy.d->ij
                +"map.xy\\\\\\\\.d.ij=eleventh\n"     // xy\\.d.ij => xy\->d->ij
                +"map.xy\\\\\\\\\\\\.d.ij=twelfth\n"  // xy\\\.d => xy\.d->ij
                ;
        MapWrapper w = mapper.readerFor(MapWrapper.class)
                .with(new JavaPropsSchema().withPathSeparatorEscapeChar('\\'))
                .readValue(INPUT);
        assertNotNull(w.map);
        assertEquals(12, w.map.size());
        assertEquals("first", w.map.get(""));
        assertEquals("second", w.map.get("b"));
        assertEquals("third", w.map.get("xyz"));
        assertEquals("fourth", w.map.get("ab.c"));
        assertEquals("fifth", w.map.get("ab\\cd.ef.gh\\\\ij"));
        assertEquals("sixth", w.map.get("."));
        assertEquals("seventh", w.map.get("ab.d"));
        assertEquals("eigth", ((Map<?,?>) w.map.get("ef\\")).get("d"));
        assertEquals("ninth", w.map.get("ab\\.d"));
        assertEquals("tenth", ((Map<?,?>) w.map.get("xy.d")).get("ij"));
        assertEquals("eleventh", ((Map<?,?>) ((Map<?,?>) w.map.get("xy\\")).get("d")).get("ij"));
        assertEquals("twelfth", ((Map<?,?>) w.map.get("xy\\.d")).get("ij"));
    }

    public void testMapWithBranchHashEscape() throws Exception
    {
        JavaPropsMapper mapper = newPropertiesMapper();
        
        // Lots of backslash escaped values
        final String INPUT = "map=first\n"
                +"map.b=second\n"
                +"map.xyz=third\n"
                +"map.ab#.c=fourth\n"             // ab#. => ab.c
                +"map.ab#cd#.ef#.gh##ij=fifth\n"  // ab#cd#.df#.gh##ij => ab#cd.df.gh##ij
                +"map.#.=sixth\n"                 // #. => .
                +"map.ab#.d=seventh\n"            // ab#.d => ab.d
                +"map.ef##.d=eigth\n"             // ef##.d => ef#->d
                +"map.ab###.d=ninth\n"            // ab###.d => ab#.d
                +"map.xy#.d.ij=tenth\n"           // xy#.d.ij => xy.d->ij
                +"map.xy##.d.ij=eleventh\n"       // xy##.d.ij => xy#->d->ij
                +"map.xy###.d.ij=twelfth\n"       // xy###.d => xy#.d->ij
                ;
        MapWrapper w = mapper.readerFor(MapWrapper.class)
                .with(new JavaPropsSchema().withPathSeparatorEscapeChar('#')).readValue(INPUT);
        assertNotNull(w.map);
        assertEquals(12, w.map.size());
        assertEquals("first", w.map.get(""));
        assertEquals("second", w.map.get("b"));
        assertEquals("third", w.map.get("xyz"));
        assertEquals("fourth", w.map.get("ab.c"));
        assertEquals("fifth", w.map.get("ab#cd.ef.gh##ij"));
        assertEquals("sixth", w.map.get("."));
        assertEquals("seventh", w.map.get("ab.d"));
        assertEquals("eigth", ((Map<?,?>) w.map.get("ef#")).get("d"));
        assertEquals("ninth", w.map.get("ab#.d"));
        assertEquals("tenth", ((Map<?,?>) w.map.get("xy.d")).get("ij"));
        assertEquals("eleventh", ((Map<?,?>) ((Map<?,?>) w.map.get("xy#")).get("d")).get("ij"));
        assertEquals("twelfth", ((Map<?,?>) w.map.get("xy#.d")).get("ij"));
    }
}
