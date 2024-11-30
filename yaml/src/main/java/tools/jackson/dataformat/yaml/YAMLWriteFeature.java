package tools.jackson.dataformat.yaml;

import tools.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for YAML generators
 *<p>
 * NOTE: in Jackson 2.x this was named {@code YAMLGenerator.Feature}.
 */
public enum YAMLWriteFeature implements FormatFeature
{
    /**
     * Whether we are to write an explicit document start marker ("---")
     * or not.
     */
    WRITE_DOC_START_MARKER(true),

    /**
     * Whether to use YAML native Object Id construct for indicating type (true);
     * or "generic" Object Id mechanism (false). Former works better for systems that
     * are YAML-centric; latter may be better choice for interoperability, when
     * converting between formats or accepting other formats.
     */
    USE_NATIVE_OBJECT_ID(true),

    /**
     * Whether to use YAML native Type Id construct for indicating type (true);
     * or "generic" type property (false). Former works better for systems that
     * are YAML-centric; latter may be better choice for interoperability, when
     * converting between formats or accepting other formats.
     */
    USE_NATIVE_TYPE_ID(true),

    /**
     * Do we try to force so-called canonical output or not.
     * <p>
     *     Ignored if you provide your own {@code DumperOptions}.
     * </p>
     */
    CANONICAL_OUTPUT(false),

    /**
     * Options passed to SnakeYAML that determines whether longer textual content
     * gets automatically split into multiple lines or not.
     * <p>
     * Feature is enabled by default to conform to SnakeYAML defaults.
     * </p>
     * <p>
     *     Ignored if you provide your own {@code DumperOptions}.
     * </p>
     */
    SPLIT_LINES(true),

    /**
     * Whether strings will be rendered without quotes (true) or
     * with quotes (false, default).
     * <p>
     *     Minimized quote usage makes for more human readable output; however, content is
     *     limited to printable characters according to the rules of
     *     <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
     * </p>
     */
    MINIMIZE_QUOTES(false),

    /**
     * Whether numbers stored as strings will be rendered with quotes (true) or
     * without quotes (false, default) when MINIMIZE_QUOTES is enabled.
     * <p>
     *     Minimized quote usage makes for more human readable output; however, content is
     *     limited to printable characters according to the rules of
     *     <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
     * </p>
     */
    ALWAYS_QUOTE_NUMBERS_AS_STRINGS(false),

    /**
     * Whether for string containing newlines a
     * <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>
     * should be used. This automatically enabled when {@link #MINIMIZE_QUOTES} is set.
     * <p>
     *     The content of such strings is limited to printable characters according to the rules of
     *     <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
     * </p>
     */
    LITERAL_BLOCK_STYLE(false),

    /**
     * Feature enabling of which adds indentation for array entry generation
     * (default indentation being 2 spaces).
     * <p>
     *     Default value is {@code false} for backwards compatibility
     * </p>
     * <p>
     *     Ignored if you provide your own {@code DumperOptions}.
     * </p>
     *
     */
    INDENT_ARRAYS(false),

    /**
     * Feature enabling of which adds indentation with indicator for array entry generation
     * (default indentation being 2 spaces).
     * <p>
     *     Default value is {@code false} for backwards compatibility
     * </p>
     * <p>
     *     Ignored if you provide your own {@code DumperOptions}.
     * </p>
     *
     */
    INDENT_ARRAYS_WITH_INDICATOR(false),

    /**
     * Option passed to SnakeYAML to allows writing key longer that 128 characters
     * (up to 1024 characters).
     * If disabled, the max key length is left as 128 characters: longer names
     * are truncated. If enabled, limit is raised to 1024 characters.
     * <p>
     *     Ignored if you provide your own {@code DumperOptions}.
     * </p>
     */
    ALLOW_LONG_KEYS(false),
    ;

    private final boolean _defaultState;
    private final int _mask;

    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    public static int collectDefaults()
    {
        int flags = 0;
        for (YAMLWriteFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    private YAMLWriteFeature(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public int getMask() { return _mask; }
}
