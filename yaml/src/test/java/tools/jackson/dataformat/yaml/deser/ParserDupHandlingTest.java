package tools.jackson.dataformat.yaml.deser;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.snakeyaml.engine.v2.api.LoadSettings;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ParserDupHandlingTest extends ModuleTestBase
{
    private final static String YAML_WITH_DUPS =
"name:\n"
+"  first: Bob\n"
+"  first: Dup\n";

    public void testDupChecksDisabled() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));

        ObjectMapper mapper = new ObjectMapper(f);
        _verifyDupsOk(mapper, YAML_WITH_DUPS, false);
        _verifyDupsOk(mapper, YAML_WITH_DUPS, true);
    }

    public void testDupChecksEnabled() throws Exception
    {
        YAMLFactory f = YAMLFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        ObjectMapper mapper = new ObjectMapper(f);
        _verifyDupsFail(mapper, YAML_WITH_DUPS, false);
        _verifyDupsFail(mapper, YAML_WITH_DUPS, true);
    }

    public void testDupChecksEnabledLoaderOptions() throws Exception
    {
        LoadSettings loadSettings = LoadSettings.builder()
                .setAllowDuplicateKeys(false)
                .build();
        YAMLFactory f = YAMLFactory.builder().loadSettings(loadSettings).build();

        ObjectMapper mapper = new ObjectMapper(f);
        _verifyDupsFail(mapper, YAML_WITH_DUPS, false);
        _verifyDupsFail(mapper, YAML_WITH_DUPS, true);
    }

    private void _verifyDupsOk(ObjectMapper mapper, String doc, boolean useBytes)
    {
        JsonParser p = useBytes
                ? mapper.createParser(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)))
                : mapper.createParser(new StringReader(doc));
        _stream(p);
        p.close();
    }

    private void _verifyDupsFail(ObjectMapper mapper, String doc, boolean useBytes)
    {
        JsonParser p = useBytes
                ? mapper.createParser(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)))
                : mapper.createParser(new StringReader(doc));
        try {
            _stream(p);
        } catch (StreamReadException e) {
            verifyException(e, "Duplicate Object property \"first\"");
        }
        p.close();
    }

    private void _stream(JsonParser p)
    {
        while (p.nextToken() != null) { }
    }
}
