package tools.jackson.dataformat.toml;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.exc.StreamReadException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Verify that `StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` is honored, and
// that redacted locations retain line/column (see [core#1833])
public class SourceRedactionInLocationTest
{
    private final static byte[] DOC = "secret = 1\nb = = x\n".getBytes(StandardCharsets.UTF_8);

    @Test
    public void sourceRedactedByDefault() throws Exception
    {
        TokenStreamLocation loc = _failLocation(TomlMapper.builder().build());
        assertThat(loc.contentReference().getRawContent()).isNull();
        assertThat(loc.sourceDescription()).startsWith("REDACTED");
        assertThat(loc.toString()).doesNotContain("secret");
    }

    @Test
    public void sourceRedactedWithoutNote() throws Exception
    {
        TokenStreamLocation loc = _failLocation(TomlMapper.builder()
                .disable(StreamReadFeature.INCLUDE_REDACTION_NOTE_IN_LOCATION)
                .build());
        assertThat(loc.contentReference().getRawContent()).isNull();
        assertThat(loc.sourceDescription()).isEmpty();
    }

    @Test
    public void sourceIncludedIfEnabled() throws Exception
    {
        TokenStreamLocation loc = _failLocation(TomlMapper.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build());
        assertThat(loc.contentReference().getRawContent()).isSameAs(DOC);
        assertThat(loc.sourceDescription()).contains("secret");
    }

    // Line/column retained whether source is redacted or not
    private TokenStreamLocation _failLocation(TomlMapper mapper)
    {
        StreamReadException e = assertThrows(StreamReadException.class,
                () -> mapper.readTree(DOC));
        TokenStreamLocation loc = e.getLocation();
        assertThat(loc.offsetDescription()).isEqualTo("line: 2, column: 5");
        return loc;
    }
}
