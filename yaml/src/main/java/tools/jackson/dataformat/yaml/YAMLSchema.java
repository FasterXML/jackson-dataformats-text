package tools.jackson.dataformat.yaml;

/**
 * Enum of YAML 1.2 schemas that can be used for parsing YAML documents.
 *
 * @since 3.2
 */
public enum YAMLSchema {
    /**
     * Schema guaranteed to work with any document.
     * @see https://yaml.org/spec/1.2.2/#101-failsafe-schema
     */
    FAILSAFE,
    /**
     * Schema for parsing JSON-compatible YAML documents.
     * @see https://yaml.org/spec/1.2.2/#102-json-schema
     */
    JSON,
    /**
     * Schema for parsing standard YAML documents.
     *<p>
     * Selecting this schema does not disable
     * {@link YAMLReadFeature#PARSE_OCTAL_NUMBERS}, which is enabled by default for
     * backwards compatibility. Consequently, {@code 0444} is read as decimal
     * {@code 292}. To read it as decimal {@code 444}, disable the feature explicitly:
     *<pre>{@code
     * YAMLFactory.builder()
     *     .yamlSchema(YAMLSchema.CORE)
     *     .disable(YAMLReadFeature.PARSE_OCTAL_NUMBERS)
     *     .build();
     *}</pre>
     * @see https://yaml.org/spec/1.2.2/#103-core-schema
     */
    CORE,
    ;
}
