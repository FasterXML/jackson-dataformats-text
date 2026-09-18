package tools.jackson.dataformat.csv.ser;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class ScratchCoreTest extends ModuleTestBase
{
    @Test
    public void testCoreEncodings() throws Exception {
        System.out.println("### jackson-core version: "+new JsonFactory().version());
        for (JsonEncoding enc : JsonEncoding.values()) {
            for (boolean autoClose : new boolean[]{true,false}) {
                for (boolean flush : new boolean[]{true,false}) {
                    JsonFactory f = JsonFactory.builder()
                            .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                            .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flush)
                            .build();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), out, enc)) {
                        g.writeStartObject();
                        g.writeName("a");
                        g.writeNumber(1);
                        g.writeEndObject();
                    }
                    System.out.println("### enc="+enc+" autoClose="+autoClose
                            +" flush="+flush+" -> bytes="+out.size());
                }
            }
        }
    }
}
