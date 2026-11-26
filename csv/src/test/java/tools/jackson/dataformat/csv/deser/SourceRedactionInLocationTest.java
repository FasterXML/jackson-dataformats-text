package tools.jackson.dataformat.csv.deser;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Verify that `StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` is honored, and
// that redacted locations retain line/column (see [core#1833])
public class SourceRedactionInLocationTest extends ModuleTestBase
{
    static class IntPair {
        public int a, b;
    }

    private final static byte[] DOC = "1,secret\n".getBytes(StandardCharsets.UTF_8);

    @Test
    public void sourceRedactedByDefault() throws Exception
    {
        TokenStreamLocation loc = _failLocation(CsvMapper.builder().build());
        assertThat(loc.contentReference().getRawContent()).isNull();
        assertThat(loc.contentReference().hasTextualContent()).isTrue();
        assertThat(loc.sourceDescription()).startsWith("REDACTED");
        assertThat(loc.offsetDescription()).isEqualTo("line: 1, column: 3");
        assertThat(loc.toString()).doesNotContain("secret");
    }

    @Test
    public void sourceRedactedWithoutNote() throws Exception
    {
        TokenStreamLocation loc = _failLocation(CsvMapper.builder()
                .disable(StreamReadFeature.INCLUDE_REDACTION_NOTE_IN_LOCATION)
                .build());
        assertThat(loc.contentReference().getRawContent()).isNull();
        assertThat(loc.sourceDescription()).isEmpty();
        assertThat(loc.offsetDescription()).isEqualTo("line: 1, column: 3");
    }

    @Test
    public void sourceIncludedIfEnabled() throws Exception
    {
        TokenStreamLocation loc = _failLocation(CsvMapper.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build());
        assertThat(loc.contentReference().getRawContent()).isSameAs(DOC);
        assertThat(loc.sourceDescription()).contains("secret");
        assertThat(loc.offsetDescription()).isEqualTo("line: 1, column: 3");
    }

    private TokenStreamLocation _failLocation(CsvMapper mapper)
    {
        ObjectReader r = mapper.readerFor(IntPair.class)
                .with(mapper.schemaFor(IntPair.class));
        DatabindException e = assertThrows(DatabindException.class,
                () -> r.readValue(DOC));
        verifyException(e, "secret"); // value itself still reported
        return e.getLocation();
    }
}
