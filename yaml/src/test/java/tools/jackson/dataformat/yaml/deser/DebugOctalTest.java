package tools.jackson.dataformat.yaml.deser;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;
import tools.jackson.dataformat.yaml.YAMLSchema;

public class DebugOctalTest extends ModuleTestBase {
    @Test
    public void testDebug() throws Exception {
        YAMLFactory f = YAMLFactory.builder()
                .yamlSchema(YAMLSchema.CORE)
                .build();
        YAMLMapper mapper = YAMLMapper.builder(f).build();
        
        System.err.println("PARSE_OCTAL_NUMBERS mask: " + YAMLReadFeature.PARSE_OCTAL_NUMBERS.getMask());
        System.err.println("collectDefaults: " + YAMLReadFeature.collectDefaults());
        System.err.println("Feature enabled in defaults: " + YAMLReadFeature.PARSE_OCTAL_NUMBERS.enabledIn(YAMLReadFeature.collectDefaults()));
        
        try (JsonParser p = mapper.createParser("0444")) {
            JsonToken t = p.nextToken();
            System.err.println("Token: " + t);
            if (t == JsonToken.VALUE_NUMBER_INT) {
                System.err.println("Int value: " + p.getIntValue());
            } else if (t == JsonToken.VALUE_STRING) {
                System.err.println("String value: " + p.getText());
            } else {
                System.err.println("Text: " + p.getText());
            }
        }
    }
}
