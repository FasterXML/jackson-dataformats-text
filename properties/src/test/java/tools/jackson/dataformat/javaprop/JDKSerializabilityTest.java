package tools.jackson.dataformat.javaprop;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JDKSerializabilityTest extends ModuleTestBase
{
    public void testApacheMapperWithModule() throws Exception {
        // very simple validation: should still work wrt serialization
        JavaPropsMapper unfrozenMapper = serializeAndDeserialize(new JavaPropsMapper());

        // and then simple verification that write+read still works

        Object input = _simpleData();
        byte[] encoded = unfrozenMapper.writeValueAsBytes(input);
        final Object result = unfrozenMapper.readerFor(Object.class)
                .readValue(encoded);
        assertEquals(result, input);

        // and also verify `rebuild()` works:
        JavaPropsMapper copy = unfrozenMapper.rebuild().build();
        assertNotSame(unfrozenMapper, copy);
        // with 3.x, factories are immutable so they need not be unshared:
        assertSame(unfrozenMapper.tokenStreamFactory(), copy.tokenStreamFactory());

        final Object result2 = copy.readerFor(Object.class)
                .readValue(encoded);
        assertEquals(input, result2);
    }

    private JavaPropsMapper serializeAndDeserialize(JavaPropsMapper mapper) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
        outputStream.writeObject(mapper);
        byte[] serializedBytes = byteArrayOutputStream.toByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);
        Object deserializedObject = inputStream.readObject();
        assertTrue("Deserialized object should be an instance of JavaPropsMapper",
                deserializedObject instanceof JavaPropsMapper);
        return (JavaPropsMapper) deserializedObject;
    }

    private Object _simpleData() {
        Map<String,Object> stuff = new LinkedHashMap<>();
        // Since typing won't be retained only use strings...
        stuff.put("foo", "bar");
        stuff.put("value", "something else");
        return stuff;
    }
}
